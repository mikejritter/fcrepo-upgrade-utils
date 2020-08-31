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

import static org.fcrepo.upgrade.utils.HttpConstants.CONTENT_TYPE_HEADER;
import static org.fcrepo.upgrade.utils.HttpConstants.LOCATION_HEADER;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.client.FcrepoLink;

/**
 * @author dbernstein
 * @since 2019-08-05
 */
class F47ToF5UpgradeManager extends UpgradeManagerBase implements UpgradeManager {

    private static final org.slf4j.Logger LOGGER = getLogger(F47ToF5UpgradeManager.class);


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
        LOGGER.info("Processing directory: {}", dir.getAbsolutePath());
        try (final Stream<Path> walk = Files.walk(dir.toPath())) {
            for (Path path : walk.filter(path -> Files.isRegularFile(path)).collect(Collectors.toList())) {
                processFile(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processFile(final Path path) {
        final Path inputPath = this.config.getInputDir().toPath();
        final Path relativePath = inputPath.relativize(path);
        final Path newLocation = new File(this.config.getOutputDir(), relativePath.toString()).toPath();
        newLocation.toFile().getParentFile().mkdirs();
        LOGGER.debug("copy file {} to {}", path, newLocation);
        try {
            FileUtils.copyFile(path.toFile(), newLocation.toFile());
            if (newLocation.toString().endsWith(".ttl")) {
                //parse the file
                final Model model = ModelFactory.createDefaultModel();
                try (final FileInputStream is = new FileInputStream(newLocation.toFile())) {
                    RDFDataMgr.read(model, is, Lang.TTL);
                }

                final Map<String, List<String>> headers = new HashMap<>();

                final AtomicBoolean isBinary = new AtomicBoolean(false);
                final AtomicBoolean isExternal = new AtomicBoolean(false);

                model.listStatements().forEachRemaining(statement -> {
                    if (statement.getPredicate().equals(RdfConstants.RDF_TYPE)) {
                        final Resource object = statement.getObject().asResource();
                        if (object.equals(RdfConstants.LDP_NON_RDFSOURCE)) {
                            isBinary.set(true);
                        }

                        if (!headers.containsKey("Link")) {
                            headers.put("Link", new ArrayList<String>());
                        }


                        final FcrepoLink link = FcrepoLink.fromUri(object.getURI()).rel("type").build();
                        headers.get("Link").add(link.toString());

                    }

                    if (statement.getPredicate().equals(RdfConstants.EBUCORE_HAS_MIME_TYPE)) {
                        final String value = statement.getObject().toString();
                        final String valueAsLiteral = statement.getObject().asLiteral().getString();

                        LOGGER.debug("predicate valueAsLiteral={}", valueAsLiteral);
                        LOGGER.debug("predicate value={}", value);

                        if (value.startsWith("message/external-body")) {
                            final String externalURI = valueAsLiteral.substring(valueAsLiteral.toLowerCase().indexOf("url=\"") + 5, valueAsLiteral.length() - 1);
                            LOGGER.debug("externalURI={}", externalURI);

                            try {
                                //guess mimetype
                                final String mimeType = URLConnection.guessContentTypeFromName(externalURI);
                                final String resourceAsString = IOUtils
                                    .toString(new FileInputStream(newLocation.toFile()),
                                              "UTF-8");
                                final String newBody = resourceAsString
                                    .replace(value, mimeType != null ? mimeType : "application/octet-stream");
                                IOUtils.write(newBody, new FileOutputStream(newLocation.toFile()), "UTF-8");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }


                            if (!headers.containsKey(LOCATION_HEADER)) {
                                headers.put(LOCATION_HEADER, new ArrayList<String>());
                            }

                            headers.get("Location").add(externalURI);
                            isExternal.set(true);
                        } else {
                            if (!headers.containsKey(CONTENT_TYPE_HEADER)) {
                                headers.put(CONTENT_TYPE_HEADER, new ArrayList<String>());
                            }
                            headers.get(CONTENT_TYPE_HEADER).add(value);
                        }
                    }
                });


                String headersPrefix;

                if (isBinary.get()) {
                    headersPrefix = newLocation.getParent().toAbsolutePath().toString();
                    headersPrefix += isExternal.get() ? ".external" : ".binary";
                } else {
                    headersPrefix = newLocation.toAbsolutePath().toString();
                }

                LOGGER.debug("isBinary={}", isBinary.get());
                LOGGER.debug("isExternal={}", isExternal.get());
                LOGGER.debug("headersPrefix={}", headersPrefix);

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
