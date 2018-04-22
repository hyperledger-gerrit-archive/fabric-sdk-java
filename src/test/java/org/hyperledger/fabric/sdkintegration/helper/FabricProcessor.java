/*
 *  Copyright 2018 Mediaocean - All Rights Reserved.
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

package org.hyperledger.fabric.sdkintegration.helper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Channel.PeerOptions;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.UpdateChannelConfiguration;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric.sdkintegration.AddingAnOrgIT;
import org.hyperledger.fabric.sdkintegration.SampleOrg;
import org.hyperledger.fabric.sdkintegration.SampleStore;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides management functions for transactions on the orderer
 *
 * @author alana
 *
 */
public class FabricProcessor {
    private static final String PEM = "ordererOrganizations/DOMAIN/orderers/NAME/tls/server.crt";

    public static final String BASEPATH = "src/test/fixture/sdkintegration/adding_an_org/";

    private final AddingAnOrgIT ait;
    private final TestConfig testConfig;
    private final SampleStore sampleStore;

    private final ConfigtxlatorHelper ctxi;

    private final String genesischannel = "testchainid"; // TODO check this
    private final String CONSORTIUM = "SampleConsortium";
    // private String genesisprofile;
    // private String genesisfile;

    public FabricProcessor(AddingAnOrgIT ait) {
        this.ait = ait;
        this.testConfig = ait.getTestconfig();
        this.sampleStore = ait.getSampleStore();
        ctxi = new ConfigtxlatorHelper(testConfig.getFabricConfigTxLaterLocation());
    }

    /**
     * Add a new org (org3) to an existing channel
     *
     * @param channelName
     * @param channelOrg Org with the ability to update channel configuration
     * @param newOrg Org to add to channel (org3 - with pregenerated JSON remember)
     * @throws Exception
     */
    public void addMemberToFooChannel(SampleOrg newOrg, SampleOrg... signers) throws Exception {
        String channelName = "foo";
        System.out.println(String.format("Attempting to add %s to the foo channel", newOrg.getName()));

        SampleOrg randomSigner = signers[0]; // Any one that can sign will do. This me being too lazy to add a random...

        HFClient client = ait.getClients1to3().get(randomSigner.getMSPID());
        Channel c = reconstructFooChannel(signers[0]);

        byte[] ccb = c.getChannelConfigurationBytes();

        // Turn the channel config into something you can manipulate
        Map<String, Object> updatedresult = ctxi.map(ctxi.decodeJSON(ccb));
        Map<String, Object> updateJson = ctxi.map(readOrgJson(newOrg.getMSPID()));

        updatedresult = addMemberToChannel(newOrg.getMSPID(), updatedresult, updateJson);

        byte[] newConfigBytes = ctxi.encodeJSON(ctxi.toString(updatedresult));

        byte[] updateBytes = ctxi.update(channelName, ccb, newConfigBytes);

        UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration(updateBytes);

        // So the policy for this channel requires all the participants to sign before it can be updated.
        // Check yours before copying this slavishly - sometimes only one is enough
        byte[][] sigs = new byte[signers.length][];
        for (int i = 0; i < signers.length; i++) {
            sigs[i] = client.getUpdateChannelConfigurationSignature(updateChannelConfiguration, signers[i].getPeerAdmin());
        }

        // Update the signed configuration
        c.updateChannelConfiguration(updateChannelConfiguration, sigs);
    }

    /**
     * Join the client to the orderer (system) channel
     *
     * @param client
     * @param ordererOrg
     * @return
     * @throws TransactionException
     * @throws InvalidArgumentException
     */
    public Channel joinOrdererChannel(HFClient client, SampleOrg ordererOrg) throws Exception {
        System.out.println("Joining orderer channel. Remember you only do this once...");

        // This works only so long as you only have one orderer and the domain doesn't change.
        // Multiple orderers appear to work fine so long as you only tie a peer to just one of them and use that orderer crypto
        String on = "orderer.example.com";
        String domain = "example.com";
        Channel c = client.newChannel(genesischannel);

        for (String ordererName : ordererOrg.getOrdererNames()) {
            Properties p = new Properties();
            // if (testConfig.isRunningFabricTLS()) {
            // p.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", maxInboundMessageSize);
            p.put("sslProvider", "openSSL");
            p.put("hostnameOverride", ordererOrg.getName());
            p.put("negotiationType", "TLS");
            p.put("pemBytes", getPem(on, domain));
            // }
            c.addOrderer(client.newOrderer(ordererName, ordererOrg.getOrdererLocation(ordererName), p));
        }

        for (String peerName : ordererOrg.getPeerNames()) {
            Properties p = new Properties();
            if (testConfig.isRunningFabricTLS()) {
                p.put("sslProvider", "openSSL");
                p.put("hostnameOverride", ordererOrg.getName());
                p.put("negotiationType", "TLS");
                p.put("pemBytes", getPem(on, domain));
            }
            Peer peer = client.newPeer(on, ordererOrg.getPeerLocation(peerName), p);
            c.joinPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
        }

        if (!c.isInitialized()) {
            System.out.println("Initializing orderer channel");
            c.initialize();
        }

        return c;
    }

    /**
     * Add a new member to the consortium. Allows you to generate channels for this member.
     *
     * @param org
     * @throws Exception
     */
    public void addMemberToConsortium(HFClient client, SampleOrg ordererOrg, SampleOrg org) throws Exception {
        Channel c = getChannel(client, ordererOrg);

        byte[] ccb = c.getChannelConfigurationBytes();
        String responseAsString = ctxi.decodeJSON(ccb);

        Map<String, Object> updatedresult = ctxi.map(responseAsString);

        Map<String, Object> updateJson = ctxi.map(readOrgJson(org.getMSPID()));

        addMemberToConsortium(CONSORTIUM, org.getMSPID(), updatedresult, updateJson);

        byte[] newConfigBytes = ctxi.encodeJSON(ctxi.toString(updatedresult));

        byte[] updateBytes = ctxi.update(genesischannel, ccb, newConfigBytes);

        // Sign the update configuration
        UpdateChannelConfiguration updateChannelConfiguration = new UpdateChannelConfiguration(updateBytes);
        byte[] sig = client.getUpdateChannelConfigurationSignature(updateChannelConfiguration, ordererOrg.getPeerAdmin());

        // Now do actual channel update.
        c.updateChannelConfiguration(updateChannelConfiguration, sig);
    }

    /**
     * <p>
     * Add a member to a previously mapped genesis channel block
     * </p>
     * <p>
     * I know, I know it's awful code but it serves to illustrate the levels this thing nests to. You could also do it with a JsonObject and some . notation - which fits better with stuff like the
     * NetworkConfig code - but this code was already written.
     * </p>
     *
     * @param consortium Name of the consortium in the block to be edited
     * @param msp
     * @param config
     * @param updateJson
     */
    @SuppressWarnings("unchecked")
    private void addMemberToConsortium(String consortium, String msp, Map<String, Object> config, Map<String, Object> configJson) {
        Map<String, Object> x = (Map<String, Object>) config.get("channel_group");
        Map<String, Object> x1 = (Map<String, Object>) x.get("groups");
        Map<String, Object> x2 = (Map<String, Object>) x1.get("Consortiums");
        Map<String, Object> x3 = (Map<String, Object>) x2.get("groups");
        Map<String, Object> x4 = (Map<String, Object>) x3.get(consortium);
        Map<String, Object> x5 = (Map<String, Object>) x4.get("groups");
        x5.put(msp, configJson);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> addMemberToChannel(String msp, Map<String, Object> config, Map<String, Object> configJson) {
        Map<String, Object> x = (Map<String, Object>) config.get("channel_group");
        Map<String, Object> x1 = (Map<String, Object>) x.get("groups");
        Map<String, Object> x2 = (Map<String, Object>) x1.get("Application");
        Map<String, Object> x3 = (Map<String, Object>) x2.get("groups");
        x3.put(msp, configJson);
        return config;
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private Map<String, Object> addAnchorPeer(String msp, Map<String, Object> config, Map<String, Object> configJson) {
        Map<String, Object> x = (Map<String, Object>) config.get("channel_group");
        Map<String, Object> x1 = (Map<String, Object>) x.get("groups");
        Map<String, Object> x2 = (Map<String, Object>) x1.get("Application");
        Map<String, Object> x3 = (Map<String, Object>) x2.get("groups");
        Map<String, Object> x4 = (Map<String, Object>) x3.get(msp);
        Map<String, Object> x5 = (Map<String, Object>) x4.get("values");

        Map<String, Object> y = (Map<String, Object>) configJson.get("payload");
        Map<String, Object> y1 = (Map<String, Object>) y.get("data");
        Map<String, Object> y2 = (Map<String, Object>) y1.get("config_update");
        Map<String, Object> y3 = (Map<String, Object>) y2.get("write_set");
        Map<String, Object> y4 = (Map<String, Object>) y3.get("groups");
        Map<String, Object> y5 = (Map<String, Object>) y4.get("Application");
        Map<String, Object> y6 = (Map<String, Object>) y5.get("groups");
        Map<String, Object> y7 = (Map<String, Object>) y6.get(msp);
        Map<String, Object> y8 = (Map<String, Object>) y7.get("values");
        Map<String, Object> y9 = (Map<String, Object>) y8.get("AnchorPeers");
        x5.put("AnchorPeers", y9);
        return config;
    }

    /**
     * <p>
     * Hard coded to pull the pre-generated org3.json from the channel-artifacts directory
     * </p>
     * <p>
     * Create it from configtx.yaml like this: configtxgen -printOrg Org3MSP > ./channel-artifacts/org3.json
     * </p>
     *
     * @param msp MSP to pull information from
     * @return
     */
    private String readOrgJson(String msp) {
        Path p = null;
        try {
            p = Paths.get(BASEPATH, "channel-artifacts", "org3.json");
            return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
        } catch (IOException e) {
            if (p != null) {
                String msg = "JSON file is missing for org3. Expected at: " + p.toAbsolutePath();
                System.out.println(msg);
                throw new RuntimeException(msg);
            } else {
                String msg = "Unable to find path to org3 JSON. Expected at: " + BASEPATH + "/channel-artifacts/org3.json";
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
        }
    }

    /**
     *
     * @param client
     * @param ordererOrg
     * @return
     * @throws TransactionException
     * @throws InvalidArgumentException
     */
    private Channel getChannel(HFClient client, SampleOrg ordererOrg) throws Exception {
        System.out.println("Getting orderer channel for " + ordererOrg.getMSPID());

        Channel c = client.getChannel(genesischannel);
        if (c == null) {

            // This works only so long as you only have one orderer and the domain doesn't change.
            // Multiple orderers work fine so long as you only tie a peer to one of them and use that one's crypto
            String on = "orderer.example.com";
            String domain = "example.com";
            c = client.newChannel(genesischannel);

            for (String ordererName : ordererOrg.getOrdererNames()) {
                Properties p = new Properties();
                if (testConfig.isRunningFabricTLS()) {
                    p.put("sslProvider", "openSSL");
                    p.put("hostnameOverride", ordererOrg.getName());
                    p.put("negotiationType", "TLS");
                    p.put("pemBytes", getPem(on, domain));
                }
                c.addOrderer(client.newOrderer(ordererName, ordererOrg.getOrdererLocation(ordererName), p));
            }

            for (String peerName : ordererOrg.getPeerNames()) {
                Properties p = new Properties();
                if (testConfig.isRunningFabricTLS()) {
                    p.put("sslProvider", "openSSL");
                    p.put("hostnameOverride", ordererOrg.getName());
                    p.put("negotiationType", "TLS");
                    p.put("pemBytes", getPem(on, domain));
                }
                Peer peer = client.newPeer(on, ordererOrg.getPeerLocation(peerName), p);
                c.addPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
            }

            if (!c.isInitialized()) {
                System.out.println("Initializing orderer channel");
                c.initialize();
            }
        }
        return c;
    }

    /**
     * Get TLS server certificate for an orderer
     *
     * @param name
     * @param domain
     * @return
     * @throws IOException
     */
    private byte[] getPem(String name, String domain) throws IOException {
        String formattedPem = PEM.replaceAll("DOMAIN", domain).replaceAll("NAME", name);
        System.out.println("Reading pem from: " + formattedPem);

        return Files.readAllBytes(Paths.get(AddingAnOrgIT.V1PATH, "crypto-config", formattedPem));
    }

    /**
     * Reconstruct the foo channel. This is just a quick cut down version of reconstruct in the End2endAndBackAgainIT
     *
     * @param client
     * @param org
     * @return
     * @throws Exception
     */
    public Channel constructFooChannel(SampleOrg org) throws Exception {
        System.out.println("Constructing the foo channel for " + org.getMSPID());
        String channel = "foo";

        HFClient client = ait.getClients1to3().get(org.getMSPID());

        Channel newChannel = client.getChannel(channel);
        if (newChannel == null) {
            System.out.println("Channel " + channel + " needs to be initialized");
            newChannel = client.newChannel(channel);

            Properties clientTLSProperties = new Properties();

            final String clientPEMTLSCertificate = sampleStore.getClientPEMTLSCertificate(org);
            if (clientPEMTLSCertificate != null) {
                System.out.println("Using clientPEMTLSCertificate");
                clientTLSProperties.put("clientCertBytes", clientPEMTLSCertificate.getBytes(UTF_8));
            }
            final String clientPEMTLSKey = sampleStore.getClientPEMTLSKey(org);

            if (clientPEMTLSKey != null) {
                System.out.println("Using clientPEMTLSKey");
                clientTLSProperties.put("clientKeyBytes", clientPEMTLSKey.getBytes(UTF_8));
            }

            for (String ordererName : org.getOrdererNames()) {
                System.out.println("Adding orderer " + ordererName);
                Properties ordererProperties = (Properties) clientTLSProperties.clone();
                ordererProperties.putAll(testConfig.getOrdererProperties(ordererName));
                newChannel.addOrderer(client.newOrderer(ordererName, org.getOrdererLocation(ordererName), ordererProperties));
            }

            for (String peerName : org.getPeerNames()) {
                System.out.println("Joining peer " + peerName);
                Properties peerProperties = testConfig.getPeerProperties(peerName);
                peerProperties.putAll(clientTLSProperties);

                Peer peer = client.newPeer(peerName, org.getPeerLocation(peerName), peerProperties);
                newChannel.joinPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
            }

            newChannel.initialize();
        }
        return newChannel;
    }

    /**
     * Reconstruct the foo channel. This is just a quick cut down version of reconstruct in the End2endAndBackAgainIT
     *
     * @param client
     * @param sampleOrg
     * @return
     * @throws Exception
     */
    public Channel reconstructFooChannel(SampleOrg sampleOrg) throws Exception {
        System.out.println("Reconstructing the foo channel for " + sampleOrg.getMSPID());
        String channel = "foo";

        HFClient client = ait.getClients1to3().get(sampleOrg.getMSPID());

        Channel newChannel = client.getChannel(channel);
        if (newChannel == null) {
            System.out.println("Channel " + channel + " needs to be initialized");

            Properties clientTLSProperties = new Properties();

            final String clientPEMTLSCertificate = sampleStore.getClientPEMTLSCertificate(sampleOrg);
            if (clientPEMTLSCertificate != null) {
                System.out.println("Using clientPEMTLSCertificate");
                clientTLSProperties.put("clientCertBytes", clientPEMTLSCertificate.getBytes(UTF_8));
            }
            final String clientPEMTLSKey = sampleStore.getClientPEMTLSKey(sampleOrg);

            if (clientPEMTLSKey != null) {
                System.out.println("Using clientPEMTLSKey");
                clientTLSProperties.put("clientKeyBytes", clientPEMTLSKey.getBytes(UTF_8));
            }

            newChannel = client.newChannel(channel);

            for (String ordererName : sampleOrg.getOrdererNames()) {
                System.out.println("Adding orderer " + ordererName);
                Properties ordererProperties = (Properties) clientTLSProperties.clone();
                ordererProperties.putAll(testConfig.getOrdererProperties(ordererName));
                newChannel.addOrderer(client.newOrderer(ordererName, sampleOrg.getOrdererLocation(ordererName), ordererProperties));
            }

            for (String peerName : sampleOrg.getPeerNames()) {
                System.out.println("Adding peer " + peerName);
                Properties peerProperties = testConfig.getPeerProperties(peerName);
                peerProperties.putAll(clientTLSProperties);

                Peer peer = client.newPeer(peerName, sampleOrg.getPeerLocation(peerName), peerProperties);
                newChannel.addPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
            }

            newChannel.initialize();
        }
        return newChannel;
    }

    /**
     * Reconstruct the foo channel. This is just a quick cut down version of reconstruct in the End2endAndBackAgainIT
     *
     * @param client
     * @param org
     * @return
     * @throws Exception
     */
    public Channel reconstructFooChannel(SampleOrg org, SampleOrg... others) throws Exception {
        System.out.println("Reconstructing the foo channel for " + org.getMSPID());
        String channel = "foo";

        HFClient client = ait.getClients1to3().get(org.getMSPID());

        Channel newChannel = client.getChannel(channel);
        if (newChannel == null) {
            System.out.println("Channel " + channel + " needs to be initialized");

            Properties clientTLSProperties = new Properties();

            final String clientPEMTLSCertificate = sampleStore.getClientPEMTLSCertificate(org);
            if (clientPEMTLSCertificate != null) {
                System.out.println("Using clientPEMTLSCertificate");
                clientTLSProperties.put("clientCertBytes", clientPEMTLSCertificate.getBytes(UTF_8));
            }
            final String clientPEMTLSKey = sampleStore.getClientPEMTLSKey(org);

            if (clientPEMTLSKey != null) {
                System.out.println("Using clientPEMTLSKey");
                clientTLSProperties.put("clientKeyBytes", clientPEMTLSKey.getBytes(UTF_8));
            }

            newChannel = client.newChannel(channel);

            for (String ordererName : org.getOrdererNames()) {
                System.out.println("Adding orderer " + ordererName);
                Properties ordererProperties = (Properties) clientTLSProperties.clone();
                ordererProperties.putAll(testConfig.getOrdererProperties(ordererName));
                newChannel.addOrderer(client.newOrderer(ordererName, org.getOrdererLocation(ordererName), ordererProperties));
            }

            for (String peerName : org.getPeerNames()) {
                System.out.println("Adding peer " + peerName);
                Properties peerProperties = testConfig.getPeerProperties(peerName);
                peerProperties.putAll(clientTLSProperties);

                Peer peer = client.newPeer(peerName, org.getPeerLocation(peerName), peerProperties);
                newChannel.addPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
            }

            // This is hacked to only add peers and ignore eventing
            for (SampleOrg other : others) {
                for (String peerName : other.getPeerNames()) {
                    System.out.println("Adding peer " + peerName);
                    Properties peerProperties = testConfig.getPeerProperties(peerName);
                    peerProperties.putAll(clientTLSProperties);

                    Peer peer = client.newPeer(peerName, other.getPeerLocation(peerName), peerProperties);
                    newChannel.addPeer(peer, PeerOptions.createPeerOptions().setPeerRoles(PeerRole.NO_EVENT_SOURCE));
                }
            }

            newChannel.initialize();
        }
        return newChannel;
    }

    public AddingAnOrgIT getAit() {
        return ait;
    }

}
