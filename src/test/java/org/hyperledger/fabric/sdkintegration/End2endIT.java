/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.ChainCodeID;
import org.hyperledger.fabric.sdk.ChainConfiguration;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.InvokeProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryProposalRequest;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric.sdk.TestConfigHelper;

import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.events.EventHub;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import org.hyperledger.fabric_ca.sdk.HFCAClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static java.lang.String.format;

/**
 * Test end to end scenario
 */
public class End2endIT {

    static final TestConfig testConfig = TestConfig.getConfig();


    static final String CHAIN_CODE_NAME = "example_cc.go";
    static final String CHAIN_CODE_PATH = "github.com/example_cc";
    static final String CHAIN_CODE_VERSION = "1.0";


  ///  static final String TEST_CHAIN_NAME = "testchainid";
    static final String FOO_CHAIN_NAME = "foo";
    static final String BAR_CHAIN_NAME = "bar";
    static final String MYCHANNEL_CHAIN_NAME = "mychannel";

    static final Hashtable<String, String> MSPIDs = new Hashtable<>();
    static final Hashtable<String, SampleUser> admins = new Hashtable<>();
    static final Hashtable<String, Set<String>> peers = new Hashtable<>();
    static final Hashtable<String, Set<Peer>> orgPeers = new Hashtable<>();
    static final Hashtable<String, SampleUser> users = new Hashtable<>();
    static final Hashtable<String, HFCAClient> fabricCAs = new Hashtable<>();

    String testTxID = null ;  // save the CC invoke TxID and use in queries

    final static Collection<String> PEER_LOCATIONS = Arrays.asList(testConfig.getIntegrationTestsPeers().split(","));

    final static Collection<String> MSPS = Arrays.asList(testConfig.getIntegrationTestsMSPIDs().split(","));

    final static Collection<String> ORDERER_LOCATIONS = Arrays.asList(testConfig.getIntegrationTestsOrderers().split(","));

    final static Collection<String> EVENTHUB_LOCATIONS = Arrays.asList(testConfig.getIntegrationtestsEventhubs().split(","));

    final static String[] FABRIC_CA_SERVICES_LOCATION = testConfig.getIntegrationtestsFabricCA().split(",");

    private final TestConfigHelper configHelper = new TestConfigHelper();

    @Before
    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MalformedURLException {
        configHelper.clearConfig();
        configHelper.customizeConfig();
        for (String peerLocation : PEER_LOCATIONS) {
            String[] temp = peerLocation.trim().split(" ");
            if (! peers.containsKey(temp[0])) {
                HashSet<String> aSet = new HashSet<>();
                aSet.add(temp[1]);
                peers.put(temp[0], aSet);
            }
            else {
                peers.get(temp[0]).add(temp[1]);
            }
        }
        for (String ca : FABRIC_CA_SERVICES_LOCATION) {
            String[] temp = ca.trim().split(" ");
            fabricCAs.put(temp[0], new HFCAClient(temp[1], null));
        }
        for (String msp : MSPS) {
            String[] temp = msp.trim().split(" ");
            MSPIDs.put(temp[0], temp[1]);
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
            if (sampleStoreFile.exists()) { //For testing start fresh
                sampleStoreFile.delete();
            }

            final SampleStore sampleStore = new SampleStore(sampleStoreFile);
            sampleStoreFile.deleteOnExit();

            //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface

            ////////////////////////////
            // get users for all orgs
            Set<String> orgs = MSPIDs.keySet();
            SampleUser admin;
            SampleUser user;
            for (String org : orgs) {
                HFCAClient ca = fabricCAs.get(org);
                client.setMemberServices(ca);
                admin = sampleStore.getMember("admin", org);
                if(!admin.isEnrolled()){  //Preregistered admin only needs to be enrolled with Fabric CA.

                    admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                    admin.setMPSID(MSPIDs.get(org));
                }
                admins.put(org, admin);

                user = sampleStore.getMember("user1", org);
                if(!user.isRegistered()){  // users need to be registered AND enrolled
                    RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                    user.setEnrollmentSecret(ca.register(rr, admin));
                }
                if (!user.isEnrolled()){
                    user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
                    user.setMPSID(MSPIDs.get(org));
                }
                users.put(org, user);
            }

            // client.setUserContext(users.get("peerOrg1"));

            ////////////////////////////
            //Construct and run the chains
            /*
            runChain(client, constructChain(FOO_CHAIN_NAME, client), true, "orgPeer1", 0);
            out("\n");
            runChain(client, constructChain(BAR_CHAIN_NAME, client), false, "orgPeer2", 100);//run a newly constructed foo chain with different b value!
            */
            runChain(client, constructChain(MYCHANNEL_CHAIN_NAME, client, "peerOrg2"), true, "peerOrg1", 0);
            out("That's all folks!");


        } catch (Exception e) {
            e.printStackTrace();

            fail(e.getMessage());
        }


    }


    void runChain(HFClient client, Chain chain, boolean installChainCode, String orgToUse, int delta) {


        try {

            final String chainName = chain.getName();
            out("Running Chain %s", chainName);
            chain.setInvokeWaitTime(testConfig.getInvokeWaitTime());
            chain.setDeployWaitTime(testConfig.getDeployWaitTime());

            Collection<Peer> channelPeers = chain.getPeers();
            Collection<Orderer> orderers = chain.getOrderers();
            final ChainCodeID chainCodeID;
            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful  = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();


            chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();


            if (installChainCode) {
                ////////////////////////////
                // Install Proposal Request
                //
                InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
                installProposalRequest.setChaincodeID(chainCodeID);
                ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
                installProposalRequest.setChaincodeSourceLocation(new File("src/test/fixture"));

                out("Sending install proposal");

                ////////////////////////////
                // only a client from the same org as the peer can issue an install request
                int numInstallProposal = 0;
                Set<String> orgs = orgPeers.keySet();
                for (String org : orgs) {
                    client.setUserContext(admins.get(org));
                    Set<Peer> peersFromOrg = orgPeers.get(org);
                    numInstallProposal = numInstallProposal + peersFromOrg.size();
                    responses = chain.sendInstallProposal(installProposalRequest, peersFromOrg);

                    for (ProposalResponse response : responses) {
                        if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                            out("Successful install proposal response Txid: %s", response.getTransactionID());
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }
                }
                out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

                if (failed.size() > 0) {
                    ProposalResponse first = failed.iterator().next();
                    fail("Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
                }
            }
            //  final ChainCodeID chainCodeID = firstInstallProposalResponse.getChainCodeID();
            // Note install chain code does not require transaction no need to
            // send to Orderers

            ///////////////
            //// Instantiate chain code.

            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();

            instantiateProposalRequest.setChaincodeID(chainCodeID);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[]{"a", "100", "b", ""+(200 + delta)});

            /*
              policyBitsAdmin - which has policy AND(DEFAULT.admin) meaning 1 signature from the DEFAULT MSP admin' is required
              See README.md Chaincode endorsement policies section for more details.
            */
            ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy(new File("src/test/resources/policyBitsAdmin"));
            instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

            out("Sending instantiateProposalRequest to all peers with arguments: a and b set to 100 and %s respectively", ""+(200 + delta) );
            successful.clear();
            failed.clear();

            responses = chain.sendInstantiationProposal(instantiateProposalRequest, chain.getPeers());
            for (ProposalResponse response : responses) {
                if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                    successful.add(response);
                    out("Succesful instantiate proposal response Txid: %s", response.getTransactionID());
                } else {
                    failed.add(response);
                }
            }
            out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                ProposalResponse first = failed.iterator().next();
                fail("Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
            }

            successful.clear();
            failed.clear();

            ///////////////
            /// Send instantiate transaction to orderer
            out("Sending instantiateTransaction to orderer with a and b set to 100 and %s respectively", ""+(200 + delta));
            chain.sendTransaction(successful, orderers).thenApply(transactionEvent -> {

                assertTrue(transactionEvent.isValid()); // must be valid to be here.
                out("Finished instantiate transaction with transaction id %s", transactionEvent.getTransactionID());

                try {
                    out("Successfully completed chaincode instantiation.");
                    out("Creating invoke proposal");

                    successful.clear();
                    failed.clear();
                    client.setUserContext(admins.get(orgToUse)); // select the user for all subsequent requests

                    ///////////////
                    /// Send invoke proposal to all peers
                    InvokeProposalRequest invokeProposalRequest = client.newInvokeProposalRequest();
                    invokeProposalRequest.setChaincodeID(chainCodeID);
                    invokeProposalRequest.setFcn("invoke");
                    invokeProposalRequest.setArgs(new String[]{"move", "a", "b", "100"});
                    out("sending invokeProposal to all peers with arguments: move(a,b,100)");

                    Collection<ProposalResponse> invokePropResp = chain.sendInvokeProposal(invokeProposalRequest, chain.getPeers());
                    for (ProposalResponse response : invokePropResp) {
                        if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                            out("Successful invoke proposal response Txid: %s", response.getTransactionID());
                            successful.add(response);
                        } else {
                            failed.add(response);
                        }
                    }
                    out("Received %d invoke proposal responses. Successful+verified: %d . Failed: %d",
                            invokePropResp.size(), successful.size(), failed.size());
                    if (failed.size() > 0) {
                        ProposalResponse firstInvokeProposalResponse = failed.iterator().next();
                        fail("Not enough endorsers for invoke(move a,b,100):" + failed.size() + " endorser error: " +
                                firstInvokeProposalResponse.getMessage() +
                                ". Was verified: " + firstInvokeProposalResponse.isVerified());
                    }
                    out("Successfully received invoke proposal responses.");

                    ////////////////////////////
                    // Invoke Transaction
                    //

                    out("Sending chain code transaction(move a,b,100) to orderer.");
                    return chain.sendTransaction(successful, orderers).get(120, TimeUnit.SECONDS);


                } catch (Exception e) {
                    out("Caught an exception while invoking chaincode");
                    e.printStackTrace();
                    fail("Failed invoking chaincode with error : " + e.getMessage());
                }

                return null;

            }).thenApply(transactionEvent -> {
                try {

                    assertTrue(transactionEvent.isValid()); // must be valid to be here.
                    out("Finished invoke transaction with transaction id %s", transactionEvent.getTransactionID());
                    testTxID = transactionEvent.getTransactionID();


                    ////////////////////////////
                    // Query Proposal
                    //


                    out("Now query chain code for the value of b.");


                    // InvokeProposalRequest qr = InvokeProposalRequest.newInstance();
                    QueryProposalRequest queryProposalRequest = client.newQueryProposalRequest();

                    queryProposalRequest.setArgs(new String[]{"query", "b"});
                    queryProposalRequest.setFcn("invoke");
                    queryProposalRequest.setChaincodeID(chainCodeID);


                    Collection<ProposalResponse> queryProposals = chain.sendQueryProposal(queryProposalRequest, chain.getPeers());

                    for (ProposalResponse proposalResponse : queryProposals) {
                        if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                            return new Exception("Failed invoke proposal.  status: " + proposalResponse.getStatus() +
                                    ". messages: " + proposalResponse.getMessage()
                                    + ". Was verified : " + proposalResponse.isVerified());

                        }

                    }

                    out("Successfully received query response.");

                    String payload = queryProposals.iterator().next().getProposalResponse().getResponse().getPayload().toStringUtf8();

                    out("Query payload of b returned %s", payload);

                    String expect = "" +(300 + delta);
                    assertEquals(payload, expect);

                    return null;

                } catch (Exception e) {
                    out("Caught exception while running query");
                    e.printStackTrace();
                    fail("Failed during chaincode query with error : " + e.getMessage());
                }

                return null;

            }).exceptionally(e -> {
                System.err.println("Bad status value for proposals transaction: " + e.getMessage());
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        fail(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
                    }
                }
                fail(format("Transaction  failed  %s", e.getMessage()));

                return null;
            }).get(120, TimeUnit.SECONDS);


            // Channel queries

            BlockchainInfo channelInfo = chain.queryBlockchainInfo();
            out("Channel info for : " + chainName);
            out("Channel height: " + channelInfo.getHeight());
            String chainCurrentHash = Hex.encodeHexString(channelInfo.getCurrentBlockHash());
            String chainPreviousHash = Hex.encodeHexString(channelInfo.getPreviousBlockHash());
            out("Channel current block hash: " + chainCurrentHash);
            out("Channel previous block hash: " + chainPreviousHash);

            // Query by block number. Should return latest block, i.e. block number 2
            BlockInfo returnedBlock = chain.queryBlockByNumber(channelInfo.getHeight()-1);
            String previousHash = Hex.encodeHexString(returnedBlock.getPreviousHash());
            out("queryBlockByNumber returned correct block with blockNumber " + returnedBlock.getBlockNumber()
                            + " \n previous_hash " + previousHash);
            assertEquals(channelInfo.getHeight()-1, returnedBlock.getBlockNumber());
            assertEquals(chainPreviousHash, previousHash);

            // Query by block hash. Using latest block's previous hash so should return block number 1
            byte[] hashQuery = returnedBlock.getPreviousHash();
            returnedBlock = chain.queryBlockByHash(hashQuery);
            out("queryBlockByHash returned block with blockNumber " + returnedBlock.getBlockNumber());
            assertEquals(channelInfo.getHeight()-2, returnedBlock.getBlockNumber());

            // Query block by TxID. Since it's the last TxID, should be block 2
            returnedBlock = chain.queryBlockByTransactionID(testTxID);
            out("queryBlockByTxID returned block with blockNumber " + returnedBlock.getBlockNumber());
            assertEquals(channelInfo.getHeight()-1, returnedBlock.getBlockNumber());

            // query transaction by ID
            TransactionInfo txInfo = chain.queryTransactionByID(testTxID);
            out("QueryTransactionByID returned TransactionInfo: txID " + txInfo.getTransactionID()
                                + "\n     validation code " + txInfo.getValidationCode().getNumber());
            /*
             * TODO printing out too many error messages right now
            boolean shouldNotFail = true;
            try {
                txInfo = chain.queryTransactionByID("fake", peer);
            } catch (Exception ee) {
                shouldNotFail = false;
            }
            if (shouldNotFail) {
                fail("Should have failed on queryTransactionByID using fake txID");
            }
            */

            out("Running for Chain %s done", chainName);


        } catch (Exception e) {
            out("Caught an exception running chain %s", chain.getName());
            e.printStackTrace();
            fail("Test failed with error : " + e.getMessage());

        }
    }



    private static Chain constructChain(String  name, HFClient client, String org) throws Exception {
        //////////////////////////// TODo Needs to be made out of bounds and here chain just retrieved
        //Construct the chain
        //


        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderloc : ORDERER_LOCATIONS) {
            orderers.add(client.newOrderer(orderloc));

        }

        //Just pick the first order in the list to create the chain.

        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        //ChainConfiguration chainConfiguration = new ChainConfiguration(new File("src/test/fixture/" + name+ ".configtx"));
        ChainConfiguration chainConfiguration = new ChainConfiguration(new File("src/test/fixture/e2e-2Orgs/channel/" + name + ".tx"));

        client.setUserContext(admins.get(org));
        Chain newChain = client.newChain(name, anOrderer, chainConfiguration);

        int i = 0;
        Set<String> orgs = peers.keySet() ;
        for (String anOrg : orgs) {
            Set<String> peersUrl = peers.get(anOrg);
            client.setUserContext(admins.get(anOrg));
            for (String peerloc : peersUrl) {
                Peer peer = client.newPeer(peerloc);
                peer.setName("peer_" + i);
                newChain.joinPeer(peer);
                if (! orgPeers.containsKey(anOrg)) {
                    Set<Peer> aSet = new HashSet<>();
                    aSet.add(peer);
                    orgPeers.put(anOrg, aSet);
                }
                else {
                    orgPeers.get(anOrg).add(peer);
                }
            }
        }

        for (String orderloc : ORDERER_LOCATIONS) {
            Orderer orderer = client.newOrderer(orderloc);
            newChain.addOrderer(orderer);
        }

        for (String eventHubLoc : EVENTHUB_LOCATIONS) {
            EventHub eventHub = client.newEventHub(eventHubLoc);
            newChain.addEventHub(eventHub);
        }


        newChain.initialize();
        return newChain;

    }


    /**
     * Sample how to reconstruct chain
     * @param name
     * @param client
     * @return
     * @throws Exception
     */
    private static Chain reconstructChain(String  name, HFClient client) throws Exception {

        //Construct the chain
        //


        Chain newChain = client.newChain(name);

        int i = 0;
        for (String peerloc : PEER_LOCATIONS) {
            Peer peer = client.newPeer(peerloc);
            peer.setName("peer_" + i);
            newChain.addPeer(peer);

        }

        for (String orderloc : ORDERER_LOCATIONS) {
            Orderer orderer = client.newOrderer(orderloc);
            newChain.addOrderer(orderer);
        }

        for (String eventHubLoc : EVENTHUB_LOCATIONS) {
            EventHub eventHub = client.newEventHub(eventHubLoc);
            newChain.addEventHub(eventHub);
        }

        newChain.initialize();
        return newChain;

    }


    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

}
