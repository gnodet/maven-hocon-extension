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
package org.apache.maven.hocon;

import javax.annotation.Priority;
import javax.inject.Named;

import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.polyglot.AbstractMapping;

@Named("hocon")
@Priority(1)
public class HoconMapping extends AbstractMapping {

    public static final String EXTENSION = ".conf";

    public HoconMapping() {
        super(EXTENSION);
    }

    @Override
    public ModelReader getReader() {
        return new HoconModelReader();
    }

    @Override
    public ModelWriter getWriter() {
        return null;
    }

    //    @Override
    //    public int getPriority() {
    //        return 1;
    //    }

}
