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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric_ca.sdk.exception.IdentityException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import static java.lang.String.format;

/**
 * A identity request is information required to add an identity
 */
public class IdentityRequest {

    // The enrollment ID of the user
    private String enrollmentID;
    // Type of identity
    private String type = "user";
    // Optional secret
    private String secret;
    // Maximum number of enrollments with the secret
    private Integer maxEnrollments = null;
    // Affiliation for a user
    private String affiliation;
    // Array of attribute names and values
    private Collection<Attribute> attrs = new ArrayList<Attribute>();

    private String caName;

    private HFCAClient client;

    private static final String HFCA_IDENTITY = HFCAClient.HFCA_CONTEXT_ROOT + "identities";
    private static final Log logger = LogFactory.getLog(HFCAClient.class);

    // Constructor
    /**
     * Manage identity with certificate authority
     *
     */
    public IdentityRequest() throws Exception { }

    /**
     * Manage identity with certificate authority
     *
     * @param id The id of the user to register.
     * @throws Exception
     */
    public IdentityRequest(String id) throws Exception {
        if (id == null) {
            throw new Exception("id may not be null");
        }
        this.enrollmentID = id;

    }

    /**
     * Manage identity with certificate authority
     *
     * @param id          The id of the user to register.
     * @param affiliation The user's affiliation.
     * @throws Exception
     */
    public IdentityRequest(String id, String affiliation) throws Exception {
        if (id == null) {
            throw new Exception("id may not be null");
        }
        if (affiliation == null) {
            throw new Exception("affiliation may not be null");
        }

        this.enrollmentID = id;
        this.affiliation = affiliation;

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

    public Integer getMaxEnrollments() {
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

    public Collection<Attribute> getAttributes() {
        return attrs;
    }

    public void addAttribute(Attribute attr) {
        this.attrs.add(attr);
    }

    void setCAName(String caName) {
        this.caName = caName;
    }

    public void setClient(HFCAClient client) {
        this.client = client;
    }

    /**
     * gets a specific identity
     *
     * @param getId   Name if the id being requested
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was requested
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity getIdentity(User registrar) throws IdentityException, InvalidArgumentException {
        HFCAClient client = this.client;
        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        if (Utils.isNullOrEmpty(this.getEnrollmentID())) {
            throw new InvalidArgumentException("EntrollmentID cannot be null or empty");
        }

        String url = client.url;

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            String getIdURL = client.getURL(url + HFCA_IDENTITY + "/" + this.enrollmentID);
            String caname = "";
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = client.httpGet(getIdURL, authHdr);

            String id = result.getString("id");
            String type = result.getString("type");
            Integer maxenrollments = result.getInt("max_enrollments");
            String affiliation = result.getString("affiliation");
            if (result.containsKey("caname")) {
                caname = result.getString("caname");
            }
            JsonArray attributes = result.getJsonArray("attrs");

            Collection<Attribute> attrs = new ArrayList<Attribute>();
            if (attributes != null && !attributes.isEmpty()) {
                for (int i = 0; i < attributes.size(); i++) {
                    JsonObject attribute = attributes.getJsonObject(i);
                    Attribute attr = new Attribute(attribute.getString("name"), attribute.getString("value"), attribute.getBoolean("ecert", false));
                    attrs.add(attr);
                }
            }

            HFCAIdentity resp = new HFCAIdentity(id, type, maxenrollments, affiliation, attrs, caname);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while getting the user %s url: %s  %s ", this.enrollmentID, url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * gets all identities that the registrar is allowed to see
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was requested
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public Collection<HFCAIdentity> getAllIdentities(User registrar) throws IdentityException, InvalidArgumentException {
        HFCAClient client = this.client;
        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            String getAllURL = client.getURL(url + HFCA_IDENTITY);
            String caname = "";
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = client.httpGet(getAllURL, authHdr);

            if (result.containsKey("caname")) {
                caname = result.getString("caname");
            }
            Collection<HFCAIdentity> allIdentities = new ArrayList<HFCAIdentity>();

            JsonArray identities = result.getJsonArray("identities");
            if (identities != null && !identities.isEmpty()) {
                for (int i = 0; i < identities.size(); i++) {
                    JsonObject identity = identities.getJsonObject(i);

                    String id = identity.getString("id");
                    String type = identity.getString("type");
                    Integer maxenrollments = identity.getInt("max_enrollments");
                    String affiliation = identity.getString("affiliation");

                    JsonArray attributes = identity.getJsonArray("attrs");

                    Collection<Attribute> attrs = new ArrayList<Attribute>();
                    if (attributes != null && !attributes.isEmpty()) {
                        for (int j = 0; j < attributes.size(); j++) {
                            JsonObject attribute = attributes.getJsonObject(j);
                            Attribute attr = new Attribute(attribute.getString("name"), attribute.getString("value"), attribute.getBoolean("ecert", false));
                            attrs.add(attr);
                        }
                     }

                    HFCAIdentity idObj = new HFCAIdentity(id, type, maxenrollments, affiliation, attrs, caname);
                    allIdentities.add(idObj);
                }
            }

            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return allIdentities;
        } catch (Exception e) {
            String msg = format("Error while getting all users url:%s %s ", url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * add an identity
     *
     * @param request   Adding an identity request
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was added along with secret
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity addIdentity(User registrar) throws IdentityException, InvalidArgumentException {
        HFCAClient client = this.client;
        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (Utils.isNullOrEmpty(this.getEnrollmentID())) {
            throw new InvalidArgumentException("EntrollmentID cannot be null or empty");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            String addURL = client.getURL(url + HFCA_IDENTITY);
            String body = this.toJson();
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), body);
            JsonObject result = client.httpPost(addURL, body, authHdr);

            HFCAIdentity resp = new HFCAIdentity(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while adding the user %s url: %s  %s ", this.getEnrollmentID(), url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * modify an identity
     *
     * @param request   Modification request for an identity
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was added along with secret
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity modifyIdentity(User registrar) throws IdentityException, InvalidArgumentException {
        HFCAClient client = this.client;
        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (Utils.isNullOrEmpty(this.getEnrollmentID())) {
            throw new InvalidArgumentException("ID to be modified can't be null or empty");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            String putURL = client.getURL(url + HFCA_IDENTITY + "/" + this.getEnrollmentID());
            String body = this.toJson();
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), body);

            JsonObject result = client.httpPut(putURL, body, authHdr);

            HFCAIdentity resp = new HFCAIdentity(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while modifying the user %s by %s url: %s  %s ", this.getEnrollmentID(), registrar.getMspId(), url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }
    }

    /**
     * delete an identity
     *
     * @param request   Modification request for an identity
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was added along with secret
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity deleteIdentity(User registrar) throws IdentityException, InvalidArgumentException {
        HFCAClient client = this.client;
        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        if (this.enrollmentID == null) {
            throw new InvalidArgumentException("Id to be removed is required");
        }

        String url = client.url;

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            String deleteURL = client.getURL(url + HFCA_IDENTITY + "/" + this.enrollmentID);
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = client.httpDelete(deleteURL, authHdr);

            HFCAIdentity resp = new HFCAIdentity(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while deleting the user %s url: %s  %s ", this.enrollmentID, url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

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
        ob.add("id", enrollmentID);
        ob.add("type", type);
        if (null != maxEnrollments) {

            ob.add("max_enrollments", maxEnrollments);
        }
        if (affiliation != null) {
            ob.add("affiliation", affiliation);
        }

        JsonArrayBuilder ab = Json.createArrayBuilder();
        for (Attribute attr : attrs) {
            ab.add(attr.toJsonObject());
        }
        ob.add("attrs", ab.build());

        JsonObjectBuilder ob2 = Json.createObjectBuilder();
        ob2.add("info", ob);
        if (this.secret != null) {
            ob2.add("secret", secret);
        }
        if (caName != null) {
            ob2.add(HFCAClient.FABRIC_CA_REQPROP, caName);
        }
        return ob2.build();
    }
}
