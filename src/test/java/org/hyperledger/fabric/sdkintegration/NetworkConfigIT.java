/*
 *  Copyright 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

package org.hyperledger.fabric.sdkintegration;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.protos.peer.Query.ChaincodeInfo;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse.Status;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TestConfigHelper;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Integration test for the Network Configuration YAML (/JSON) file
 * <p>
 * It has no real dependencies on any of the other integration tests.
 * That is, it can be run with or without having run the other End to End tests.
 * <br>
 * Furthermore, it can be executed multiple times without having to restart the blockchain.
 * <p>
 * The only main requirement is that the network configuration file matches the topology
 * that is set up by End2endIT - because it is typical that End2endIT will be run prior to this test!
 * <p>
 * It first examines the blockchain to see if the "foo" channel has been setup, and if not it constructs it
 * and deploys some chaincode.
 * <br>
 * The method {@link #getChaincodeName()} contains the chaincode name.
 * <br>
 * If the channel already exists, it checks that chaincode with the name defined by the method {@link #getChaincodeName()}
 * exists, and if not it deploys the chaincode with that name.
 * (This is handy if for some reason you need to edit the chaincode and run the tests again - simply change the name
 * {@link #getChaincodeName()} and it will be re-deployed)
 * <p>
 *
 */
public class NetworkConfigIT {

    private static final TestConfig testConfig = TestConfig.getConfig();
    private static final String TEST_ADMIN_NAME = "admin";

    private static final String TEST_FIXTURES_PATH = "src/test/fixture";

    private static final String CHAIN_CODE_PATH = "github.com/example_cc";

    private static final String CHAIN_CODE_VERSION = "1";

    private static final String FOO_CHANNEL_NAME = "foo";

    // Path to the YAML file containing the Network Configuration to be tested
    private static final String NETWORK_CONFIG_FILE = TEST_FIXTURES_PATH + "/sdkintegration/e2e-2Orgs/channel/network-config.yaml";

    private static final TestConfigHelper configHelper = new TestConfigHelper();

    private static Collection<SampleOrg> testSampleOrgs;


    @BeforeClass
    public static void doMainSetup() throws Exception {
        out("\n\n\nRUNNING: NetworkConfigIT.\n");

        configHelper.clearConfig();
        configHelper.customizeConfig();

        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();

        //Set up hfca for each sample org
        for (SampleOrg sampleOrg : testSampleOrgs) {
            sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
        }

        doInitialSetup();
    }

    @After
    public void clearConfig() {
        try {
            configHelper.clearConfig();
        } catch (Exception e) {
        }
    }


    private static void doInitialSetup() throws Exception {

        out("doInitialSetup - enter...");

        ////////////////////////////
        // Setup client
        HFClient client = getTheClient();

        // See if this is a brand new channel
        boolean isNewChannel = !checkAnyInstalledChaincode(client);
        out("isNewChannel = " + isNewChannel);

        Channel fooChannel = null;

        if (isNewChannel) {
            // Construct the channel
            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
            fooChannel = constructChannel(FOO_CHANNEL_NAME, client, sampleOrg);
        }

        final String chaincodeName = getChaincodeName();

        if (!checkInstalledChaincode(client, chaincodeName, CHAIN_CODE_PATH, CHAIN_CODE_VERSION)) {

            // The chaincode we require does not exist, so deploy it...

            if (fooChannel == null) {
                // Reconstruct the channel from the config file
                fooChannel = reconstructChannel(FOO_CHANNEL_NAME, client);
            }

            out("doInitialSetup - deploying new chaincode...");
            deployChaincode(client, fooChannel, chaincodeName);
        }

        out("doInitialSetup - exit...");
    }


    // Returns a new client instance
    private static HFClient getTheClient() throws Exception {

        // Initialize the client from the network config
        File configFile = new File(NETWORK_CONFIG_FILE);
        HFClient client = HFClient.loadFromConfig(configFile);

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        // TODO???
        // client.setMemberServices(peerOrg1FabricCA);

        setupAllSampleOrgs(client);

        // Use admin for all requests
        SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        client.setUserContext(sampleOrg.getPeerAdmin());

        return client;
    }

    // Returns the pre-configured channel
    private static Channel getTheChannel(HFClient client) throws Exception {
        return reconstructChannel(FOO_CHANNEL_NAME, client);
    }

    private static void setupAllSampleOrgs(HFClient client) throws Exception {

        ////////////////////////////
        //Set up USERS

        //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
        //   MUST be replaced with more robust application implementation  (Database, LDAP)
        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }

        final SampleStore sampleStore = new SampleStore(sampleStoreFile);
        //  sampleStoreFile.deleteOnExit();

        //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface

        ////////////////////////////
        // get users for all orgs
        out("SampleOrg setup...");

        for (SampleOrg sampleOrg : testSampleOrgs) {

            HFCAClient ca = sampleOrg.getCAClient();
            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
            if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                admin.setMspId(mspid);
            }

            sampleOrg.setAdmin(admin); // The admin of this org --

            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();

            // src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/

            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                    findFileSk(Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/",
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());

            sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode
            setupSampleOrg(client, sampleOrg);
        }

        out("SampleOrg setup complete...");
    }

    // Sets up a single sample organization
    private static void setupSampleOrg(HFClient client, SampleOrg sampleOrg) throws Exception {

        out("Setting up sample org...");

        //Only peer Admin org
        client.setUserContext(sampleOrg.getPeerAdmin());

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            out("setupSampleOrg: peerName=" + peerName + " peerLocation=" + peerLocation + " peerProps=" + peerProperties.toString());
            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            sampleOrg.addPeer(peer);
        }

    }

    /**
     * Defines the name of the chaincode to be deployed.
     * @return
     */
    private static String getChaincodeName() {
        return "testChaincode-001";
    }

    @Test
    public void testUpdate1() throws Exception {

        // Setup client and channel instances
        HFClient client = getTheClient();
        Channel channel = getTheChannel(client);

        final String chaincodeName = getChaincodeName();

        final ChaincodeID chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();


        final String channelName = channel.getName();

        out("Running testUpdate1 - Channel %s", channelName);
        channel.setTransactionWaitTime(testConfig.getTransactionWaitTime());
        channel.setDeployWaitTime(testConfig.getDeployWaitTime());

        int moveAmount = 5;
        String originalVal = queryChaincodeForCurrentValue(client, channel, chaincodeID);
        String newVal = "" + (Integer.parseInt(originalVal) + moveAmount);

        out("Original value = %s", originalVal);

        // Move some assets
        moveAmount(client, channel, chaincodeID, "a", "b", "" + moveAmount, null).thenApply(transactionEvent -> {
            // Check that they were moved
            queryChaincodeForExpectedValue(client, channel, newVal, chaincodeID);
            return null;

        }).thenApply(transactionEvent -> {
            // Move them back
            try {
                return moveAmount(client, channel, chaincodeID, "b", "a", "" + moveAmount, null).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }).thenApply(transactionEvent -> {
            // Check that they were moved back
            queryChaincodeForExpectedValue(client, channel, originalVal, chaincodeID);
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

        channel.shutdown(true); // Force channel to shutdown clean up resources.

        out("testUpdate1 - done");
    }


    private void queryChaincodeForExpectedValue(HFClient client, Channel channel, final String expect, ChaincodeID chaincodeID) {

        out("Now query chaincode on channel %s for the value of b expecting to see: %s", channel.getName(), expect);

        String value = queryChaincodeForCurrentValue(client, channel, chaincodeID);
        assertEquals(expect, value);
    }

    // Returns the current value of b's assets
    private String queryChaincodeForCurrentValue(HFClient client, Channel channel, ChaincodeID chaincodeID) {

        out("Now query chaincode on channel %s for the current value of b", channel.getName());

        User u = client.getUserContext();
        System.out.println("User context = " + u.getName());

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

        String expect = null;
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != Status.SUCCESS) {
                fail("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                        ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                if (expect != null) {
                    assertEquals(expect, payload);
                } else {
                    expect = payload;
                }
            }
        }
        return expect;
    }

    private CompletableFuture<BlockEvent.TransactionEvent> moveAmount(HFClient client, Channel channel, ChaincodeID chaincodeID, String from, String to, String moveAmount, User user) throws Exception {

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        ///////////////
        /// Send transaction proposal to all peers
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn("invoke");
        transactionProposalRequest.setArgs(new String[] {"move", from, to, moveAmount});
        transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
        if (user != null) { // specific user use that
            transactionProposalRequest.setUserContext(user);
        }
        out("sending transaction proposal to all peers with arguments: move(%s,%s,%s)", from, to, moveAmount);

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

            throw new ProposalException(format("Not enough endorsers for invoke(move %s,%s,%s):%d endorser error:%s. Was verified:%b",
                    from, to, moveAmount, firstTransactionProposalResponse.getStatus().getStatus(), firstTransactionProposalResponse.getMessage(), firstTransactionProposalResponse.isVerified()));
        }
        out("Successfully received transaction proposal responses.");

        ////////////////////////////
        // Send transaction to orderer
        out("Sending chaincode transaction(move %s,%s,%s) to orderer.", from, to, moveAmount);
        if (user != null) {
            return channel.sendTransaction(successful, user);
        }

        return channel.sendTransaction(successful);
    }


    private static ChaincodeID deployChaincode(HFClient client, Channel channel, String chaincodeName) {

        out("deployChaincode - enter");
        ChaincodeID chaincodeID = null;

        try {

            final String channelName = channel.getName();
            out("deployChaincode - channelName = " + channelName);

            channel.setTransactionWaitTime(testConfig.getTransactionWaitTime());
            channel.setDeployWaitTime(testConfig.getDeployWaitTime());

            Collection<Orderer> orderers = channel.getOrderers();
            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();

            ////////////////////////////
            // Install Proposal Request
            //
            out("Creating install proposal");

            InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);

            ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
            installProposalRequest.setChaincodeSourceLocation(new File(TEST_FIXTURES_PATH + "/sdkintegration/gocc/sample1"));

            installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);

            out("Sending install proposal");

            ////////////////////////////
            // only a client from the same org as the peer can issue an install request
            int numInstallProposal = 0;

            Collection<Peer> peersFromOrg = channel.getPeers();
            numInstallProposal = numInstallProposal + peersFromOrg.size();
            responses = client.sendInstallProposal(installProposalRequest, peersFromOrg);

            for (ProposalResponse response : responses) {
                if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }

            SDKUtils.getProposalConsistencySets(responses);
            //   }
            out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            }

            ///////////////
            //// Instantiate chaincode.
            //
            // From the docs:
            // The instantiate transaction invokes the lifecycle System Chaincode (LSCC) to create and initialize a chaincode on a channel
            // After being successfully instantiated, the chaincode enters the active state on the channel and is ready to process any transaction proposals of type ENDORSER_TRANSACTION

            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            instantiateProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[] {"a", "500", "b", "999"});

            Map<String, byte[]> tm = new HashMap<>();
            tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
            tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
            instantiateProposalRequest.setTransientMap(tm);

            /*
              policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
              See README.md Chaincode endorsement policies section for more details.
            */
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
            chaincodeEndorsementPolicy.fromYamlFile(new File(TEST_FIXTURES_PATH + "/sdkintegration/chaincodeendorsementpolicy.yaml"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            out("Sending instantiateProposalRequest to all peers...");
            successful.clear();
            failed.clear();

            responses = channel.sendInstantiationProposal(instantiateProposalRequest);

            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                    out("Succesful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                } else {
                    failed.add(response);
                }
            }
            out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                fail("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
            }

            ///////////////
            /// Send instantiate transaction to orderer
            out("Sending instantiateTransaction to orderer...");
            CompletableFuture<TransactionEvent> future = channel.sendTransaction(successful, orderers);

            out("calling get...");
            TransactionEvent event = future.get(30, TimeUnit.SECONDS);
            out("get done...");

            assertTrue(event.isValid()); // must be valid to be here.
            out("Finished instantiate transaction with transaction id %s", event.getTransactionID());


        } catch (Exception e) {
            e.printStackTrace();
            out("Caught an exception running channel %s", channel.getName());
            fail("Test failed with error : " + e.getMessage());
        }

        return chaincodeID;
    }


    private static Channel constructChannel(String name, HFClient client, SampleOrg sampleOrg) throws Exception {
        ////////////////////////////
        //Construct the channel
        //

        out("Constructing channel %s", name);

        //Only peer Admin org
        client.setUserContext(sampleOrg.getPeerAdmin());


        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : sampleOrg.getOrdererNames()) {

            System.out.println("Orderer = " + orderName);

            Properties ordererProperties = testConfig.getOrdererProperties(orderName);

            //example of setting keepAlive to avoid timeouts on inactive http2 connections.
            // Under 5 minutes would require changes to server side to accept faster ping rates.
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    ordererProperties));
        }

        //Just pick the first orderer in the list to create the channel.

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(TEST_FIXTURES_PATH + "/sdkintegration/e2e-2Orgs/channel/" + name + ".tx"));

        //Create channel that has only one signer that is this orgs peer admin. If channel creation policy needed more signature they would need to be added too.
        Channel newChannel = client.newChannel(name, anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, sampleOrg.getPeerAdmin()));

        out("Created channel %s", name);

        for (Peer peer : sampleOrg.getPeers()) {
            newChannel.joinPeer(peer);
            out("Peer %s joined channel %s", peer.getName(), name);
        }

        for (Orderer orderer : orderers) { //add remaining orderers if any.
            newChannel.addOrderer(orderer);
        }

        for (String eventHubName : sampleOrg.getEventHubNames()) {

            final Properties eventHubProperties = testConfig.getEventHubProperties(eventHubName);

            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
            eventHubProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});

            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                    eventHubProperties);
            newChannel.addEventHub(eventHub);
        }

        newChannel.initialize();

        out("Finished initialization channel %s", name);

        return newChannel;

    }


    private static Channel reconstructChannel(String name, HFClient client) throws Exception {

        Channel newChannel = client.getChannel(name);
        if (newChannel == null) {
            throw new RuntimeException("Channel " + name + " is not defined in the config file!");
        }

        // TODO: Should getChannel also initialize the channel???
        newChannel.initialize();

        return newChannel;
    }


    // Determines if any chaincode has been installed on the channel
    private static boolean checkAnyInstalledChaincode(HFClient client) throws InvalidArgumentException, ProposalException {
        //Peer peer = sampleOrg.getPeers().iterator().next();
        // Find a suitable peer in the configured client organization
        // TODO: If we use a peer from Org2 - we get the error: UNKNOWN: Failed to deserialize creator identity, err Expected MSP ID Org2MSP, received Org1MSP
        // I need to understand exactly what this means..
        // I think it means that the peer was created using a different user context to what we are using now...
        Peer peer = client.getPeerFromConfig(null, NetworkConfig.PeerRole.LEDGER_QUERY);
        List<ChaincodeInfo> ccinfoList = client.queryInstalledChaincodes(peer);
        return ccinfoList.size() > 0;
    }

    // Determines if chaincode with the given name is installed on the channel
    private static boolean checkInstalledChaincode(HFClient client, String ccName, String ccPath, String ccVersion) throws InvalidArgumentException, ProposalException {

        // Find a suitable peer to query
        Peer peer = client.getPeerFromConfig(null, null);

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

    private static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }


    private static File findFileSk(File directory) {

        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];

    }



}
