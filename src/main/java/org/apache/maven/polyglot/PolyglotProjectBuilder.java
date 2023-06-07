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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.eclipse.sisu.Typed;

@Named("polyglot")
@Priority(10)
@Typed(ProjectBuilder.class)
public class PolyglotProjectBuilder implements ProjectBuilder {

    private final PolyglotModelProcessor processor;
    private final ProjectBuilder builder;

    @Inject
    public PolyglotProjectBuilder(PolyglotModelProcessor processor, @Named("default") ProjectBuilder builder) {
        this.processor = processor;
        this.builder = builder;
    }

    private ProjectBuildingResult convert(ProjectBuildingResult result) {
        if (result.getPomFile() == null) {
            return result;
        }
        MavenProject project = result.getProject();

        // When running with the argument `-f <pomFile>`, we must restore the location of the generated pom xml file.
        // Otherwise, it retains a reference to the polyglot pom, which causes a `409 Conflict` error when deployed.
        File pomFile = processor.getPomXmlFile(result.getPomFile()).orElse(result.getPomFile());
        project.setPomFile(pomFile);
        project.getModel().setPomFile(pomFile);

        return new PolyglotProjectBuildingResult(result, pomFile);
    }

    @Override
    public ProjectBuildingResult build(File projectFile, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return convert(builder.build(projectFile, request));
    }

    @Override
    public ProjectBuildingResult build(Artifact projectArtifact, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return convert(builder.build(projectArtifact, request));
    }

    @Override
    public ProjectBuildingResult build(Artifact projectArtifact, boolean allowStubModel, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return convert(builder.build(projectArtifact, allowStubModel, request));
    }

    @Override
    public ProjectBuildingResult build(ModelSource modelSource, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        return convert(builder.build(modelSource, request));
    }

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request)
            throws ProjectBuildingException {
        List<ProjectBuildingResult> results = builder.build(pomFiles, recursive, request);
        return results.stream().map(this::convert).collect(Collectors.toList());
    }

    static class PolyglotProjectBuildingResult implements ProjectBuildingResult {

        private final ProjectBuildingResult result;
        private final File pomFile;

        PolyglotProjectBuildingResult(ProjectBuildingResult result, File pomFile) {
            this.result = result;
            this.pomFile = pomFile;
        }

        @Override
        public File getPomFile() {
            return pomFile;
        }

        @Override
        public String getProjectId() {
            return result.getProjectId();
        }

        @Override
        public MavenProject getProject() {
            return result.getProject();
        }

        @Override
        public List<ModelProblem> getProblems() {
            return result.getProblems();
        }

        @Override
        public DependencyResolutionResult getDependencyResolutionResult() {
            return result.getDependencyResolutionResult();
        }
    }
}
