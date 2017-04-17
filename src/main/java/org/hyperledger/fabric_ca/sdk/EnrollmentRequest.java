/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
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
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;

/**
 * An enrollment request is information required to enroll the user with member service.
 */
public class EnrollmentRequest {

    // Enrollment ID of the user
    private String enrollmentID;
    // A PEM-encoded string containing the CSR (Certificate Signing Request) based on PKCS #10
    private String pem;
    // Comma-separated list of host names to associate with the certificate
    private Collection<String> hosts;
    // Name of the signing profile to use when issuing the certificate
    private String profile;
    // Label used in HSM operations
    private String label;

    // Constructor
    public EnrollmentRequest(String id, String pem) throws Exception {
        if (pem == null) {
            throw new Exception("pem should not be null");
        }
        if (id == null) {
            throw new Exception("id may not be null");
        }
        this.pem = pem;
        this.enrollmentID = id;
        this.hosts = new ArrayList<String>();
        this.profile = null;
        this.label = null;
    }

    public String getEnrollmentID() {
        return enrollmentID;
    }

    public void setEnrollmentID(String enrollmentID) {
        this.enrollmentID = enrollmentID;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Collection<String> getHosts() {
        return hosts;
    }

    public void addHost(String host) {
        this.hosts.add(host);
    }

    // Convert the enrollment request to a JSON string
    public String toJson() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(new PrintWriter(stringWriter));
        jsonWriter.writeObject(this.toJsonObject());
        jsonWriter.close();
        return stringWriter.toString();
    }

    // Convert the enrollment request to a JSON object
    private JsonObject toJsonObject() {
        JsonObjectBuilder factory = Json.createObjectBuilder();
        if (this.profile != null) {
            factory.add("profile", this.profile);
        }
        if (!this.hosts.isEmpty()) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            for (String host: this.hosts) {
                ab.add(host);
            }
            factory.add("hosts", ab.build());
        }
        if (this.label != null) {
            factory.add("label", this.label);
        }
        factory.add("certificate_request", this.pem);
        return factory.build();
    }
}
