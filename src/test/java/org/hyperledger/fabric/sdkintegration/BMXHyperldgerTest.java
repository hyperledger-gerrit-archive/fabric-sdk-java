/*
 *
 *  Copyright 2017 IBM - All Rights Reserved.
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

package org.hyperledger.fabric.sdkintegration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;

import static java.lang.String.format;

public class BMXHyperldgerTest {

    public static void main(String[] args) throws Exception {
        new BMXHyperldgerTest().run(args);

    }

    private void run(String[] args) throws Exception {

        // Network network = parseFile(new File("bmxcreds.json"));
        Network network = parseFile(new File("bmxcreds_local.json"));

        Network.Organization peerOrg1 = network.getOrganization("PeerOrg1");
        Network.Organization.CA certificateAuthority = peerOrg1.getCertificateAuthority("fabric-ca-peerorg1-14662a");
        MyUser admin = certificateAuthority.getRegistrar("admin");

        Properties properties = new Properties();
        properties.put("pemBytes", certificateAuthority.getTLSCerts().getBytes());
        properties.setProperty("allowAllHostNames", "true");
        HFCAClient hfcaClient = HFCAClient.createNewInstance(certificateAuthority.getCAName(),
                certificateAuthority.getURL(), properties);
        hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        HFCAInfo info = hfcaClient.info(); //basic check if it connects.
        assert info != null : "hfcaClient.info() is null";

        Enrollment enroll = hfcaClient.enroll(admin.getName(), admin.getEnrollSecret());
        admin.setEnrollment(enroll);

    }

    private static Network parseFile(File parseFilie) throws FileNotFoundException {

        JsonReader reader = Json.createReader(new BufferedReader(new FileReader(parseFilie)));
        JsonObject jobj = (JsonObject) reader.read();
        Network network = new Network(jobj);

        return network;

    }

    static class Network {

        private final JsonObject value;

        Network(JsonObject jobj) {
            value = jobj;
        }

        Map<String, Organization> orgs;

        Organization getOrganization(String name) {
            return getOrganizations().get(name);
        }

        Map<String, Organization> getOrganizations() {
            if (orgs == null) {
                orgs = new HashMap<>();
                for (Map.Entry<String, JsonValue> orgEntry : value.getJsonObject("organizations").entrySet()) {

                    Organization organization = new Organization(orgEntry.getKey(), (JsonObject) orgEntry.getValue());
                    orgs.put(organization.getName(), organization);
                }

            }

            return orgs;
        }

        Map<String, Orderer> orderers = null;

        Map<String, Orderer> getOrderers() {
            if (orderers == null) {
                orderers = new HashMap<>();
                for (Map.Entry<String, JsonValue> orgEntry : value.getJsonObject("orderers").entrySet()) {

                    Orderer orderer = new Orderer(orgEntry.getKey(), (JsonObject) orgEntry.getValue());
                    orderers.put(orderer.getName(), orderer);
                }

            }

            return orderers;
        }

        Orderer getOrderer(String name) {
            return getOrderers().get(name);
        }

        class Organization {
            private final JsonObject value;
            private final String name;

            Organization(String key, JsonObject value) {
                this.value = value;
                this.name = key;
            }

            String getName() {
                return name;
            }

            String getMspid() {
                return value.getString("mspid");
            }

            public CA getCertificateAuthority(String name) {
                return getCertificateAuthorities().get(name);
            }

            Map<String, CA> certificateAuthorities;

            public Map<String, CA> getCertificateAuthorities() {
                if (null == certificateAuthorities) {

                    JsonObject cas = Network.this.value.getJsonObject("certificateAuthorities");

                    certificateAuthorities = new HashMap<>();
                    for (Map.Entry<String, JsonValue> ca : cas.entrySet()) {

                        CA cao = new CA(ca.getKey(), ca.getValue().asJsonObject(), Organization.this.getMspid());
                        certificateAuthorities.put(cao.getName(), cao);

                    }

                }
                return certificateAuthorities;

            }

            class CA extends EndPoint {

                final String mspid;

                private CA(String name, JsonObject value, String mspid) {
                    super(name, value);

                    this.mspid = mspid;

                }

                public String getCAName() {
                    return value.getString("caName");
                }

                MyUser getRegistrar(String name) {
                    return getRegistrars().get(name);
                }

                public Map<String, MyUser> getRegistrars() {

                    Map<String, MyUser> ret = new HashMap<>();

                    JsonArray registrars = value.getJsonArray("registrar");

                    assert registrars != null : "Expected registars to not be null.";

                    for (Iterator<JsonValue> it = registrars.iterator(); it.hasNext();
                            ) {
                        JsonObject x = (JsonObject) it.next();
                        Registrar registrar = new Registrar(x);

                        MyUser myUser = new MyUser(registrar.getEnrollId(),
                                Organization.this.getMspid(), registrar.getEnrollSecret());
                        myUser.affiliation = registrar.getAffiliation();

                        ret.put(registrar.getEnrollId(), myUser);

                    }

                    return ret;
                }

            }

        }

        static void out(String format, Object... args) {

            System.err.flush();
            System.out.flush();

            System.out.println(format(format, args));
            System.err.flush();
            System.out.flush();

        }

        class Registrar {
            private final JsonObject value;

            Registrar(JsonObject value) {

                this.value = value;
            }

            public String getEnrollId() {
                return value.getString("enrollId");
            }

            public String getAffiliation() {
                return value.getString("affiliation");
            }

            public String getEnrollSecret() {
                return value.getString("enrollSecret");
            }

        }

        class Orderer extends EndPoint {
            Orderer(String name, JsonObject value) {
                super(name, value);
            }

        }

        class EndPoint {

            final JsonObject value;
            public String name;

            EndPoint(String name, JsonObject value) {
                this.name = name;
                this.value = value;
            }

            public String getURL() {
                return value.getString("url");
            }

            public String getName() {
                return name;
            }

            public String getTLSCerts() {
                return value.getJsonObject("tlsCACerts").getString("pem");

            }

        }

        class Peer extends EndPoint {

            Peer(String name, JsonObject value) {
                super(name, value);
            }

        }
    }

    static class MyUser implements User {
        private final String mspId;
        private final String enrollSecret;
        String name;
        private String affiliation;

        public void setEnrollment(Enrollment enrollment) {
            this.enrollment = enrollment;
        }

        Enrollment enrollment;

        MyUser(String name, String mspId, String enrollSecret) {
            this.name = name;
            this.mspId = mspId;
            this.enrollSecret = enrollSecret;

        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Set<String> getRoles() {
            return null;
        }

        @Override
        public String getAccount() {
            return null;
        }

        @Override
        public String getAffiliation() {
            return affiliation;
        }

        @Override
        public Enrollment getEnrollment() {
            return enrollment;
        }

        @Override
        public String getMspId() {
            return mspId;
        }

        String getEnrollSecret() {
            return enrollSecret;
        }
    }

}
