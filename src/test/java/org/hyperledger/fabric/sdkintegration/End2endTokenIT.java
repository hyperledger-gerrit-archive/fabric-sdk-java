package org.hyperledger.fabric.sdkintegration;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.identity.IdentityFactory;
import org.hyperledger.fabric.sdk.identity.SigningIdentity;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric.sdk.token.ProverPeer;
import org.junit.Test;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hyperledger.fabric.protos.token.ProverPeer.TokenOutput;
import static org.hyperledger.fabric.protos.token.ProverPeer.TokenToIssue;
import static org.hyperledger.fabric.protos.token.Transaction.TokenTransaction;
import static org.hyperledger.fabric.sdk.Channel.TransactionOptions;
import static org.junit.Assert.*;

/*
    This runs a version of end2end but with Node chaincode.
    It requires that End2endIT has been run already to do all enrollment and setting up of orgs,
    creation of the channels. None of that is specific to chaincode deployment language.
 */

public class End2endTokenIT extends End2endIT {

    {

        testName = "End2endTokenIT";  //Just print out what test is really running.

        CHAIN_CODE_FILEPATH = "sdkintegration/gocc/sample1";
        CHAIN_CODE_NAME = "example_cc_go";
        CHAIN_CODE_PATH = "github.com/example_cc";
        CHAIN_CODE_VERSION = "1";
        CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;

        testUser1 = "user1";
    }

    private static final String FOO_CHANNEL_NAME = "foo";
    private static final TestConfig testConfig = TestConfig.getConfig();
    private Collection<SampleOrg> testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();

    void tokenTest(HFClient client, Channel channel) throws InterruptedException, ExecutionException, TimeoutException {
        out("\nPlay with tokens on chain %s ", channel.getName());

        SigningIdentity signingIdentity = IdentityFactory.getSigningIdentity(client.getCryptoSuite(), client.getUserContext());
        ByteString serializedIdentity = signingIdentity.createSerializedIdentity().toByteString();

        // todo change to prover peer
//        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.PROVER_PEER));
        Collection<Peer> peers = channel.getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER));
        peers.forEach(System.out::println);

        List<ProverPeer> provers = peers.stream()
                .map(peer -> new ProverPeer(signingIdentity, channel.getName(), peer.getUrl(), peer.getProperties()))
                .collect(Collectors.toList());

        // we just use the first one
        ProverPeer prover = provers.get(0);

        // low-level API example
        List<TokenToIssue> tokensToIssue = Lists.newArrayList();
        tokensToIssue.add(TokenToIssue.newBuilder().setQuantity(100).setType("USD").setRecipient(serializedIdentity).build());
        tokensToIssue.add(TokenToIssue.newBuilder().setQuantity(50).setType("Euro").setRecipient(serializedIdentity).build());

        // send to a single prover
        // CompletableFuture<TokenTransaction> tokenTxF = prover.requestImport(tokensToIssue);
        // TokenTransaction tokenTx = tokenTxF.get(5, TimeUnit.SECONDS);

        // send to all prover peers ... take first response
        CompletableFuture[] futures = new CompletableFuture[provers.size()];
        provers.stream().map(p -> p.requestImport(tokensToIssue)).collect(Collectors.toList()).toArray(futures);

        TokenTransaction tokenTx = (TokenTransaction) CompletableFuture.anyOf(futures).get(5, TimeUnit.SECONDS);

        channel.sendTokenTransaction(tokenTx, TransactionOptions.createTransactionOptions());

        // TODO fetch TxEvents from prover peers

        out("Sleep 10seconds");
        Thread.sleep(10000);

        List<TokenOutput> tokens = prover.listTokens().get(5, TimeUnit.SECONDS);
        tokens.forEach(System.out::println);

    }

    @Override
    public void runFabricTest(final SampleStore sampleStore) throws Exception {

        ////////////////////////////
        // Setup client

        //Create instance of client.
        HFClient client = HFClient.createNewInstance();

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        ////////////////////////////
        //Construct and run the channels
        SampleOrg sampleOrg = testConfig.getIntegrationTestsSampleOrg("peerOrg1");
        Channel fooChannel = constructChannel(FOO_CHANNEL_NAME, client, sampleOrg);
        sampleStore.saveChannel(fooChannel);
//        runChannel(client, fooChannel, true, sampleOrg, 100);

        tokenTest(client, fooChannel);

        assertFalse(fooChannel.isShutdown());
        fooChannel.shutdown(true); // Force foo channel to shutdown clean up resources.
        assertTrue(fooChannel.isShutdown());

        assertNull(client.getChannel(FOO_CHANNEL_NAME));
        out("That's all folks!");
    }

    @Override
    @Test
    public void setup() throws Exception {
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        sampleStore = new SampleStore(sampleStoreFile);
        enrollUsersSetup(sampleStore); //This enrolls users with fabric ca and setups sample store to get users later.
        runFabricTest(sampleStore); //Runs Fabric tests with constructing channels, joining peers, exercising chaincode
    }
}
