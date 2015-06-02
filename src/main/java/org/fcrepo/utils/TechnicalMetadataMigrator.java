/**
 * Copyright 2015 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.utils;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;

import javax.inject.Inject;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.annotations.VisibleForTesting;

import org.fcrepo.kernel.models.Container;
import org.fcrepo.kernel.models.FedoraBinary;
import org.fcrepo.kernel.models.FedoraResource;
import org.fcrepo.kernel.services.NodeService;

import org.slf4j.Logger;

/**
 * Utility to migrate file technical metadata in existing repositories to comply
 * with changes made in May 2015:
 *   fedora:digest => premis:hasMessageDigest
 *   fedora:mimeType => ebucore:hasMimeType
 *   premis:hasOriginalName => ebucore:filename
 *
 * @author escowles
 * @since 2015-05-21
**/
public class TechnicalMetadataMigrator {
    /*
    ApplicationContext context = new ClasspathXmlApplication("context.xml");
    Repository repo = (Repository)context.getBean("repository");
    */

    private static final Logger logger = getLogger(TechnicalMetadataMigrator.class);
    private static boolean dryrun = false;

    @Inject
    private static Session session;

    @Inject
    private static NodeService nodeService;

    @VisibleForTesting
    protected TechnicalMetadataMigrator(final Session session, final NodeService nodeService) {
        this.session = session;
        this.nodeService = nodeService;
    }

    /**
     * Migrate technical metadata.
     * @param args If "dryrun" is passed as an argument, the utility will print out what would be done,
     *             but no changes will be made.
    **/
    public static void main(final String[] args) {
        try {
            if (args.length > 0 && "dryrun".equals(args[0])) {
                dryrun = true;
            }
            logger.warn("main(): dryrun={}", dryrun);
            processResource(nodeService.find(session, "/"));
        } catch (RepositoryException ex) {
            ex.printStackTrace();
        }
    }

    private static void processResource(final FedoraResource resource) throws RepositoryException {
        logger.warn("processResource(): " + resource.getPath());
        if (resource instanceof Container) {
            for (final Iterator<FedoraResource> children = resource.getChildren(); children.hasNext(); ) {
                processResource(children.next());
            }
        } else if (resource instanceof FedoraBinary) {
            processBinary((FedoraBinary)resource);
        }
    }

    private static void processBinary(final FedoraBinary binary) throws RepositoryException {
        logger.warn(binary.getPath());
        migrate(binary, "fedora:digest", "premis:hasMessageDigest");
        migrate(binary, "fedora:mimeType", "ebucore:hasMimeType");
        migrate(binary, "premis:hasOriginalName", "ebucore:filename");
    }

    private static void migrate(final FedoraBinary binary, final String fromProp, final String toProp)
            throws RepositoryException {
        final Property p = binary.getProperty(fromProp);
        logger.warn("  {} => {}: {}", fromProp, toProp, p.getString());
        if (!dryrun) {
            binary.getNode().setProperty(toProp, p.getValue());
            p.remove();
        }
    }
}
