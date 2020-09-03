/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.upgrade.utils;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * RDF-related constants
 *
 * @author dbernstein
 */
public class RdfConstants {
    private RdfConstants() {
    }

    public static final Resource LDP_NON_RDFSOURCE =
        ResourceFactory.createResource("http://www.w3.org/ns/ldp#NonRDFSource");
    public static final Property EBUCORE_HAS_MIME_TYPE =
        ResourceFactory.createProperty("http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#hasMimeType");
}
