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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.HFCAInfo;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import static java.lang.String.format;

public class BMXHyperldgerTest {

    static final SampleStore SS = new SampleStore(new File("BMXHyperldgerTest.Store.properties"));

    public static final String PEER_ADMIN_NAME = "admin";
    public static final String TEST_CHANNEL = "ricks-test-channel";
    public static final String NETWORK_CONFIG_PEERORG = "PeerOrg1"; //Only test with this peer org

//    public static final String NETWORK_CONFIG_PEERORG_CA = "fabric-ca-peerorg1-14662a"; //Only test with this CA
//    public static final String NETWORK_CONFIG_FILE = "bmxcreds_local.json";

    public static final String NETWORK_CONFIG_PEERORG_CA = "fabric-ca-peerorg1-16306a"; //Only test with this CA
    public static final String NETWORK_CONFIG_FILE = "bmxcreds.json";

    public static void main(String[] args) throws Exception {
        new BMXHyperldgerTest().run(args);

    }

    private void run(String[] args) throws Exception {

        // Network network = parseFile(new File("bmxcreds.json"));
        NetworkConfig networkConfig = parseFile(new File(NETWORK_CONFIG_FILE));

        NetworkConfig.OrganizationConfig peerOrg1 = networkConfig.getOrganization(NETWORK_CONFIG_PEERORG);
        NetworkConfig.OrganizationConfig.CertificateAuthorityConfig certificateAuthority = peerOrg1.getCertificateAuthority(NETWORK_CONFIG_PEERORG_CA);

        SampleUser admin;

        if (!SS.hasMember(PEER_ADMIN_NAME, NETWORK_CONFIG_PEERORG)) {

            MyUser casadmin = certificateAuthority.getRegistrar(PEER_ADMIN_NAME);
            admin = SS.getMember(PEER_ADMIN_NAME, NETWORK_CONFIG_PEERORG);
            admin.setEnrollmentSecret(casadmin.getEnrollSecret());
            admin.setAffiliation(casadmin.getAffiliation());
            admin.setMspId(casadmin.getMspId());

        } else {

            admin = SS.getMember(PEER_ADMIN_NAME, NETWORK_CONFIG_PEERORG);

        }

        assert admin != null;

        if (!admin.isEnrolled()) {
            HFCAClient hfcaClient = constructFabricCAEndpoint(networkConfig);
            enrollUser(hfcaClient, admin);
            out("Peer Admin %s not previously enrolled. Make sure all peers include certificate.", admin.getName());
            printUser(admin);
            System.exit(8);
        }

        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        client.setUserContext(admin);

        Map<String, Orderer> orderers = new HashMap<>();
        Map<String, Peer> peers = new HashMap<>();
        Map<String, EventHub> eventHubs = new HashMap<>();

        constructFabricEndpoints(client, networkConfig, orderers, peers, eventHubs);

        assert !orderers.isEmpty() : "no Orderers were found in network configuration file!";

        assert !peers.isEmpty() : "no Peers were found in network configuration file!";

        assert !eventHubs.isEmpty() : "no Event hubs were found in network configuration file!";

        joinPeers(client, TEST_CHANNEL, orderers.values().iterator().next(), peers.values());

    }

    private void joinPeers(HFClient client, String channelName, Orderer orderer, Collection<Peer> peers) throws ProposalException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {

        for (Peer peer : peers) {

            Set<String> channels = client.queryChannels(peer);
            if (!channels.contains(channelName)) {

                out("found it!");

            }

        }

    }

    void enrollUser(HFCAClient hfcaClient, SampleUser user) throws EnrollmentException, InvalidArgumentException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {

        Enrollment enroll = hfcaClient.enroll(user.getName(), user.getEnrollmentSecret());
        user.setEnrollment(enroll);
        User.userContextCheck(user);
        printUser(user);
    }

    private void printUser(User user) {
        out("User: %s, MSPID: %s\nEnrollment certificate:\n%s", user.getName(), user.getMspId(), user.getEnrollment().getCert());
    }

    private void constructFabricEndpoints(HFClient client, NetworkConfig networkConfig,
                                          Map<String, Orderer> orderers,
                                          Map<String, Peer> peers, Map<String, EventHub> eventHubs) throws Exception {

        for (NetworkConfig.OrdererConfig orderer : networkConfig.getOrderers().values()) {

            orderers.put(orderer.getName(), client.newOrderer(orderer.getName(), orderer.getURL(),
                    orderer.getProperties()));
        }

        for (NetworkConfig.OrganizationConfig.PeerConfig peerConfig : networkConfig.getOrganization(NETWORK_CONFIG_PEERORG).getPeers().values()) {

            peers.put(peerConfig.getName(), client.newPeer(peerConfig.getName(), peerConfig.getURL(),
                    peerConfig.getProperties()));
            if (peerConfig.getEventURL() != null || !peerConfig.getEventURL().isEmpty()) {
                EventHub eventHub = client.newEventHub(peerConfig.getName(), peerConfig.getEventURL(), peerConfig.getProperties());
                eventHubs.put(eventHub.getName(), eventHub);

            }
        }

    }

    private HFCAClient constructFabricCAEndpoint(NetworkConfig networkConfig) throws Exception {

        //Get a fabric ca for the network.

        NetworkConfig.OrganizationConfig peerOrg1 = networkConfig.getOrganization(NETWORK_CONFIG_PEERORG);

        NetworkConfig.OrganizationConfig.CertificateAuthorityConfig certificateAuthority = peerOrg1.getCertificateAuthority(NETWORK_CONFIG_PEERORG_CA);

        Properties properties = new Properties();
        properties.put("pemBytes", certificateAuthority.getTLSCerts().getBytes());
        properties.setProperty("allowAllHostNames", "true");
        HFCAClient hfcaClient = HFCAClient.createNewInstance(certificateAuthority.getCAName(),
                certificateAuthority.getURL(), properties);
        hfcaClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        HFCAInfo info = hfcaClient.info(); //basic check if it connects.
        assert info != null : "hfcaClient.info() is null";
        return hfcaClient;

    }

    private static NetworkConfig parseFile(File parseFilie) throws FileNotFoundException {

        JsonReader reader = Json.createReader(new BufferedReader(new FileReader(parseFilie)));
        JsonObject jobj = (JsonObject) reader.read();
        NetworkConfig networkConfig = new NetworkConfig(jobj);

        return networkConfig;

    }

    static class NetworkConfig {

        private final JsonObject value;

        NetworkConfig(JsonObject jobj) {
            value = jobj;
        }

        Map<String, OrganizationConfig> orgs;

        OrganizationConfig getOrganization(String name) {
            return getOrganizations().get(name);
        }

        Map<String, OrganizationConfig> getOrganizations() {
            if (orgs == null) {
                orgs = new HashMap<>();
                for (Map.Entry<String, JsonValue> orgEntry : value.getJsonObject("organizations").entrySet()) {

                    OrganizationConfig organizationConfig = new OrganizationConfig(orgEntry.getKey(), (JsonObject) orgEntry.getValue());
                    orgs.put(organizationConfig.getName(), organizationConfig);
                }

            }

            return orgs;
        }

        Map<String, OrdererConfig> orderers = null;

        Map<String, OrdererConfig> getOrderers() {
            if (orderers == null) {
                orderers = new HashMap<>();
                for (Map.Entry<String, JsonValue> orgEntry : value.getJsonObject("orderers").entrySet()) {

                    OrdererConfig ordererConfig = new OrdererConfig(orgEntry.getKey(), (JsonObject) orgEntry.getValue());
                    orderers.put(ordererConfig.getName(), ordererConfig);
                }

            }

            return orderers;
        }

        OrdererConfig getOrderer(String name) {
            return getOrderers().get(name);
        }

        class OrganizationConfig {
            private final JsonObject value;
            private final String name;

            OrganizationConfig(String key, JsonObject value) {
                this.value = value;
                this.name = key;
            }

            String getName() {
                return name;
            }

            String getMspid() {
                return value.getString("mspid");
            }

            public CertificateAuthorityConfig getCertificateAuthority(String name) {
                return getCertificateAuthorities().get(name);
            }

            Map<String, CertificateAuthorityConfig> certificateAuthorities;

            public Map<String, CertificateAuthorityConfig> getCertificateAuthorities() {
                if (null == certificateAuthorities) {

                    JsonObject cas = NetworkConfig.this.value.getJsonObject("certificateAuthorities");

                    certificateAuthorities = new HashMap<>();
                    for (Map.Entry<String, JsonValue> ca : cas.entrySet()) {

                        CertificateAuthorityConfig cao = new CertificateAuthorityConfig(ca.getKey(), ca.getValue().asJsonObject(), OrganizationConfig.this.getMspid());
                        certificateAuthorities.put(cao.getName(), cao);

                    }

                }
                return certificateAuthorities;

            }

            class CertificateAuthorityConfig extends EndPoint {

                final String mspid;

                private CertificateAuthorityConfig(String name, JsonObject value, String mspid) {
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
                                OrganizationConfig.this.getMspid(), registrar.getEnrollSecret());
                        myUser.affiliation = registrar.getAffiliation();

                        ret.put(registrar.getEnrollId(), myUser);

                    }

                    return ret;
                }

            }

            Map<String, PeerConfig> peers;

            Map<String, PeerConfig> getPeers() {

                if (peers == null) {

                    peers = new HashMap<>();

                    for (Map.Entry<String, JsonValue> pe : NetworkConfig.this.value.getJsonObject("peers").entrySet()) {

                        PeerConfig peerConfig = new PeerConfig(pe.getKey(), pe.getValue().asJsonObject());
                        peers.put(peerConfig.getName(), peerConfig);

                    }
                }

                return peers;

            }

            PeerConfig getPeer(String name) {

                return getPeers().get(name);

            }

            class PeerConfig extends EndPoint {

                PeerConfig(String name, JsonObject value) {
                    super(name, value);
                }

                public String getEventURL() {
                    return value.getString("eventUrl");
                }

            }

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

            Properties properties = null;

            public Properties getProperties() {
                if (properties == null) {
                    properties = new Properties();
                    if (value.containsKey("grpcOptions")) {
                        JsonObject grpcOptions = value.getJsonObject("grpcOptions");
                        if (null != grpcOptions) {
                            JsonNumber jsonNumber = grpcOptions.getJsonNumber("grpc.http2.keepalive_time");
                            if (null != jsonNumber) {
                                properties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {jsonNumber.longValue(), TimeUnit.MINUTES});
                            }
                        }
                    }

                }
                return properties;
            }
        }

        class OrdererConfig extends EndPoint {
            OrdererConfig(String name, JsonObject value) {
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

    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

}
