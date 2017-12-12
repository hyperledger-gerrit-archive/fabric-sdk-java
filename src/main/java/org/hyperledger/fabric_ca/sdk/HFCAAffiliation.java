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
import org.hyperledger.fabric_ca.sdk.exception.HTTPException;
import org.hyperledger.fabric_ca.sdk.exception.IdentityException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import static java.lang.String.format;

// Hyperledger Fabric CA Affiliation information
public class HFCAAffiliation {

    private String name;
    private String newName;

    private HFCAClient client;

    // Affiliations affected by this affiliation request
    private Collection<HFCAAffiliation> childHFCAAffiliations = new ArrayList<HFCAAffiliation>();
    // Identities affected by this affiliation request
    private Collection<HFCAIdentity> identities = new ArrayList<HFCAIdentity>();

    static final String HFCA_AFFILIATION = HFCAClient.HFCA_CONTEXT_ROOT + "affiliations";
    private static final Log logger = LogFactory.getLog(HFCAAffiliation.class);

    HFCAAffiliation(String name, HFCAClient client) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Affiliation name cannot be null or empty");
        }

        if (client.getCryptoSuite() == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        this.name = name;
        this.client = client;
    }

    HFCAAffiliation(JsonObject result) {
        generateResponse(result);
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
     * The name of the new affiliation
     *
     * @return The affiliation name.
     */

    public String getNewName() {
        return newName;
    }

    /**
     * The name of the new affiliation
     *
     */

    public void setNewName(String newName) {
        this.newName = newName;
    }

    /**
     * The names of all affiliations
     * affected by request
     *
     * @return The affiliation name.
     */

    public Collection<HFCAAffiliation> getChildHFCAAffiliations() {
        return childHFCAAffiliations;
    }

    /**
     * The identities affected during request. Identities are only returned
     * for update and delete requests. Read and Create do not return identities
     *
     * @return The identities affected.
     */

    public Collection<HFCAIdentity> getIdentities() {
        return identities;
    }

    /**
     * The identities affected during request
     * @param name Name of the child affiliation
     *
     * @return The requested child affiliation
     * @throws InvalidArgumentException
     */
    public HFCAAffiliation createChild(String name) throws InvalidArgumentException {
        return new HFCAAffiliation(this.name + "." + name, this.client);
    }

    /**
     * Gets child affiliation by name
     * @param name Name of the child affiliation to get
     *
     * @return The requested child affiliation
     */
    public HFCAAffiliation getChild(String name) {
        for (HFCAAffiliation childAff : this.childHFCAAffiliations) {
            if (childAff.getName().equals(name)) {
                return childAff;
            }
        }
        return null;
    }

    /**
     * gets a specific affiliation
     *
     * @param registrar The identity of the registrar
     * @return Returns response
     * @throws AffiliationException if getting an affiliation fails.
     * @throws InvalidArgumentException
     */

    public Response read(User registrar) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String readAffURL = "";
        try {
            readAffURL = HFCA_AFFILIATION + "/" + name;
            logger.debug(format("affiliation  url: %s, registrar: %s", readAffURL, registrar.getName()));

            JsonObject result = client.httpGet(readAffURL, registrar);

            logger.debug(format("affiliation  url: %s, registrar: %s done.", readAffURL, registrar));
            return getResponse(result);
        } catch (HTTPException e) {
            String msg = format("[Code: %d] - Error while getting affiliation '%s' from url '%s': %s", e.getStatusCode(), this.name, readAffURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        } catch (Exception e) {
            String msg = format("Error while getting affiliation %s url: %s  %s ", this.name, readAffURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * create an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return Response of request
     * @throws AffiliationException    if adding an affiliation fails.
     * @throws InvalidArgumentException
     */

    public Response create(User registrar) throws AffiliationException, InvalidArgumentException {
        return create(registrar, false);
    }

    /**
     * create an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @param force Forces the creation of parent affiliations
     * @return Response of request
     * @throws AffiliationException    if adding an affiliation fails.
     * @throws InvalidArgumentException
     */
    public Response create(User registrar, boolean force) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String createURL = "";
        try {
            createURL = client.getURL(HFCA_AFFILIATION);
            logger.debug(format("affiliation  url: %s, registrar: %s", createURL, registrar.getName()));

            Map<String, String> queryParm = new HashMap<String, String>();
            queryParm.put("force", String.valueOf(force));
            String body = client.toJson(affToJsonObject());
            JsonObject result = client.httpPost(createURL, body, registrar);

            logger.debug(format("identity  url: %s, registrar: %s done.", createURL, registrar));
            return getResponse(result);
        } catch (HTTPException e) {
            String msg = format("[Code: %d] - Error while creating affiliation '%s' from url '%s': %s", e.getStatusCode(), this.name, createURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        } catch (Exception e) {
            String msg = format("Error while creating affiliation %s url: %s  %s ", this.name, createURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * update an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return Response of request
     * @throws AffiliationException If updating an affiliation fails
     * @throws InvalidArgumentException
     */

    public Response update(User registrar) throws AffiliationException, InvalidArgumentException {
        return update(registrar, false);
    }

    /**
     * update an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @param force Forces updating of child affiliations
     * @return Response of request
     * @throws AffiliationException If updating an affiliation fails
     * @throws InvalidArgumentException
     */
    public Response update(User registrar, boolean force) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }
        if (Utils.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Affiliation name cannot be null or empty");
        }

        String updateURL = "";
        try {
            Map<String, String> queryParm = new HashMap<String, String>();
            queryParm.put("force", String.valueOf(force));
            updateURL = client.getURL(HFCA_AFFILIATION + "/" + this.name, queryParm);

            logger.debug(format("affiliation  url: %s, registrar: %s", updateURL, registrar.getName()));

            String body = client.toJson(affToJsonObject());
            JsonObject result = client.httpPut(updateURL, body, registrar);

            generateResponse(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", updateURL, registrar));
            return getResponse(result);
        } catch (HTTPException e) {
            String msg = format("[Code: %d] - Error while updating affiliation '%s' from url '%s': %s", e.getStatusCode(), this.name, updateURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        } catch (Exception e) {
            String msg = format("Error while updating affiliation %s url: %s  %s ", this.name, updateURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * delete an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return Response of request
     * @throws AffiliationException    if deleting an affiliation fails.
     * @throws InvalidArgumentException
     */

    public Response delete(User registrar) throws AffiliationException, InvalidArgumentException {
        return delete(registrar, false);
    }

    /**
     * delete an affiliation
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @param force Forces the deletion of affiliation
     * @return Response of request
     * @throws AffiliationException    if deleting an affiliation fails.
     * @throws InvalidArgumentException
     */
    public Response delete(User registrar, boolean force) throws AffiliationException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String deleteURL = "";
        try {
            Map<String, String> queryParm = new HashMap<String, String>();
            queryParm.put("force", String.valueOf(force));
            deleteURL = client.getURL(HFCA_AFFILIATION + "/" + this.name, queryParm);

            logger.debug(format("affiliation  url: %s, registrar: %s", deleteURL, registrar.getName()));

            JsonObject result = client.httpDelete(deleteURL, registrar);

            logger.debug(format("identity  url: %s, registrar: %s done.", deleteURL, registrar));
            return getResponse(result);
        } catch (HTTPException e) {
            String msg = format("[Code: %d] - Error while deleting affiliation '%s' from url '%s': %s", e.getStatusCode(), this.name, deleteURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }  catch (Exception e) {
            String msg = format("Error while deleting affiliation %s url: %s  %s ", this.name, deleteURL, e.getMessage());
            AffiliationException affiliationException = new AffiliationException(msg, e);
            logger.error(msg);
            throw affiliationException;
        }
    }

    /**
     * Response of affiliation requests
     *
     */
    public class Response {

        // Affiliations affected by this affiliation request
        private Collection<HFCAAffiliation> childHFCAAffiliations = new ArrayList<HFCAAffiliation>();
        // Identities affected by this affiliation request
        private Collection<HFCAIdentity> identities = new ArrayList<HFCAIdentity>();

        private int statusCode = 200;

        /**
         * The identities affected during request
         *
         * @return The identities affected.
         */

        public Collection<HFCAIdentity> getIdentities() {
            return identities;
        }

        /**
         * The names of all affiliations
         * affected by request
         *
         * @return The affiliation name.
         */

        public Collection<HFCAAffiliation> getChildHFCAAffiliations() {
            return childHFCAAffiliations;
        }

        /**
         * @return HTTP status code
         */
        public int getStatusCode() {
            return statusCode;
        }

         Response(JsonObject result) {
            if (result.containsKey("affiliations")) {
                JsonArray affiliations = result.getJsonArray("affiliations");
                if (affiliations != null && !affiliations.isEmpty()) {
                    for (int i = 0; i < affiliations.size(); i++) {
                        JsonObject aff = affiliations.getJsonObject(i);
                        this.childHFCAAffiliations.add(new HFCAAffiliation(aff));
                    }
                }
            }
            if (result.containsKey("identities")) {
                JsonArray ids = result.getJsonArray("identities");
                if (ids != null && !ids.isEmpty()) {
                    for (int i = 0; i < ids.size(); i++) {
                        JsonObject id = ids.getJsonObject(i);
                        HFCAIdentity hfcaID = new HFCAIdentity(id);
                        this.identities.add(hfcaID);
                    }
                }
            }
            if (result.containsKey("statusCode")) {
                this.statusCode = result.getInt("statusCode");
            }
        }
    }

    Response getResponse(JsonObject result) {
        if (result.containsKey("name")) {
            this.name = result.getString("name");
        }
        return new Response(result);
    }

    private void generateResponse(JsonObject result) {
        if (result.containsKey("name")) {
            this.name = result.getString("name");
        }
        if (result.containsKey("affiliations")) {
            JsonArray affiliations = result.getJsonArray("affiliations");
            if (affiliations != null && !affiliations.isEmpty()) {
                for (int i = 0; i < affiliations.size(); i++) {
                    JsonObject aff = affiliations.getJsonObject(i);
                    this.childHFCAAffiliations.add(new HFCAAffiliation(aff));
                }
            }
        }
        if (result.containsKey("identities")) {
              JsonArray ids = result.getJsonArray("identities");
              if (ids != null && !ids.isEmpty()) {
                  for (int i = 0; i < ids.size(); i++) {
                      JsonObject id = ids.getJsonObject(i);
                      HFCAIdentity hfcaID = new HFCAIdentity(id);
                      this.identities.add(hfcaID);
                  }
              }
        }
    }

    // Convert the affiliation request to a JSON object
    private JsonObject affToJsonObject() {
        JsonObjectBuilder ob = Json.createObjectBuilder();
        if (client.getCAName() != null) {
            ob.add(HFCAClient.FABRIC_CA_REQPROP, client.getCAName());
        }
        if (this.newName != null) {
            ob.add("name", newName);
            this.newName = null;
        } else {
            ob.add("name", name);
        }

        return ob.build();
    }
}