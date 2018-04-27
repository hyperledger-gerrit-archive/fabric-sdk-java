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

package org.hyperledger.fabric.sdkintegration;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.junit.Test;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;
import static org.junit.Assert.assertFalse;

public class ServiceDiscoveryIT {
    private static final TestConfig testConfig = TestConfig.getConfig();

    File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");

    private static final Pattern PEER_ENDPOINT_PATTERN = Pattern.compile("^peer\\d\\.org\\d\\.example\\.com:\\d\\d\\d\\d$");

    String CHAIN_CODE_NAME = "example_cc_go";
    TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

    @Test
    public void setup() throws Exception {
        //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
        //   MUST be replaced with more robust application implementation  (Database, LDAP)
        out("\n\n\nRUNNING: %s.\n", "ServiceDiscoveryIT");

        SampleStore sampleStore = new SampleStore(sampleStoreFile);

        //  SampleUser peerAdmin = sampleStore.getMember("admin", "peerOrg1");
        SampleUser user1 = sampleStore.getMember("user1", "peerOrg1");
        HFClient client = HFClient.createNewInstance();
        testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        SampleOrg peerOrg1 = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        String pem = sampleStore.getClientPEMTLSCertificate(peerOrg1);

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(user1);

        Properties properties = new Properties();
        properties.put("pemFile", "src/test/fixture/sdkintegration/e2e-2Orgs/v1.1/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/server.crt");
        properties.put("hostnameOverride", "peer0.org1.example.com");

        final String clientPEMTLSKey = sampleStore.getClientPEMTLSKey(peerOrg1);
        properties.put("clientKeyBytes", clientPEMTLSKey.getBytes(UTF_8));

        final String clientPEMTLSCertificate = sampleStore.getClientPEMTLSCertificate(peerOrg1);
        if (clientPEMTLSCertificate != null) {
            properties.put("clientCertBytes", clientPEMTLSCertificate.getBytes(UTF_8));
        }

        //Create initial discovery peer.
        Peer discoveryPeer = client.newPeer("peer0.org1.example.com", "grpcs://localhost:7051", properties);
        Channel foo = client.newChannel("foo"); //create channel that will be discovered.

        foo.addPeer(discoveryPeer, createPeerOptions().setPeerRoles(EnumSet.of(PeerRole.SERVICE_DISCOVERY)));

        // Application override any discovered peer creation.
        foo.setSDPeerAddition(sdPeerAdditionInfo -> {

            String endpoint = sdPeerAdditionInfo.getEndpoint();

            assertTrue(PEER_ENDPOINT_PATTERN.matcher(endpoint).matches());

            String port = endpoint.split(":")[1];

            String newEndpoint = "localhost:" + port; // for testing here but up to application - rarely need to change endpoint
            Peer peer = sdPeerAdditionInfo.getEndpointMap().get(newEndpoint); // maybe there already.
            if (null == peer) {

                Properties peerProperties = sdPeerAdditionInfo.getServiceDiscoveryPeer().getProperties();
                peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000); //application adjust grpc maxInboundMessageSize

                peer = sdPeerAdditionInfo.getClient().newPeer(sdPeerAdditionInfo.getEndpoint(),
                        sdPeerAdditionInfo.getServiceDiscoveryPeer().getProtocol() + "://" + newEndpoint,
                        peerProperties);
                sdPeerAdditionInfo.getChannel().addPeer(peer, createPeerOptions().setPeerRoles(
                        EnumSet.of(PeerRole.ENDORSING_PEER, PeerRole.SERVICE_DISCOVERY, PeerRole.EVENT_SOURCE))); //application can decide on roles.
            }

            return peer;

        });

        //Application override default orderer creation.

        foo.setSDOrdererAddition(sdOrdererAdditionInfo -> {

            String endpoint = sdOrdererAdditionInfo.getEndpoint();
            assertEquals("orderer.example.com", endpoint.split(":")[0]);

            String port = endpoint.split(":")[1];
            String newEndpoint = "localhost:" + port;

            Orderer orderer = sdOrdererAdditionInfo.getEndpointMap().get(newEndpoint);
            if (null == orderer) {

                Properties ordererProperties = sdOrdererAdditionInfo.getServiceDiscoveryPeer().getProperties();
                //application want's additional grpc properties.
                ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
                ordererProperties.put("hostnameOverride", "orderer.example.com");
                ordererProperties.put("pemFile", "src/test/fixture/sdkintegration/e2e-2Orgs/v1.1/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tls/server.crt");

                orderer = sdOrdererAdditionInfo.getClient().newOrderer(sdOrdererAdditionInfo.getEndpoint(),
                        sdOrdererAdditionInfo.getServiceDiscoveryPeer().getProtocol() + "://" + newEndpoint,
                        ordererProperties);
                sdOrdererAdditionInfo.getChannel().addOrderer(orderer);
            }

            return orderer;

        });

        foo.initialize(); // initialize the channel.

        Set<String> expect = new HashSet<>(Arrays.asList("grpcs://localhost:7050")); //discovered orderer
        for (Orderer orderer : foo.getOrderers()) {
            expect.remove(orderer.getUrl());
        }
        assertTrue(expect.isEmpty());

        expect = new HashSet<>(Arrays.asList("grpcs://localhost:7051")); //discovered peer.
        for (Peer peer : foo.getPeers()) {
            expect.remove(peer.getUrl());
        }
        assertTrue(expect.isEmpty());

        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME);

        ChaincodeID chaincodeID = chaincodeIDBuilder.build();

        ///////////////
        /// Send transaction proposal to all peers
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
        transactionProposalRequest.setFcn("move");
        transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
        transactionProposalRequest.setArgs("a", "b", "1");

        //Send proposal request discovering the what endorsers (peers) are needed.
        Collection<ProposalResponse> transactionPropResp = foo.sendTransactionProposalToEndorsers(transactionProposalRequest);
        assertFalse(transactionPropResp.isEmpty());

        String expectedTransactionId = null;

        final StringBuilder evenTransactionId = new StringBuilder();

        for (ProposalResponse response : transactionPropResp) {
            expectedTransactionId = response.getTransactionID();
            if (response.getStatus() != ProposalResponse.Status.SUCCESS || !response.isVerified()) {

                fail("Failure is not an option.");
            }
        }
        //Send it to the orderer that was discovered.
        foo.sendTransaction(transactionPropResp).thenApply(transactionEvent -> {

            evenTransactionId.setLength(0);

            evenTransactionId.append(transactionEvent.getTransactionID());

            return null;
        }).exceptionally(e -> {
            if (e instanceof TransactionEventException) {
                BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                if (te != null) {
                    throw new AssertionError(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()), e);
                }
            }

            throw new AssertionError(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()), e);

        }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

        assertEquals(expectedTransactionId, evenTransactionId.toString());

    }

    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

}
