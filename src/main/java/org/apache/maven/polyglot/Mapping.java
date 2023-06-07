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

import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;

public interface Mapping {

    /**
     * Locates the pom in the given directory
     * @param dir the directory to locate the pom for
     * @return the located pom or <code>null</code> if none was found by this mapping
     */
    File locatePom(File dir);

    /**
     * Tests whether this mapping accepts the given option
     * @param options the options to use
     * @return <code>true</code> if options are accepted, <code>false</code> otherwise
     */
    boolean accept(Map<String, ?> options);

    /**
     *
     * @return the {@link ModelReader} responsible for reading poms returned by the {@link #locatePom(File)} method
     */
    ModelReader getReader();

    /**
     *
     * @return the {@link ModelWriter} responsible for writing poms returned by the {@link #locatePom(File)} method
     */
    ModelWriter getWriter();
}
