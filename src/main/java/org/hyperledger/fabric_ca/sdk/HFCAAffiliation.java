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

    private Collection<String> names = new ArrayList<String>();

    private Collection<HFCAIdentity> identities = new ArrayList<HFCAIdentity>();


    public HFCAAffiliation(String name) {
        this.name = name;
    }

    public HFCAAffiliation(String name, String caName) {
        this(name);
        this.caName = caName;
    }

    public HFCAAffiliation(JsonObject result) {
        if (result.containsKey("info")) {
            JsonObject info = result.getJsonObject("info");
            if (info.containsKey("name")) {
                this.name = info.getString("name");
            }
        }
        if (result.containsKey("caname")) {
            this.caName = result.getString("caname");
        }
        if (result.containsKey("affiliations")) {
            JsonArray affiliations = result.getJsonArray("affiliations");
            if (affiliations != null && !affiliations.isEmpty()) {
                for (int i = 0; i < affiliations.size(); i++) {
                    JsonObject aff = affiliations.getJsonObject(i);
                    names.add(aff.getString("name"));
                }
            }
        }
        if (result.containsKey("identities")) {
            JsonArray ids = result.getJsonArray("identities");
            if (ids != null && !ids.isEmpty()) {
                for (int i = 0; i < ids.size(); i++) {
                    JsonObject id = ids.getJsonObject(i);
                    HFCAIdentity hfcaID = new HFCAIdentity(id);
                    identities.add(hfcaID);
                }
            }
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
     * The name of the affiliation affected during request
     *
     * @return The affiliation names.
     */

    public Collection<String> getNames() {
        return names;
    }

    /**
     * The identities affected during request
     *
     * @return The identities affected.
     */

    public Collection<HFCAIdentity> getIdentities() {
        return identities;
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