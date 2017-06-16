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
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChannelConfiguration;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TestConfigHelper;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test end to end scenario
 */
public class JavaSDKStressorMT {

    private static final TestConfig testConfig = TestConfig.getConfig();
    private static final String TEST_ADMIN_NAME = "admin";
    private static final String TESTUSER_1_NAME = "user1";
    private static final String TEST_FIXTURES_PATH = "src/test/fixture";

    private static final String CHAIN_CODE_NAME = "example_cc_go";
    private static final String CHAIN_CODE_PATH = "github.com/example_cc";
    private static final String CHAIN_CODE_VERSION = "1";

    private static final String FOO_CHANNEL_NAME = "foo";
    private static final String BAR_CHANNEL_NAME = "bar";

    String testTxID = null;  // save the CC invoke TxID and use in queries

    private final TestConfigHelper configHelper = new TestConfigHelper();

    private Collection<SampleOrg> testSampleOrgs;

    @Before
    public void checkConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, MalformedURLException {
        out("\n\n\nRUNNING: End2endIT.\n");
        configHelper.clearConfig();
        configHelper.customizeConfig();

        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
        //Set up hfca for each sample org

        for (SampleOrg sampleOrg : testSampleOrgs) {
            sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
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
            //  sampleStoreFile.deleteOnExit();

            //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface

            ////////////////////////////
            // get users for all orgs

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

                SampleUser user = sampleStore.getMember(TESTUSER_1_NAME, sampleOrg.getName());
                if (!user.isRegistered()) {  // users need to be registered AND enrolled
                    RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                    user.setEnrollmentSecret(ca.register(rr, admin));
                }
                if (!user.isEnrolled()) {
                    user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
                    user.setMspId(mspid);
                }
                sampleOrg.addUser(user); //Remember user belongs to this Org

                final String sampleOrgName = sampleOrg.getName();
                final String sampleOrgDomainName = sampleOrg.getDomainName();

                // src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/

                SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                        findFileSk(Paths.get(testConfig.getTestChannlePath(), "crypto-config/peerOrganizations/",
                                sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                        Paths.get(testConfig.getTestChannlePath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                                format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());

                sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode

            }

            ////////////////////////////
            //Construct and run the channels
            SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
            SampleOrg sampleOrg2 = testConfig.getIntegrationTestsSampleOrg("peerOrg2");
            Channel fooChannel = constructChannel(FOO_CHANNEL_NAME, client, sampleOrg);

            List<Thread> threads = new LinkedList<>();

            for (int i = 0; i < 25; ++i) {
                RunChannel runChaincode = new RunChannel(client, fooChannel, sampleOrg, "chaincode-" + i);
                final Thread thread = new Thread(runChaincode);
                thread.setName("ChaincodeRunner-" + i);

                threads.add(thread);
                thread.start();
                Thread.sleep(1000 * 20);
            }

            for (Thread thread : threads) {
                thread.join();
            }
            //   thread.setDaemon(true);

            //   runChannel(client, fooChannel, sampleOrg);
            //    fooChannel.shutdown(true); // Force foo channel to shutdown clean up resources.
            out("\n");

//            sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg2");
//            Channel barChannel = constructChannel(BAR_CHANNEL_NAME, client, sampleOrg);
//            runChannel(client, barChannel, true, sampleOrg, 100); //run a newly constructed bar channel with different b value!
//            //let bar channel just shutdown so we have both scenarios.
//
//            out("\nTraverse the blocks for chain %s ", barChannel.getName());
//            blockWalker(barChannel);
//            out("That's all folks!");

        } catch (Exception e) {
            e.printStackTrace();

            fail(e.getMessage());
        }

    }

    static final Random RAND = new Random();

    static int getRandom(int bound) {

        return RAND.nextInt(bound) + 1;
    }

    //CHECKSTYLE.OFF: Method length is 320 lines (max allowed is 150).
    void runChannel(HFClient client, Channel channel, SampleOrg sampleOrg) {

        //    final Runtime runtime = Runtime.getRuntime();

        try {

            for (int i = 1; i < 2; ++i) {

                //        System.gc();

                //   out("On run %d, Runtime memory is : %d", i, runtime.totalMemory() - runtime.freeMemory());
                final String channelName = channel.getName();

                out("Running channel %s", channelName);
                channel.setTransactionWaitTime(testConfig.getTransactionWaitTime());
                channel.setDeployWaitTime(testConfig.getDeployWaitTime());
                String chaincodeName = CHAIN_CODE_NAME + i;

                installInstantiateChaincode(client, channel, chaincodeName, sampleOrg.getPeerAdmin()).thenApply(dontCare -> {

                    try {

                        for (int j = 1; j < 12000000; ++j) {

                            Thread.sleep(getRandom(1000));

                            //                System.gc();

                            out("running move in channel code %s for %d times", chaincodeName, j);

                            //   out("On invoke run %d, Runtime memory is : %d", j, runtime.totalMemory() - runtime.freeMemory());
                            //return moveAmountAndExpect(client, sampleOrg.getPeerAdmin(), channel, chaincodeName, "1", "201").get();
                            moveAmountAndExpect(client, sampleOrg.getPeerAdmin(), channel, chaincodeName, "1", "" + (200 + j)).thenApply(x -> {

                                try {
                                    blockWalker(channel);
                                } catch (Exception e) {

                                    throw new CompletionException(e);

                                }

                                return null;

                            }).get();
                        }

                        return null;

                    } catch (Exception e) {
                        out("Caught an exception while invoking chaincode");
                        e.printStackTrace();
                        fail("Failed invoking chaincode with error : " + e.getMessage());
                    }

                    return null;

                }).thenApply(dontCare -> {

                    try {
                        blockWalker(channel);
                    } catch (Exception e) {

                        throw new CompletionException(e);

                    }

                    return null;
                }).exceptionally(e -> {
                    if (e instanceof TransactionEventException) {
                        BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                        if (te != null) {
                            fail(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
                        }
                    }
                    fail(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));

                    return null;
                }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

                out("Running for Channel %s done", channelName);
            }

        } catch (Exception e) {
            out("Caught an exception running channel %s", channel.getName());
            e.printStackTrace();
            fail("Test failed with error : " + e.getMessage());
        }
    }
    //CHECKSTYLE.ON: Method length is 320 lines (max allowed is 150).

    private Channel constructChannel(String name, HFClient client, SampleOrg sampleOrg) throws Exception {
        ////////////////////////////
        //Construct the channel
        //

        out("Constructing channel %s", name);

        //Only peer Admin org
        client.setUserContext(sampleOrg.getPeerAdmin());

        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : sampleOrg.getOrdererNames()) {

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

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            Properties peerProperties = testConfig.getPeerProperties(peerName); //test properties for peer.. if any.
            if (peerProperties == null) {
                peerProperties = new Properties();
            }
            //Example of setting specific options on grpc's NettyChannelBuilder
            peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            newChannel.joinPeer(peer);
            out("Peer %s joined channel %s", peerName, name);
            sampleOrg.addPeer(peer);
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

    static class RunChannel implements Runnable {
        final HFClient client;
        final Channel channel;
        final SampleOrg sampleOrg;
        final String chaincodeName;

        RunChannel(HFClient client, Channel channel, SampleOrg sampleOrg, String chaincodeName) {
            this.client = client;
            this.channel = channel;
            this.sampleOrg = sampleOrg;
            this.chaincodeName = chaincodeName;
        }

        @Override
        public void run() {

            //  final Runtime runtime = Runtime.getRuntime();

            try {

                //        System.gc();

                //  out("On run %d, Runtime memory is : %d", chaincodeName, runtime.totalMemory() - runtime.freeMemory());
                final String channelName = channel.getName();

                out("Running channel %s", channelName);
                channel.setTransactionWaitTime(testConfig.getTransactionWaitTime());
                channel.setDeployWaitTime(testConfig.getDeployWaitTime());

                installInstantiateChaincode(client, channel, chaincodeName, sampleOrg.getPeerAdmin()).thenApply(dontCare -> {

                    try {
                        out("running chaincode %s: ", chaincodeName);

                        for (int j = 1; j < 1200000; ++j) {

                            Thread.sleep(getRandom(1000));

                            //                System.gc();

                            out("running move in chaincode %s for %d times", chaincodeName, j);

                            //     out("On invoke run %d, Runtime memory is : %d", j, runtime.totalMemory() - runtime.freeMemory());
                            //return moveAmountAndExpect(client, sampleOrg.getPeerAdmin(), channel, chaincodeName, "1", "201").get();
                            moveAmountAndExpect(client, sampleOrg.getPeerAdmin(), channel, chaincodeName, "1", "" + (200 + j)).thenApply(x -> {

                                try {
                                    blockWalker(channel);
                                } catch (Exception e) {

                                    throw new CompletionException(e);

                                }

                                return null;

                            }).get();
                        }

                        return null;

                    } catch (Exception e) {
                        out("Caught an exception while invoking chaincode");
                        e.printStackTrace();
                        fail("Failed invoking chaincode with error : " + e.getMessage());
                    }

                    return null;

                }).thenApply(dontCare -> {

                    try {
                        blockWalker(channel);
                    } catch (Exception e) {

                        throw new CompletionException(e);

                    }

                    return null;
                }).exceptionally(e -> {
                    if (e instanceof TransactionEventException) {
                        BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                        if (te != null) {
                            fail(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
                        }
                    }
                    fail(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));

                    return null;
                }).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

                out("Running for Channel %s done", channelName);

            } catch (Exception e) {
                out("Caught an exception running channel %s", channel.getName());
                e.printStackTrace();
                fail("Test failed with error : " + e.getMessage());
            }

        }
    }

    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(Thread.currentThread().getName() + ": " + format(format, args));
        System.err.flush();
        System.out.flush();

    }

    File findFileSk(File directory) {

        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];

    }

//    private static final Map<String, String> TX_EXPECTED;
//
//    static {
//        TX_EXPECTED = new HashMap<>();
//        TX_EXPECTED.put("readset1", "Missing readset for channel bar block 1");
//        TX_EXPECTED.put("writeset1", "Missing writeset for channel bar block 1");
//    }

    static void blockWalker(Channel channel) throws InvalidArgumentException, ProposalException, IOException {
        try {
            BlockchainInfo channelInfo = channel.queryBlockchainInfo();

            for (long current = channelInfo.getHeight() - 1; current > -1; --current) {
                Thread.sleep(getRandom(130));
                BlockInfo returnedBlock = channel.queryBlockByNumber(current);
                final long blockNumber = returnedBlock.getBlockNumber();

                out("current block number %d has data hash: %s", blockNumber, Hex.encodeHexString(returnedBlock.getDataHash()));
                out("current block number %d has previous hash id: %s", blockNumber, Hex.encodeHexString(returnedBlock.getPreviousHash()));
                out("current block number %d has calculated block hash is %s", blockNumber, Hex.encodeHexString(SDKUtils.calculateBlockHash(blockNumber, returnedBlock.getPreviousHash(), returnedBlock.getDataHash())));

                final int envelopCount = returnedBlock.getEnvelopCount();
                //   assertEquals(1, envelopCount);
                out("current block number %d has %d envelope count:", blockNumber, returnedBlock.getEnvelopCount());
                int i = 0;
                for (BlockInfo.EnvelopeInfo envelopeInfo : returnedBlock.getEnvelopeInfos()) {
                    ++i;

                    out("  Transaction number %d has transaction id: %s", i, envelopeInfo.getTransactionID());
                    final String channelId = envelopeInfo.getChannelId();
                    //      assertTrue("foo".equals(channelId) || "bar".equals(channelId));

                    out("  Transaction number %d has channel id: %s", i, channelId);
                    out("  Transaction number %d has epoch: %d", i, envelopeInfo.getEpoch());
                    out("  Transaction number %d has transaction timestamp: %tB %<te,  %<tY  %<tT %<Tp", i, envelopeInfo.getTimestamp());
                    out("  Transaction number %d has type id: %s", i, "" + envelopeInfo.getType());

                    if (envelopeInfo.getType() == TRANSACTION_ENVELOPE) {
                        BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo = (BlockInfo.TransactionEnvelopeInfo) envelopeInfo;

                        out("  Transaction number %d has %d actions", i, transactionEnvelopeInfo.getTransactionActionInfoCount());
                        //        assertEquals(1, transactionEnvelopeInfo.getTransactionActionInfoCount()); // for now there is only 1 action per transaction.
                        out("  Transaction number %d isValid %b", i, transactionEnvelopeInfo.isValid());
                        //        assertEquals(transactionEnvelopeInfo.isValid(), true);
                        out("  Transaction number %d validation code %d", i, transactionEnvelopeInfo.getValidationCode());
                        //         assertEquals(0, transactionEnvelopeInfo.getValidationCode());

                        int j = 0;
                        for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionEnvelopeInfo.getTransactionActionInfos()) {
                            ++j;
                            out("   Transaction action %d has response status %d", j, transactionActionInfo.getResponseStatus());
                            //      assertEquals(200, transactionActionInfo.getResponseStatus());
                            out("   Transaction action %d has response message bytes as string: %s", j,
                                    printableString(new String(transactionActionInfo.getResponseMessageBytes(), "UTF-8")));
                            out("   Transaction action %d has %d endorsements", j, transactionActionInfo.getEndorsementsCount());
                            //       assertEquals(2, transactionActionInfo.getEndorsementsCount());

                            for (int n = 0; n < transactionActionInfo.getEndorsementsCount(); ++n) {
                                BlockInfo.EndorserInfo endorserInfo = transactionActionInfo.getEndorsementInfo(n);
                                out("Endorser %d signature: %s", n, Hex.encodeHexString(endorserInfo.getSignature()));
                                out("Endorser %d endorser: %s", n, new String(endorserInfo.getEndorser(), "UTF-8"));
                            }
                            out("   Transaction action %d has %d chaincode input arguments", j, transactionActionInfo.getChaincodeInputArgsCount());
                            for (int z = 0; z < transactionActionInfo.getChaincodeInputArgsCount(); ++z) {
                                out("     Transaction action %d has chaincode input argument %d is: %s", j, z,
                                        printableString(new String(transactionActionInfo.getChaincodeInputArgs(z), "UTF-8")));
                            }

                            out("   Transaction action %d proposal response status: %d", j,
                                    transactionActionInfo.getProposalResponseStatus());
                            out("   Transaction action %d proposal response payload: %s", j,
                                    printableString(new String(transactionActionInfo.getProposalResponsePayload())));

                            TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
                            if (null != rwsetInfo) {
                                out("   Transaction action %d has %d name space read write sets", j, rwsetInfo.getNsRwsetCount());

                                for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
                                    final String namespace = nsRwsetInfo.getNaamespace();
                                    KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();

                                    int rs = -1;
                                    for (KvRwset.KVRead readList : rws.getReadsList()) {
                                        rs++;

                                        out("     Namespace %s read set %d key %s  version [%d:%d]", namespace, rs, readList.getKey(),
                                                readList.getVersion().getBlockNum(), readList.getVersion().getTxNum());

//                                        if ("bar".equals(channelId) && blockNumber == 2) {
//                                            if ("example_cc_go".equals(namespace)) {
//                                                if (rs == 0) {
//                                                    assertEquals("a", readList.getKey());
//                                                    assertEquals(1, readList.getVersion().getBlockNum());
//                                                    assertEquals(0, readList.getVersion().getTxNum());
//                                                } else if (rs == 1) {
//                                                    assertEquals("b", readList.getKey());
//                                                    assertEquals(1, readList.getVersion().getBlockNum());
//                                                    assertEquals(0, readList.getVersion().getTxNum());
//                                                } else {
//                                                    fail(format("unexpected readset %d", rs));
//                                                }
//
//                                                TX_EXPECTED.remove("readset1");
//                                            }
//                                        }
                                    }

                                    rs = -1;
                                    for (KvRwset.KVWrite writeList : rws.getWritesList()) {
                                        rs++;
                                        String valAsString = printableString(new String(writeList.getValue().toByteArray(), "UTF-8"));

                                        out("     Namespace %s write set %d key %s has value '%s' ", namespace, rs,
                                                writeList.getKey(),
                                                valAsString);

//                                        if ("bar".equals(channelId) && blockNumber == 2) {
//                                            if (rs == 0) {
//                                                assertEquals("a", writeList.getKey());
//                                                assertEquals("400", valAsString);
//                                            } else if (rs == 1) {
//                                                assertEquals("b", writeList.getKey());
//                                                assertEquals("400", valAsString);
//                                            } else {
//                                                fail(format("unexpected writeset %d", rs));
//                                            }
//
//                                            TX_EXPECTED.remove("writeset1");
//                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
//            if (!TX_EXPECTED.isEmpty()) {
//                fail(TX_EXPECTED.get(0));
//            }
        } catch (InvalidProtocolBufferRuntimeException e) {
            throw e.getCause();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //CHECKSTYLE.OFF: Method length is 320 lines (max allowed is 150).
    static CompletableFuture<Boolean> installInstantiateChaincode(HFClient client, Channel channel, String chaincodeName, User installingUser) {

        CompletableFuture ret = new CompletableFuture<>();
        // ((CompletableFuture) proposalResponseListenableFuture).completeExceptionally(e);

        try {

            final String channelName = channel.getName();
            boolean isFooChain = FOO_CHANNEL_NAME.equals(channelName);
            out("Running channel %s", channelName);
            channel.setTransactionWaitTime(testConfig.getTransactionWaitTime());
            channel.setDeployWaitTime(testConfig.getDeployWaitTime());

            Collection<Peer> channelPeers = channel.getPeers();
            Collection<Orderer> orderers = channel.getOrderers();
            final ChaincodeID chaincodeID;
            Collection<ProposalResponse> responses;
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();

            ////////////////////////////
            // Install Proposal Request
            //

            //  client.setUserContext(installingUser);

            out("Creating install proposal");

            InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
            installProposalRequest.setChaincodeID(chaincodeID);
            installProposalRequest.setUserContext(installingUser);

            if (isFooChain) {
                // on foo chain install from directory.

                ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
                installProposalRequest.setChaincodeSourceLocation(new File(TEST_FIXTURES_PATH + "/sdkintegration/gocc/sample1"));
            } else {
                // On bar chain install from an input stream.

                installProposalRequest.setChaincodeInputStream(Util.generateTarGzInputStream(
                        (Paths.get(TEST_FIXTURES_PATH, "/sdkintegration/gocc/sample1", "src", CHAIN_CODE_PATH).toFile()),
                        Paths.get("src", CHAIN_CODE_PATH).toString()));

            }

            installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);

            out("Sending install proposal");

            ////////////////////////////
            // only a client from the same org as the peer can issue an install request
            int numInstallProposal = 0;
            //    Set<String> orgs = orgPeers.keySet();
            //   for (SampleOrg org : testSampleOrgs) {

            Collection<Peer> peersFromOrg = channel.getPeers();
            numInstallProposal = numInstallProposal + peersFromOrg.size();
            responses = client.sendInstallProposal(installProposalRequest, channelPeers);

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

            //   client.setUserContext(sampleOrg.getUser(TEST_ADMIN_NAME));
            //  final ChaincodeID chaincodeID = firstInstallProposalResponse.getChaincodeID();
            // Note installing chaincode does not require transaction no need to
            // send to Orderers

            ///////////////
            //// Instantiate chaincode.
            InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
            installProposalRequest.setUserContext(installingUser);
            instantiateProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
            instantiateProposalRequest.setChaincodeID(chaincodeID);
            instantiateProposalRequest.setFcn("init");
            instantiateProposalRequest.setArgs(new String[] {"a", "5000000", "b", "" + (200)});
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

            out("Sending instantiateProposalRequest to all peers with arguments: a and b set to 100 and %s respectively", "" + (200));
            successful.clear();
            failed.clear();

            if (isFooChain) {  //Send responses both ways with specifying peers and by using those on the channel.
                responses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
            } else {
                responses = channel.sendInstantiationProposal(instantiateProposalRequest);

            }
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
            out("Sending instantiateTransaction to orderer with a and b set to 100 and %s respectively", "" + (200));
            channel.sendTransaction(successful, orderers).thenApply(transactionEvent -> {

                assertTrue(transactionEvent.isValid()); // must be valid to be here.
                out("Finished instantiate transaction with transaction id %s", transactionEvent.getTransactionID());

                return ret.complete(true);

            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        fail(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
                    }
                }
                fail(format("Test failed with %s exception %s", e.getClass().getName(), e.getMessage()));

                return ret.completeExceptionally(e);
            });

        } catch (Exception e) {

            ret.completeExceptionally(e);

        }

        return ret;

    }
    //CHECKSTYLE.ON: Method length is 320 lines (max allowed is 150).

    static CompletableFuture<Boolean> moveAmountAndExpect(HFClient client, User user, Channel channel, String chaincodeName, String moveAmount, String expect) {

        CompletableFuture ret = new CompletableFuture<Boolean>();

        try {
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            ////////////

            final ChaincodeID chaincodeID;

            chaincodeID = ChaincodeID.newBuilder().setName(chaincodeName)
                    .setVersion(CHAIN_CODE_VERSION)
                    .setPath(CHAIN_CODE_PATH).build();

            ///////////////
            /// Send transaction proposal to all peers
            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chaincodeID);
            transactionProposalRequest.setFcn("invoke");
            transactionProposalRequest.setArgs(new String[] {"move", "a", "b", moveAmount});
            transactionProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());

            transactionProposalRequest.setUserContext(user);

            out("sending transaction proposal to all peers with arguments: move(a,b,%s)", moveAmount);

            Collection<ProposalResponse> invokePropResp = channel.sendTransactionProposal(transactionProposalRequest);
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == ChaincodeResponse.Status.SUCCESS) {
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
                throw new AssertionError(format("Expected only one set of consistent move proposal responses but got %d", proposalConsistencySets.size()));

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

            channel.sendTransaction(successful, user).thenApply(transactionEvent -> {
                try {

                    assertTrue(transactionEvent.isValid()); // must be valid to be here.
                    out("Finished transaction with transaction id %s", transactionEvent.getTransactionID());
                    String testTxID = transactionEvent.getTransactionID(); // used in the channel queries later

                    ////////////////////////////
                    // Send Query Proposal to all peers
                    //
                    out("Now query chaincode for the value of b.");
                    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
                    queryByChaincodeRequest.setArgs(new String[] {"query", "b"});
                    queryByChaincodeRequest.setFcn("invoke");
                    queryByChaincodeRequest.setChaincodeID(chaincodeID);

                    Map<String, byte[]> tm2 = new HashMap<>();
                    tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
                    tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
                    queryByChaincodeRequest.setTransientMap(tm2);

                    Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
                    for (ProposalResponse proposalResponse : queryProposals) {
                        if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                            fail("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() +
                                    ". Messages: " + proposalResponse.getMessage()
                                    + ". Was verified : " + proposalResponse.isVerified());
                        } else {
                            String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                            out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                            assertEquals(payload, expect);
                        }
                    }

                    ret.complete(true);
                } catch (Exception e) {
                    out("Caught exception while running query");
                    e.printStackTrace();
                    fail("Failed during chaincode query with error : " + e.getMessage());
                }

                return ret;
            }).exceptionally(e -> {
                if (e instanceof TransactionEventException) {
                    BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                    if (te != null) {
                        fail(format("Transaction with txid %s failed. %s", te.getTransactionID(), e.getMessage()));
                    }
                }

                ret.completeExceptionally(e);
                return ret;
            });

        } catch (Exception e) {
            ret.completeExceptionally(e);
        }

        return ret;

    }

    static String printableString(final String string) {
        int maxLogStringLength = 64;
        if (string == null || string.length() == 0) {
            return string;
        }

        String ret = string.replaceAll("[^\\p{Print}]", "?");

        ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..." : "");

        return ret;

    }

}
