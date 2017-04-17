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

import io.netty.util.internal.StringUtil;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

/**
 * A RevocationRequest defines the attributes required to revoke credentials with member service.
 */
public class RevocationRequest {

    // Enrollment ID whose certificates are to be revoked
    private String enrollmentID;
    // Serial number of certificate to be revoked
    private String serial;
    // Authority key identifier of certificate to be revoked
    private String aki;
    // Reason for revocation
    private int reason;

    // Constructor
    public RevocationRequest(String id, String serial, String aki, int reason) throws Exception {
        if (StringUtil.isNullOrEmpty(id)) {
            if (StringUtil.isNullOrEmpty(serial) || StringUtil.isNullOrEmpty(aki)) {
                throw new Exception("Enrollment ID is empty, thus both aki and serial must have non-empty values");
            }
        }
        this.enrollmentID = id;
        this.serial = serial;
        this.aki = aki;
        this.reason = reason;
    }

    public String getUser() {
        return enrollmentID;
    }

    public void setUser(String user) {
        this.enrollmentID = user;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getAki() {
        return aki;
    }

    public void setAki(String aki) {
        this.aki = aki;
    }

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    // Convert the revocation request to a JSON string
    public String toJson() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(new PrintWriter(stringWriter));
        jsonWriter.writeObject(this.toJsonObject());
        jsonWriter.close();
        return stringWriter.toString();
    }

    // Convert the revocation request to a JSON object
    private JsonObject toJsonObject() {
        JsonObjectBuilder factory = Json.createObjectBuilder();
        if (this.enrollmentID != null) {
            // revoke all enrollments of this user, serial and aki are ignored in this case
            factory.add("id", this.enrollmentID);
        } else {
            // revoke one particular enrollment
            factory.add("serial", "0" + this.serial);
            factory.add("aki", this.aki);
        }
        factory.add("reason", this.reason);
        return factory.build();
    }
}
