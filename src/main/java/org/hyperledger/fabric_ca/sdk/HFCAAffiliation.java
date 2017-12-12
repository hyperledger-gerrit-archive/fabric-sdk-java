/*
 *
 *  Copyright 2016,2017,2018 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric_ca.sdk.exception.AffiliationException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import static java.lang.String.format;

// Hyperledger Fabric CA Affiliation information

public class HFCAAffiliation {

    private String name;
    private boolean force;

    // Affiliations affected by this affiliation request
    private Collection<String> names = new ArrayList<String>();
    // Identities associated by this affiliation request
    private Collection<HFCAIdentity> identities = new ArrayList<HFCAIdentity>();

    // Optional CA name
    private String caName = "";
    private HFCAClient client;

    static final String HFCA_AFFILIATION = HFCAClient.HFCA_CONTEXT_ROOT + "affiliations";
    private static final Log logger = LogFactory.getLog(HFCAAffiliation.class);

    HFCAAffiliation(String name, HFCAClient client) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Affiliation name cannot be null or empty");
        }

        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        this.name = name;
        this.client = client;
    }

    HFCAAffiliation(String name, boolean force, HFCAClient client) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Affiliation name cannot be null or empty");
        }

        if (client.cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        this.name = name;
        this.force = force;
        this.client = client;
    }


    HFCAAffiliation(JsonObject result) {
        getHFCAAffiliation(result);
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
     * The names of all affiliations
     * affected by request
     *
     * @return The affiliation name.
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

    public boolean getForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * The CAName for the Fabric Certificate Authority.
     *
     * @return The CA Name.
     */

    public String getCAName() {
        return caName;
    }

    public void setCAName(String caname) {
        this.caName = caname;
    }

    /**
     * gets a specific affiliation
     *
     * @param registrar The identity of the registrar
     * @throws AffiliationExcpetion    if getting an affilition fails.
     * @throws InvalidArgumentException
     */

    public void get(User registrar) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;

        logger.debug(format("affiliation  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            String getAffURL = client.getURL(url + HFCA_AFFILIATION + "/" + this.name);
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = client.httpGet(getAffURL, authHdr);

            getHFCAAffiliation(result);
            logger.debug(format("affiliation  url: %s, registrar: %s done.", url, registrar));
        } catch (Exception e) {
            String msg = format("Error while getting the affiliation %s url: %s  %s ", this.name, url, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * create an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @throws AffiliationException    if adding an affiliation fails.
     * @throws InvalidArgumentException
     */

    public void create(User registrar) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;
        logger.debug(format("affiliation  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            Map<String, String> queryParm = new HashMap<String, String>();
            queryParm.put("ca", this.caName);
            queryParm.put("force", String.valueOf(this.force));
            String addURL = client.getURL(url + HFCA_AFFILIATION, queryParm);
            String body = this.toJson();
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), body);
            JsonObject result = client.httpPost(addURL, body, authHdr);

            getHFCAAffiliation(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
        } catch (Exception e) {
            String msg = format("Error while creating the affiliation %s url: %s  %s ", this.name, url, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * update an affiliation
     *
     * @param newAffiliation   The affiliation to be updated
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @throws IdentityException    if updating an identity fails.
     * @throws InvalidArgumentException
     */

    public void update(String newAffiliation, User registrar) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;

        logger.debug(format("affiliation  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            Map<String, String> queryParm = new HashMap<String, String>();
            queryParm.put("ca", caName);
            queryParm.put("force", String.valueOf(this.force));
            String modifyURL = client.getURL(url + HFCA_AFFILIATION + "/" + this.name, queryParm);

            this.name = newAffiliation;
            String body = this.toJson();
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), body);
            JsonObject result = client.httpPut(modifyURL, body, authHdr);

            getHFCAAffiliation(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
        } catch (Exception e) {
            String msg = format("Error while updating the affiliation %s url: %s  %s ", this.name, url, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * delete an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @throws AffiliationException    if deleting an affiliation fails.
     * @throws InvalidArgumentException
     */

    public void delete(User registrar) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String url = client.url;

        logger.debug(format("affiliation  url: %s, registrar: %s", url, registrar.getName()));

        try {
            client.setUpSSL();

            Map<String, String> queryParm = new HashMap<String, String>();
            queryParm.put("ca", caName);
            queryParm.put("force", String.valueOf(this.force));
            String deleteURL = client.getURL(url + HFCA_AFFILIATION + "/" + this.name, queryParm);
            String authHdr = client.getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = client.httpDelete(deleteURL, authHdr);

            getHFCAAffiliation(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
        } catch (Exception e) {
            String msg = format("Error while deleting the affiliation %s url: %s  %s ", this.name, url, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    private void getHFCAAffiliation(JsonObject result) {
        if (result.containsKey("info")) {
            JsonObject info = result.getJsonObject("info");
            if (info.containsKey("name")) {
                this.name = info.getString("name");
            }
        }
        if (result.containsKey("affiliations")) {
            JsonArray affiliations = result.getJsonArray("affiliations");
            if (affiliations != null && !affiliations.isEmpty()) {
                for (int i = 0; i < affiliations.size(); i++) {
                    JsonObject aff = affiliations.getJsonObject(i);
                    this.names.add(aff.getString("name"));
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
        if (result.containsKey("caname")) {
            this.caName = result.getString("caname");
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
        ob.add("force", force);
        if (caName != null) {
            ob.add(HFCAClient.FABRIC_CA_REQPROP, caName);
        }

        JsonObjectBuilder info = Json.createObjectBuilder();
        info.add("name", name);
        ob.add("info", info);

        return ob.build();
    }
}