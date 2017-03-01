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

package org.hyperledger.fabric.sdk;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;

/**
 * A registration request is information required to register a user, peer, or other
 * type of member.
 */
public class RegistrationRequest {

    // The enrollment ID of the user
    private String enrollmentID;
    // Type of identity
    private String type;
    // Optional secret
    private String secret;
    // Maximum number of enrollments with the secret
    private int maxEnrollments;
    // Affiliation for a user
    private String affiliation;
    // Array of attribute names and values
    private ArrayList<Attribute> attrs = new ArrayList<Attribute>();
    
    // Constructor
    public RegistrationRequest(String id, String affiliation) throws Exception {
    	if (id == null) {
    		throw new Exception("id may not be null");
    	}
    	if (affiliation == null) {
    		throw new Exception("affiliation may not be null");
    	}
    	this.enrollmentID = id;
    	this.affiliation = affiliation;
    	this.type = "user";
    }
    
	public String getEnrollmentID() {
		return enrollmentID;
	}

	
	public void setEnrollmentID(String enrollmentID) {
		this.enrollmentID = enrollmentID;
	}
	
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public int getMaxEnrollments() {
		return maxEnrollments;
	}

	public void setMaxEnrollments(int maxEnrollments) {
		this.maxEnrollments = maxEnrollments;
	}

	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getAffiliation() {
		return affiliation;
	}

	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	
	// Convert the registration request to a JSON string
	public String toJson() {
	    StringWriter stringWriter = new StringWriter();
	    JsonWriter jsonWriter = Json.createWriter(new PrintWriter(stringWriter));
	    jsonWriter.writeObject(this.toJsonObject());
	    jsonWriter.close();
	    return stringWriter.toString();
	}

	// Convert the registration request to a JSON object
	public JsonObject toJsonObject() {
        JsonObjectBuilder ob = Json.createObjectBuilder();
        ob.add("id", this.enrollmentID);
        ob.add("type",  this.type);
        if (this.secret != null) {
            ob.add("secret",  this.secret);
        }
        ob.add("max_enrollments",  this.maxEnrollments);
        ob.add("affiliation",  this.affiliation);
        ob.add("group",  this.affiliation);  // TODO: REMOVE THIS WHEN API IS CHANGED  (See https://jira.hyperledger.org/browse/FAB-2534)
        JsonArrayBuilder ab = Json.createArrayBuilder();
        for (Attribute attr: this.attrs) {
        	ab.add(attr.toJsonObject());
        }
        return ob.build();
	}

  
}
