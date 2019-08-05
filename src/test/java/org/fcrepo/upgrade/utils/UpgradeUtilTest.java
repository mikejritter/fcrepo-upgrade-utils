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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link org.fcrepo.upgrade.utils.UpgradeUtil}
 *
 * @author dbernstein
 */

public class UpgradeUtilTest {


    static final String TARGET_DIR = System.getProperty("project.build.directory");

    @Test
    public void test475to5xUpgrade() throws Exception {


        //prepare

        final File tmpDir = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + "");
        tmpDir.mkdirs();
        final File input = new File(TARGET_DIR + "/test-classes/4.7.5-export");
        final File output = new File(tmpDir, "output");
        output.mkdir();

        //run
        new UpgradeUtil(input, output).run();

        //ensure all expected files exist
        final String[] expectedFiles = new String[]{"rest.ttl",
                "rest.ttl.headers",
                "rest/external1",
                "rest/external1/fcr%3Ametadata.ttl",
                "rest/container1.ttl.headers",
                "rest/container1/testbinary.binary",
                "rest/container1/testbinary/fcr%3Ametadata.ttl",
                "rest/container1/testbinary.binary.headers",
                "rest/container1.ttl",
                "rest/external1.external.headers",
                "rest/external1.external"};

        for (String f : expectedFiles) {
            assertTrue(f + " does not exist as expected", getFile(output, f).exists());
        }

        //ensure external content has been transformed properly
        try (FileInputStream fis = new FileInputStream(getFile(output, "rest/external1/fcr%3Ametadata.ttl"))) {
            final String externalContent = IOUtils.toString(fis, "UTF-8");
            assertFalse("external content metadata should contain the mimetype", externalContent.contains("image/jpg"));
            assertFalse("message/external-body should not be present in the external content metadata",
                    externalContent.contains("message/external-body"));

        }

        //ensure the binaries contain NonRDFSource types in their headers
        final Map<String, List<String>> bheadders =
                deserializeHeaders(getFile(output, "rest/container1/testbinary.binary.headers"));
        assertTrue("binary does not contain NonRDFSource type in the link headers",
                bheadders.get("Link").stream().anyMatch(x -> x.contains("NonRDFSource")));

    }

    private Map<String, List<String>> deserializeHeaders(final File headerFile) throws IOException {
        final byte[] mapData = Files.readAllBytes(Paths.get(headerFile.toURI()));
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(mapData, new TypeReference<HashMap<String, List<String>>>() {
        });
    }

    private File getFile(final File output, final String subpath) {
        return new File(output.getAbsolutePath() + "/" + subpath);
    }
}
