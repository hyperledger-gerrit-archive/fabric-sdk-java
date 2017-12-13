/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric_ca.sdk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

/**
 * A identity request is information required to add an identity
 */
public class AffiliationRequest {

    // The name of the affiliation
    private String name;
    // Force the operation
    private boolean force;

    private String caName;

    // Constructor

    /**
     * Register user with certificate authority
     *
     * @param id The id of the user to register.
     * @throws Exception
     */
    public AffiliationRequest(String name) throws Exception {
        if (name == null) {
            throw new Exception("name may not be null");
        }
        this.name = name;
    }

    public AffiliationRequest(String name, boolean force) throws Exception {
        if (name == null) {
            throw new Exception("name may not be null");
        }
        this.force = force;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean getForce() {
        return force;
    }

    // Convert the registration request to a JSON string
    String toJson() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(new PrintWriter(stringWriter));
        jsonWriter.writeObject(toJsonObject());
        jsonWriter.close();
        return stringWriter.toString();
    }

    // Convert the registration request to a JSON object
    JsonObject toJsonObject() {
        JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("name", name);
        ob.add("force", force);

        JsonObjectBuilder ob2 = Json.createObjectBuilder();
        ob2.add("info", ob);
        if (caName != null) {
            ob2.add(HFCAClient.FABRIC_CA_REQPROP, caName);
        }
        return ob2.build();
    }
}
