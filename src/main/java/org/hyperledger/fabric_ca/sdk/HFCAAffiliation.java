/*
 *
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hyperledger.fabric_ca.sdk;

import java.util.ArrayList;
import java.util.Collection;

import javax.json.JsonArray;
import javax.json.JsonObject;

// Hyperledger Fabric CA Affiliation information

public class HFCAAffiliation {

    private String name;
    // Optional CA name
    private String caName = "";


    public HFCAAffiliation(String name) {
        this.name = name;
    }

    public HFCAAffiliation(String name, String caName) {
        this(name);
        this.caName = caName;
    }

    public HFCAAffiliation(JsonObject result) {
        JsonObject info = result.getJsonObject("info");
        this.name = info.getString("name");
        if (result.containsKey("caname")) {
            this.caName = result.getString("caname");
        }
    }

    /**
     * The name of the affiliation
     *
     * @return The affiliation name.
     */

    public String getName() {
        return name;
    }

    /**
     * The CAName for the Fabric Certificate Authority.
     *
     * @return The CA Name.
     */

    public String getCAName() {
        return caName;
    }

}