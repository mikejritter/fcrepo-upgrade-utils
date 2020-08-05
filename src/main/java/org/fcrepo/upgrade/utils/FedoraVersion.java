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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * An enum representing supported Fedora versions
 * @author dbernstein
 */
public enum FedoraVersion {
    V_4_7_5("4.7.5"),
    V_5("5+"),
    V_6("6+");

    private String strValue;

    private FedoraVersion(final String strValue) {
        this.strValue = strValue;
    }

    public String getStringValue() {
        return this.strValue;
    }

    public static FedoraVersion fromString(final String strValue) {
        for (FedoraVersion v : FedoraVersion.values()) {
            if (v.strValue.equals(strValue)) {
                return v;
            }
        }

        throw new IllegalArgumentException(
                String.format("%s is not a valid version. Please try one of the following: %s",
                        strValue, String.join(",", Arrays.asList(FedoraVersion.values())
                                .stream().map(x -> x.getStringValue())
                                .collect(Collectors.toList()))));
    }

}
