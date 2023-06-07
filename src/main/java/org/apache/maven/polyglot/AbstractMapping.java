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

import java.io.File;
import java.util.Map;

import org.apache.maven.building.Source;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;

public abstract class AbstractMapping implements Mapping {

    protected final String extension;

    public AbstractMapping(String extension) {
        this.extension = extension;
    }

    @Override
    public File locatePom(File dir) {
        File file = new File(dir, "pom" + extension);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    @Override
    public boolean accept(Map<String, ?> options) {
        Source source = (Source) options.get(ModelProcessor.SOURCE);
        if (source != null) {
            String location = source.getLocation();
            if (location != null) {
                if (location.endsWith(extension)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ModelReader getReader() {
        return null;
    }

    @Override
    public ModelWriter getWriter() {
        return null;
    }
}
