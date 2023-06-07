/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.polyglot;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.maven.building.Source;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.eclipse.sisu.Typed;

@Named("polyglot")
@Priority(10)
@Typed(ModelProcessor.class)
public class PolyglotModelProcessor implements ModelProcessor {

    private static final String DEFAULT_POM_FILE = "pom.xml";
    private static final String POM_FILE_PREFIX = ".polyglot.";

    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String WARNING = "?>" + NEW_LINE + "<!--" + NEW_LINE
            + "" + NEW_LINE
            + "" + NEW_LINE
            + "DO NOT MODIFY - GENERATED CODE" + NEW_LINE
            + "" + NEW_LINE
            + "" + NEW_LINE
            + "-->";

    protected final Collection<Mapping> mappings;

    @Inject
    public PolyglotModelProcessor(Collection<Mapping> mappings) {
        this.mappings = mappings;
    }

    @Override
    public File locatePom(File projectDirectory) {
        File pomFile = mappings.stream()
                .map(m -> m.locatePom(projectDirectory))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (pomFile == null) {
            return new File(projectDirectory, DEFAULT_POM_FILE);
        }
        if (pomFile.getName().equals(DEFAULT_POM_FILE)
                && pomFile.getParentFile().equals(projectDirectory)) {
            // behave like proper maven in case there is no pom from manager
            return pomFile;
        }
        File polyglotPomFile = new File(pomFile.getParentFile(), POM_FILE_PREFIX + pomFile.getName());
        try {
            if (!polyglotPomFile.exists() && polyglotPomFile.createNewFile()) {
                polyglotPomFile.deleteOnExit();
            }
        } catch (IOException e) {
            throw new RuntimeException("error creating empty file", e);
        }
        return polyglotPomFile;
    }

    protected Model read(Reader input, Path pomFile, Map<String, ?> options) throws IOException {
        Optional<File> optionalPomXml = getPomXmlFile(options);
        if (optionalPomXml.isPresent()) {
            File pom = optionalPomXml.get();
            File realPom = new File(pom.getPath().replaceFirst(Pattern.quote(POM_FILE_PREFIX), ""));

            ((Map) options).put(ModelProcessor.SOURCE, new FileModelSource(realPom));

            ModelReader reader = getReaderFor(options);
            Model model = reader.read(realPom, options);
            MavenXpp3Writer xmlWriter = new MavenXpp3Writer();
            StringWriter xml = new StringWriter();
            xmlWriter.write(xml, model);

            FileUtils.fileWrite(pom, xml.toString());

            // dump pom if filename is given via the pom properties
            String dump = model.getProperties().getProperty("polyglot.dump.pom");
            if (dump == null) {
                // just nice to dump the pom.xml via commandline switch
                dump = System.getProperty("polyglot.dump.pom");
            }
            if (dump != null) {
                File dumpPom = new File(pom.getParentFile(), dump);
                if (!dumpPom.exists()
                        || !FileUtils.fileRead(dumpPom).equals(xml.toString().replace("?>", WARNING))) {
                    dumpPom.setWritable(true);
                    FileUtils.fileWrite(dumpPom, xml.toString().replace("?>", WARNING));
                    if ("true".equals(model.getProperties().getProperty("polyglot.dump.readonly"))) {
                        dumpPom.setReadOnly();
                    }
                }
            }

            model.setPomFile(pom);
            return model;
        } else {
            ModelReader reader = getReaderFor(options);
            if (pomFile != null) {
                return reader.read(pomFile.toFile(), options);
            } else if (input != null) {
                return reader.read(input, options);
            } else {
                throw new IllegalArgumentException("A File or Reader should be given");
            }
        }
    }

    private ModelReader getReaderFor(final Map<String, ?> options) {
        return mappings.stream()
                .filter(m -> m.accept(options))
                .map(Mapping::getReader)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to determine model input format; options=" + options));
    }

    private Optional<File> getPomXmlFile(Map<String, ?> options) {
        Source source = (Source) options.get(ModelProcessor.SOURCE);
        if (source != null) {
            return getPomXmlFile(new File(source.getLocation()));
        }
        return Optional.empty();
    }

    Optional<File> getPomXmlFile(File sourceFile) {
        String filename = sourceFile.getName();
        if (filename.startsWith(POM_FILE_PREFIX)) {
            return Optional.of(sourceFile);
        } else if (!filename.equals("pom.xml") && !filename.endsWith(".pom")) {
            File parent = sourceFile.getParentFile();
            if (parent == null) {
                // "virtual" model
                return Optional.empty();
            }
            File pom = locatePom(parent);
            if (pom.getName().startsWith(POM_FILE_PREFIX)) {
                return Optional.of(pom);
            }
        }
        return Optional.empty();
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException, ModelParseException {
        Objects.requireNonNull(input, "input cannot be null");
        Model model = read(null, input.toPath(), options);
        model.setPomFile(input);
        return model;
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException, ModelParseException {
        Objects.requireNonNull(input, "input cannot be null");
        try (Reader in = input) {
            return read(in, null, options);
        }
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException, ModelParseException {
        Objects.requireNonNull(input, "input cannot be null");
        try (XmlStreamReader in = ReaderFactory.newXmlReader(input)) {
            return read(in, null, options);
        }
    }
}
