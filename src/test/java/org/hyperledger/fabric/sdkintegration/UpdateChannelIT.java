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
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hyperledger.fabric.protos.peer.Query.ChaincodeInfo;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TestConfigHelper;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.UpdateChannelConfiguration;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Upgrade channel scenario
 */
public class UpdateChannelIT {

    private static final TestConfig testConfig = TestConfig.getConfig();
    private static final String TESTUSER_1_NAME = "user1";
    private static final String TEST_FIXTURES_PATH = "src/test/fixture";

    private static final String CHAIN_CODE_NAME = "example_cc_go";
    private static final String CHAIN_CODE_PATH = "github.com/example_cc";
    private static final String CHAIN_CODE_VERSION = "1";
    private static final String CHAIN_CODE_VERSION_11 = "11";
    private static final String ORIGINAL_BATCH_TIMEOUT = "\"timeout\": \"2s\""; // Batch time out in configtx.yaml
    private static final String UPDATED_BATCH_TIMEOUT = "\"timeout\": \"5s\"";  // What we want to change it to.

    final ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
            .setVersion(CHAIN_CODE_VERSION)
            .setPath(CHAIN_CODE_PATH).build();
    final ChaincodeID chaincodeID_11 = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
            .setVersion(CHAIN_CODE_VERSION_11)
            .setPath(CHAIN_CODE_PATH).build();

    private static final String FOO_CHANNEL_NAME = "foo";
    private static final String BAR_CHANNEL_NAME = "bar";

    String testTxID = null;  // save the CC invoke TxID and use in queries

    private final TestConfigHelper configHelper = new TestConfigHelper();

    private Collection<SampleOrg> testSampleOrgs;

    @Before
    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MalformedURLException {

        out("\n\n\nRUNNING: UpdateChannelIT\n");

        configHelper.clearConfig();
        configHelper.customizeConfig();

        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
        //Set up hfca for each sample org

        for (SampleOrg sampleOrg : testSampleOrgs) {
            String caURL = sampleOrg.getCALocation();
            sampleOrg.setCAClient(HFCAClient.createNewInstance(caURL, null));
        }
    }

    @After
    public void clearConfig() {
        try {
            configHelper.clearConfig();
        } catch (Exception e) {
        }
    }

    @Test
    public void setup() {

        try {

            ////////////////////////////
            // Setup client

            //Create instance of client.
            HFClient client = HFClient.createNewInstance();

            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

            // client.setMemberServices(peerOrg1FabricCA);

            ////////////////////////////
            //Set up USERS

            //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
            //   MUST be replaced with more robust application implementation  (Database, LDAP)
            File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
            sampleStoreFile.deleteOnExit();

            final SampleStore sampleStore = new SampleStore(sampleStoreFile);

            //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface

            ////////////////////////////
            // get users for all orgs

            for (SampleOrg sampleOrg : testSampleOrgs) {

                final String orgName = sampleOrg.getName();
//
//                SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
//                sampleOrg.setAdmin(admin); // The admin of this org.
//
//                // No need to enroll or register all done in End2endIt !
//                SampleUser user = sampleStore.getMember(TESTUSER_1_NAME, orgName);
//                sampleOrg.addUser(user);  //Remember user belongs to this Org
//
                sampleOrg.setPeerAdmin(sampleStore.getMember(orgName + "Admin", orgName));
            }

            ////////////////////////////
            //Reconstruct and run the channels
            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
            Channel fooChannel = reconstructChannel(FOO_CHANNEL_NAME, client, sampleOrg);

            final byte[] channelConfigurationBytes = fooChannel.getChannelConfigurationBytes();

            HttpClients.createDefault();

            HttpClient httpclient = HttpClients.createDefault();
            HttpPost httppost = new HttpPost("http://localhost:7059/protolator/decode/common.Config");
            httppost.setEntity(new ByteArrayEntity(channelConfigurationBytes));

            HttpResponse response = httpclient.execute(httppost);
            int statuscode = response.getStatusLine().getStatusCode();
            assertEquals(200, statuscode);

            //        out("http status code = %d", statuscode);

            String responseAsString = EntityUtils.toString(response.getEntity());

            // out("responseAsString:%s", responseAsString);

            if (!responseAsString.contains(ORIGINAL_BATCH_TIMEOUT)) {

                fail(format("Did not find expected batch timeout '%s', in:%s", ORIGINAL_BATCH_TIMEOUT, responseAsString));
            }

            String updateString = responseAsString.replace(ORIGINAL_BATCH_TIMEOUT, UPDATED_BATCH_TIMEOUT);
            //     out("updateString:%s", updateString);

            httppost = new HttpPost("http://localhost:7059/protolator/encode/common.Config");
            httppost.setEntity(new StringEntity(updateString));

            response = httpclient.execute(httppost);
            statuscode = response.getStatusLine().getStatusCode();
            assertEquals(200, statuscode);
            byte[] newConfigBytes = EntityUtils.toByteArray(response.getEntity());

            httppost = new HttpPost("http://localhost:7059/configtxlator/compute/update-from-configs");

            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody("original", channelConfigurationBytes, ContentType.APPLICATION_OCTET_STREAM, "originalFakeFilename")
                    .addBinaryBody("updated", newConfigBytes, ContentType.APPLICATION_OCTET_STREAM, "updatedFakeFilename")
                    .addBinaryBody("channel", fooChannel.getName().getBytes()).build();

            httppost.setEntity(multipartEntity);

            response = httpclient.execute(httppost);
            statuscode = response.getStatusLine().getStatusCode();
            assertEquals(200, statuscode);

            byte[] updateBytes = EntityUtils.toByteArray(response.getEntity());

            UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration(updateBytes);

            //To change the channel we need to sign with orderer admin certs which crypto gen stores:

            // private key: src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/ordererOrganizations/example.com/users/Admin@example.com/msp/keystore/f1a9a940f57419a18a83a852884790d59b378281347dd3d4a88c2b820a0f70c9_sk
            //certificate:  src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/ordererOrganizations/example.com/users/Admin@example.com/msp/signcerts/Admin@example.com-cert.pem

            String sampleOrgName = sampleOrg.getName();
            SampleUser ordererAdmin = sampleStore.getMember(sampleOrgName + "OrderAdmin", sampleOrgName, "OrdererMSP",
                    Util.findFileSk(Paths.get("src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/ordererOrganizations/example.com/users/Admin@example.com/msp/keystore/").toFile()),
                    Paths.get("src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/ordererOrganizations/example.com/users/Admin@example.com/msp/signcerts/Admin@example.com-cert.pem").toFile());

            client.setUserContext(ordererAdmin);

            fooChannel.updateChannelConfiguration(updateChannelConfiguration, client.getUpdateChannelConfigurationSignature(updateChannelConfiguration, ordererAdmin));

            //Let's add some additional verification...

            final byte[] modChannelBytes = fooChannel.getChannelConfigurationBytes();

            httppost = new HttpPost("http://localhost:7059/protolator/decode/common.Config");
            httppost.setEntity(new ByteArrayEntity(modChannelBytes));

            response = httpclient.execute(httppost);
            statuscode = response.getStatusLine().getStatusCode();
            assertEquals(200, statuscode);

            responseAsString = EntityUtils.toString(response.getEntity());

            // out("responseAsString:%s", responseAsString);

            if (!responseAsString.contains(UPDATED_BATCH_TIMEOUT)) {

                fail(format("Did not find updated expected batch timeout '%s', in:%s", UPDATED_BATCH_TIMEOUT, responseAsString));
            }

            if (responseAsString.contains(ORIGINAL_BATCH_TIMEOUT)) { //Should not have been there.

                fail(format("Found original batch timeout '%s', when it was not expected in:%s", ORIGINAL_BATCH_TIMEOUT, responseAsString));
            }

            out("\n");

            out("That's all folks!");

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    // Disable MethodLength as this method is for instructional purposes and hence
    // we don't want to split it into smaller pieces
    // CHECKSTYLE:OFF: MethodLength
    void runChannel(HFClient client, Channel channel, SampleOrg sampleOrg, final int delta) {
        final String channelName = channel.getName();
        try {

//            final boolean changeContext = false; // BAR_CHANNEL_NAME.equals(channel.getName()) ? true : false;
            final boolean changeContext = BAR_CHANNEL_NAME.equals(channel.getName());

            out("Running Channel %s with a delta %d", channelName, delta);
            channel.setTransactionWaitTime(testConfig.getTransactionWaitTime());
            channel.setDeployWaitTime(testConfig.getDeployWaitTime());

            ////////////////////////////
            // Send Query Proposal to all peers see if it's what we expect from end of End2endIT
            //
            queryChaincodeForExpectedValue(client, channel, "" + (300 + delta), chaincodeID);

            //Set user context on client but use explicit user contest on each call.
            if (changeContext) {
                client.setUserContext(sampleOrg.getUser(TESTUSER_1_NAME));

            }

            // exercise v1 of chaincode

            moveAmount(client, channel, chaincodeID, "25", changeContext ? sampleOrg.getPeerAdmin() : null).thenApply(transactionEvent -> {
                try {

                    waitOnFabric();

                    queryChaincodeForExpectedValue(client, channel, "" + (325 + delta), chaincodeID);

                    //////////////////
                    // Start of upgrade first must install it.

                    ///////////////
                    ////
                    InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
                    installProposalRequest.setChaincodeID(chaincodeID);
                    ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
                    installProposalRequest.setChaincodeSourceLocation(new File(TEST_FIXTURES_PATH + "/sdkintegration/gocc/sample_11"));
                    installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION_11);
                    installProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());

                    if (changeContext) {
                        installProposalRequest.setUserContext(sampleOrg.getPeerAdmin());
                    }

                    out("Sending install proposal for channel: %s", channel.getName());

                    ////////////////////////////
                    // only a client from the same org as the peer can issue an install request
                    int numInstallProposal = 0;

                    Collection<ProposalResponse> responses;
                    final Collection<ProposalResponse> successful = new LinkedList<>();
                    final Collection<ProposalResponse> failed = new LinkedList<>();
                    Collection<Peer> peersFromOrg = channel.getPeers();
                    numInstallProposal = numInstallProposal + peersFromOrg.size();

                    responses = client.sendInstallProposal(installProposalRequest, peersFromOrg);

                    for (ProposalResponse response : responses) {
                        if (response.getStatus() == Status.SUCCESS) {
                            out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }

                    out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

                    if (failed.size() > 0) {
                        ProposalResponse first = failed.iterator().next();
                        fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
                    }

                    // Check that all the proposals are consistent with each other. We should have only one set
                    // where all the proposals above are consistent.
                    Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(responses);
                    if (proposalConsistencySets.size() != 1) {
                        fail(format("Expected only one set of consistent install proposal responses but got %d", proposalConsistencySets.size()));
                    }

                    //////////////////
                    // Upgrade chaincode to ***double*** our move results.

                    if (changeContext) {
                        installProposalRequest.setUserContext(sampleOrg.getPeerAdmin());
                    }

                    UpgradeProposalRequest upgradeProposalRequest = client.newUpgradeProposalRequest();
                    upgradeProposalRequest.setChaincodeID(chaincodeID_11);
                    upgradeProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
                    upgradeProposalRequest.setFcn("init");
                    upgradeProposalRequest.setArgs(new String[] {});    // no arguments don't change the ledger see chaincode.

                    ChaincodeEndorsementPolicy chaincodeEndorsementPolicy;

                    chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
                    chaincodeEndorsementPolicy.fromYamlFile(new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml"));

                    upgradeProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
                    Map<String, byte[]> tmap = new HashMap<>();
                    tmap.put("test", "data".getBytes());
                    upgradeProposalRequest.setTransientMap(tmap);

                    if (changeContext) {
                        upgradeProposalRequest.setUserContext(sampleOrg.getPeerAdmin());
                    }

                    out("Sending upgrade proposal");

                    Collection<ProposalResponse> responses2;

                    responses2 = channel.sendUpgradeProposal(upgradeProposalRequest);

                    successful.clear();
                    failed.clear();
                    for (ProposalResponse response : responses2) {
                        if (response.getStatus() == Status.SUCCESS) {
                            out("Successful upgrade proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }

                    out("Received %d upgrade proposal responses. Successful+verified: %d . Failed: %d", channel.getPeers().size(), successful.size(), failed.size());

                    if (failed.size() > 0) {
                        ProposalResponse first = failed.iterator().next();
                        throw new AssertionError("Not enough endorsers for upgrade :"
                                + successful.size() + ".  " + first.getMessage());
                    }

                    // Check that all the proposals are consistent with each other. We should have only one set
                    // where the proposals above are consistent.
                    proposalConsistencySets = SDKUtils.getProposalConsistencySets(responses2);
                    if (proposalConsistencySets.size() != 1) {
                        fail(format("Expected only one set of consistent upgrade proposal responses but got %d", proposalConsistencySets.size()));
                    }

                    if (changeContext) {
                        return channel.sendTransaction(successful, sampleOrg.getPeerAdmin()).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

                    } else {

                        return channel.sendTransaction(successful).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

                    }

                } catch (CompletionException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }

            }).thenApply(transactionEvent -> {
                try {
                    waitOnFabric(10000);

                    out("Chaincode has been upgraded to version %s", CHAIN_CODE_VERSION_11);

                    //Check to see if peers have new chaincode and old chaincode is gone.

                    client.setUserContext(sampleOrg.getPeerAdmin());
                    for (Peer peer : channel.getPeers()) {

                        if (!checkInstalledChaincode(client, peer, CHAIN_CODE_NAME, CHAIN_CODE_PATH, CHAIN_CODE_VERSION_11)) {
                            throw new AssertionError(format("Peer %s is missing chaincode name:%s, path:%s, version: %s",
                                    peer.getName(), CHAIN_CODE_NAME, CHAIN_CODE_PATH, CHAIN_CODE_PATH));
                        }

                        //should be instantiated too..
                        if (!checkInstantiatedChaincode(channel, peer, CHAIN_CODE_NAME, CHAIN_CODE_PATH, CHAIN_CODE_VERSION_11)) {

                            throw new AssertionError(format("Peer %s is missing instantiated chaincode name:%s, path:%s, version: %s",
                                    peer.getName(), CHAIN_CODE_NAME, CHAIN_CODE_PATH, CHAIN_CODE_PATH));
                        }

                        if (checkInstantiatedChaincode(channel, peer, CHAIN_CODE_NAME, CHAIN_CODE_PATH, CHAIN_CODE_VERSION)) {

                            throw new AssertionError(format("Peer %s still has old instantiated chaincode name:%s, path:%s, version: %s",
                                    peer.getName(), CHAIN_CODE_NAME, CHAIN_CODE_PATH, CHAIN_CODE_PATH));
                        }

                    }

                    client.setUserContext(sampleOrg.getUser(TESTUSER_1_NAME));

//
//                    if( !changeContext ){
//
//                        client.setUserContext(sampleOrg.getUser(TESTUSER_1_NAME));
//                    }

                    ///Check if we still get the same value on the ledger
                    out("delta is %s", delta);
                    queryChaincodeForExpectedValue(client, channel, "" + (325 + delta), chaincodeID);

                    //Now lets run the new chaincode which should *double* the results we asked to move.
                    return moveAmount(client, channel, chaincodeID_11, "50",
                            changeContext ? sampleOrg.getPeerAdmin() : null).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS); // really move 100
                } catch (CompletionException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }

            }).thenApply(transactionEvent -> {

                waitOnFabric(10000);

                queryChaincodeForExpectedValue(client, channel, "" + (425 + delta), chaincodeID_11);

                return null;
            }).exceptionally(e -> {
                if (e instanceof CompletionException && e.getCause() != null) {
                    e = e.getCause();
                }
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {

                        e.printStackTrace(System.err);
                        fail(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
                    }
                }

                e.printStackTrace(System.err);
                fail(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));

                return null;
            }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        out("Running for Channel %s done", channelName);

    }

    CompletableFuture<BlockEvent.TransactionEvent> moveAmount(HFClient client, Channel channel, ChaincodeID chaincodeID, String moveAmount, User user) {

        try {
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            ///////////////
            /// Send transaction proposal to all peers
            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setFcn("invoke");
            transactionProposalRequest.setArgs(new String[] {"move", "a", "b", moveAmount});
            transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
            if (user != null) { // specific user use that
                transactionProposalRequest.setUserContext(user);
            }
            out("sending transaction proposal to all peers with arguments: move(a,b,%s)", moveAmount);

            Collection<ProposalResponse> invokePropResp = channel.sendTransactionProposal(transactionProposalRequest);
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }

            // Check that all the proposals are consistent with each other. We should have only one set
            // where all the proposals above are consistent.
            Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(invokePropResp);
            if (proposalConsistencySets.size() != 1) {
                fail(format("Expected only one set of consistent move proposal responses but got %d", proposalConsistencySets.size()));
            }

            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    invokePropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse firstTransactionProposalResponse = failed.iterator().next();

                throw new ProposalException(format("Not enough endorsers for invoke(move a,b,%s):%d endorser error:%s. Was verified:%b",
                        moveAmount, firstTransactionProposalResponse.getStatus().getStatus(), firstTransactionProposalResponse.getMessage(), firstTransactionProposalResponse.isVerified()));

            }
            out("Successfully received transaction proposal responses.");

            ////////////////////////////
            // Send transaction to orderer
            out("Sending chaincode transaction(move a,b,%s) to orderer.", moveAmount);
            if (user != null) {
                return channel.sendTransaction(successful, user);
            }
            return channel.sendTransaction(successful);
        } catch (Exception e) {

            throw new CompletionException(e);

        }

    }

    private Channel reconstructChannel(String name, HFClient client, SampleOrg sampleOrg) throws Exception {

        client.setUserContext(sampleOrg.getPeerAdmin());
        Channel newChannel = client.newChannel(name);

        for (String orderName : sampleOrg.getOrdererNames()) {
            newChannel.addOrderer(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    testConfig.getOrdererProperties(orderName)));
        }

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);
            Peer peer = client.newPeer(peerName, peerLocation, testConfig.getPeerProperties(peerName));

            //Query the actual peer for which channels it belongs to and check it belongs to this channel
            Set<String> channels = client.queryChannels(peer);
            if (!channels.contains(name)) {
                throw new AssertionError(format("Peer %s does not appear to belong to channel %s", peerName, name));
            }

            newChannel.addPeer(peer);
            sampleOrg.addPeer(peer);
        }

        for (String eventHubName : sampleOrg.getEventHubNames()) {
            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                    testConfig.getEventHubProperties(eventHubName));
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();

        return newChannel;
    }

    private void queryChaincodeForExpectedValue(HFClient client, Channel channel, final String expect, ChaincodeID chaincodeID) {

        out("Now query chaincode on channel %s for the value of b expecting to see: %s", channel.getName(), expect);
        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[] {"query", "b"});
        queryByChaincodeRequest.setFcn("invoke");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = channel.queryByChaincode(queryByChaincodeRequest);
        } catch (Exception e) {
            throw new CompletionException(e);
        }

        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != Status.SUCCESS) {
                fail("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                        ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                assertEquals(payload, expect);
            }
        }
    }

    private void waitOnFabric() {

        waitOnFabric(0);
    }

    ///// NO OP ... leave in case it's needed.
    private void waitOnFabric(int additional) {
        // wait a few seconds for the peers to catch up with each other via the gossip network.
        // Another way would be to wait on all the peers event hubs for the event containing the transaction TxID
//        try {
//            out("Wait %d milliseconds for peers to sync with each other", gossipWaitTime + additional);
//            TimeUnit.MILLISECONDS.sleep(gossipWaitTime + additional);
//        } catch (InterruptedException e) {
//            fail("should not have jumped out of sleep mode. No other threads should be running");
//        }
    }

    private static boolean checkInstalledChaincode(HFClient client, Peer peer, String ccName, String ccPath, String ccVersion) throws InvalidArgumentException, ProposalException {

        out("Checking installed chaincode: %s, at version: %s, on peer: %s", ccName, ccVersion, peer.getName());
        List<ChaincodeInfo> ccinfoList = client.queryInstalledChaincodes(peer);

        boolean found = false;

        for (ChaincodeInfo ccifo : ccinfoList) {

            found = ccName.equals(ccifo.getName()) && ccPath.equals(ccifo.getPath()) && ccVersion.equals(ccifo.getVersion());
            if (found) {
                break;
            }

        }

        return found;
    }

    private static boolean checkInstantiatedChaincode(Channel channel, Peer peer, String ccName, String ccPath, String ccVersion) throws InvalidArgumentException, ProposalException {
        out("Checking instantiated chaincode: %s, at version: %s, on peer: %s", ccName, ccVersion, peer.getName());
        List<ChaincodeInfo> ccinfoList = channel.queryInstantiatedChaincodes(peer);

        boolean found = false;

        for (ChaincodeInfo ccifo : ccinfoList) {
            found = ccName.equals(ccifo.getName()) && ccPath.equals(ccifo.getPath()) && ccVersion.equals(ccifo.getVersion());
            if (found) {
                break;
            }

        }

        return found;
    }

    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

}
