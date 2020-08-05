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

import static java.lang.String.format;

/**
 * A factory class for creating Upgrade managers based on source and target versions.
 * @author  dbernstein
 */
public class UpgradeManagerFactory {

    public static UpgradeManager create(final Config config) {
        if(config.getSourceVersion().equals(FedoraVersion.V_4_7_5) &&
                config.getTargetVersion().equals(FedoraVersion.V_5)) {
            return new F47ToF5UpgradeManager(config);
        } else if(config.getSourceVersion().equals(FedoraVersion.V_5) &&
                config.getTargetVersion().equals(FedoraVersion.V_6)) {
            return new F5ToF6UpgradeManager(config);
        } else {
            throw new RuntimeException(format("The migration path from {} to {} is not supported.",
                    config.getSourceVersion().getStringValue(), config.getTargetVersion().getStringValue()));
        }
    }
}
