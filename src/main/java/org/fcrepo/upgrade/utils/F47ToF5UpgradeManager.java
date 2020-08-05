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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.client.FcrepoLink;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author dbernstein
 * @since 2019-08-05
 */
class F47ToF5UpgradeManager extends UpgradeManagerBase implements UpgradeManager {

    private static final org.slf4j.Logger logger = getLogger(F47ToF5UpgradeManager.class);


    /**
     * Constructor
     *
     * @param config the upgrade configuration
     */
    F47ToF5UpgradeManager(final Config config) {
        super(config);
    }

    @Override
    public void start() {
        //walk the directory structure
        processDirectory(this.config.getInputDir());
    }

    private void processDirectory(final File dir) {
        logger.info("Processing directory: {}", dir.getAbsolutePath());
        try (final Stream<Path> walk = Files.walk(Paths.get(this.config.getInputDir().toURI()))) {
            //process files
            final List<String> files = walk.filter(path -> Files.isRegularFile(path))
                    .map(x -> x.toString()).collect(Collectors.toList());
            files.forEach(file -> {
                processFile(new File(file));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processFile(final File file) {
        final String inputPath = this.config.getInputDir().getAbsolutePath();
        final String absolutePath = file.getAbsolutePath();
        final String relativePath = absolutePath.substring(inputPath.length());
        final File newLocation = new File(this.config.getOutputDir().getAbsolutePath() + relativePath);
        newLocation.getParentFile().mkdirs();
        logger.debug("copy file {} to {}", file.getAbsolutePath(), newLocation.getAbsoluteFile());
        try {
            Files.copy(file.toPath(), new FileOutputStream(newLocation));
            if (newLocation.getAbsolutePath().endsWith(".ttl")) {
                //parse the file
                final Model model = ModelFactory.createDefaultModel();
                try (final FileInputStream is = new FileInputStream(newLocation)) {
                    RDFDataMgr.read(model, is, Lang.TTL);
                }

                final Map<String, List<String>> headers = new HashMap<>();

                final AtomicBoolean isBinary = new AtomicBoolean(false);
                final AtomicBoolean isExternal = new AtomicBoolean(false);

                model.listStatements().forEachRemaining(statement -> {
                    if (statement.getPredicate().getURI().equals(RdfConstants.RDF_TYPE)) {
                        final String objectURI = statement.getObject().asResource().getURI();
                        if (objectURI.equals(RdfConstants.LDP_NON_RDFSOURCE)) {
                            isBinary.set(true);
                        }

                        if (!headers.containsKey("Link")) {
                            headers.put("Link", new ArrayList<String>());
                        }


                        final FcrepoLink link = FcrepoLink.fromUri(objectURI).rel("type").build();
                        headers.get("Link").add(link.toString());

                    }

                    if (statement.getPredicate().getURI().equals(RdfConstants.EBUCORE_HAS_MIME_TYPE)) {
                        final String value = statement.getObject().toString();
                        final String valueAsLiteral = statement.getObject().asLiteral().getString();

                        logger.debug("predicate valueAsLiteral={}", valueAsLiteral);
                        logger.debug("predicate value={}", value);

                        if (value.startsWith("message/external-body")) {
                            final String externalURI = valueAsLiteral.substring(valueAsLiteral.toLowerCase().indexOf("url=\"") + 5, valueAsLiteral.length() - 1);
                            logger.debug("externalURI={}", externalURI);

                            try {
                                //guess mimetype
                                final String mimeType = URLConnection.guessContentTypeFromName(externalURI);
                                final String resourceAsString = IOUtils.toString(new FileInputStream(newLocation),
                                        "UTF-8");
                                final String newBody = resourceAsString.replace(value, mimeType != null ? mimeType : "application/octet-stream");
                                IOUtils.write(newBody, new FileOutputStream(newLocation), "UTF-8");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }


                            if (!headers.containsKey("Location")) {
                                headers.put("Location", new ArrayList<String>());
                            }

                            headers.get("Location").add(externalURI);
                            isExternal.set(true);
                        } else {
                            if (!headers.containsKey("Content-Type")) {
                                headers.put("Content-Type", new ArrayList<String>());
                            }
                            headers.get("Content-Type").add(value);
                        }
                    }
                });


                String headersPrefix;

                if (isBinary.get()) {
                    headersPrefix = newLocation.getParentFile().getAbsolutePath();
                    headersPrefix += isExternal.get() ? ".external" : ".binary";
                } else {
                    headersPrefix = newLocation.getAbsolutePath();
                }

                logger.debug("isBinary={}", isBinary.get());
                logger.debug("isExternal={}", isExternal.get());
                logger.debug("headersPrefix={}", headersPrefix);

                writeHeadersFile(headers, new File(headersPrefix + ".headers"));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHeadersFile(final Map<String, List<String>> headers, final File file) throws IOException {
        final String json = new ObjectMapper().writeValueAsString(headers);
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write(json);
        }
    }
}
