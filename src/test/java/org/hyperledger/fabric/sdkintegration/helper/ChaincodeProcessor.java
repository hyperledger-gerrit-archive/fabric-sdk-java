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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hyperledger.fabric.protos.peer.Query.ChaincodeInfo;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.SDKUtils;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.UpgradeProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdkintegration.AddingAnOrgIT;
import org.hyperledger.fabric.sdkintegration.SampleOrg;
import org.hyperledger.fabric.sdkintegration.Util;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChaincodeProcessor {
    private static final String BASEPATH = FabricProcessor.BASEPATH;
    private static final String POLICY_YAML = "tripartychaincodeendorsementpolicy.yaml";

    private static final String TEST_FIXTURES_PATH = "src/test/fixture";

    private static final String CHAIN_CODE_FILEPATH = "sdkintegration/gocc/sample1";

    public static final String METHOD = "method";
    public static final String INIT = "init";

    public static final String HYPERLEDGERFABRIC = "HyperLedgerFabric";
    public static final String QUERY_CHAINCODE = "QueryByChaincodeRequest";
    public static final String QUERY_CHAINCODE_JAVA = QUERY_CHAINCODE + ":JavaSDK";

    public static final String INVOKE_CHAINCODE = "TransactionProposalRequest";
    public static final String INVOKE_CHAINCODE_JAVA = "TransactionProposalRequest:JavaSDK";

    public static final String INSTANTIATE_CHAINCODE = "InstantiateProposalRequest";
    public static final String INSTANTIATE_CHAINCODE_JAVA = "InstantiateProposalRequest:JavaSDK";

    private final FabricProcessor fp;
    private final AddingAnOrgIT ait;

    public ChaincodeProcessor(FabricProcessor fp) {
        this.fp = fp;
        this.ait = fp.getAit();
    }

    /**
     * Install and instantiate chaincode on a channel.<br />
     *
     * @throws Exception
     */
    public void installChaincode(ChaincodeID cc, SampleOrg... orgs) throws Exception {
        String channelName = "foo";
        System.out.println(String.format("Installing %s : %s on %s", cc.getName(), cc.getVersion(), channelName));

        // This was hacked from something more generic. Channel would be required here.
        install(cc, orgs);
        instantiate(cc, orgs);
    }

    /**
     * Install chaincode on all the peers for a channel
     *
     * @param ccid chaincode to install
     * @param channel channel to install chaincode onto
     * @throws Exception
     */
    private void install(ChaincodeID ccid, SampleOrg... orgs) throws Exception {
        String channelName = "foo";

        int proposals = 0;
        Collection<ProposalResponse> responses = new ArrayList<>();

        // Now we go install the chaincode on all the peers that it wasn't already installed on
        // There's a problem here - you need the peeradmin for the org to install CC on that peer so we go loopity loop around all the orgs

        for (SampleOrg org : orgs) {
            System.out.println(String.format("Installing chaincode %s:%s on %s for %s", ccid.getName(), ccid.getVersion(), channelName, org.getName()));

            HFClient client = ait.getClients1to3().get(org.getMSPID());
            Channel c = fp.reconstructFooChannel(org);

            InstallProposalRequest ipr = client.newInstallProposalRequest();
            ipr.setChaincodeID(ccid);
            ipr.setChaincodeVersion(ccid.getVersion());

            // The "src" here is due to how it is saved. You don't need it to load. You DO need it in the pathPrefix though.
            Path base = Paths.get(TEST_FIXTURES_PATH, CHAIN_CODE_FILEPATH, "src", ccid.getPath());
            Path src = Paths.get("src", ccid.getPath());

            ipr.setChaincodeInputStream(Util.generateTarGzInputStream(base.toFile(), src.toString()));

            // Peers are filtered in the channel factory so only the peers joined to this channel on construction were added to the channel object
            // You also need to check if this chaincode was previously installed (for another channel on the same peer for instance)
            Collection<Peer> peers = new ArrayList<>();
            for (Peer peer : c.getPeers()) {
                if (!checkInstalledChaincode(client, peer, ccid)) {
                    peers.add(peer);
                }
            }
            if (peers.size() > 0) {
                proposals += peers.size();
                responses.addAll(client.sendInstallProposal(ipr, peers));
            }
        }

        Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();

        // All the collected responses are here - let's see how we did
        for (ProposalResponse response : responses) {
            switch (response.getStatus()) {
                case SUCCESS:
                    System.out.println(String.format("Successful install proposal response %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                    successful.add(response);
                    break;

                case FAILURE:
                    System.out.println(String.format("Failed install proposal response %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                    failed.add(response);
                    break;

                case UNDEFINED:
                    System.out.println(String.format("Undefined install proposal response %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
                    failed.add(response);
                    break;

                default:
                    break;
            }
        }

        System.out.println(
                        String.format("Transmitted %d install proposals. Received %d responses. Successful+verified: %d . Failed: %d", proposals, responses.size(), successful.size(), failed.size()));

        if (failed.size() > 0) {
            System.err.println(String.format("Failed chaincode install: %s", failed.iterator().next().getMessage()));
            throw new Exception("Failed chaincode install");
        }
    }

    /**
     * Instantiate the chaincode. Pick a peer and have at it. You do this once on one peer after you install the cc on all the peers.
     *
     * @param chaincodeId
     * @throws Exception
     */
    private void instantiate(ChaincodeID chaincodeId, SampleOrg... orgs) throws Exception {
        String channel = "foo";

        for (SampleOrg org : orgs) {
            System.out.println(String.format("Instantiating chaincode %s:%s on %s for %s", chaincodeId.getName(), chaincodeId.getVersion(), channel, org.getMSPID()));

            HFClient client = ait.getClients1to3().get(org.getMSPID());
            Channel c = fp.reconstructFooChannel(org);

            boolean isInstalled = false;
            boolean isInstantiated = false;

            for (Peer p : c.getPeers()) {
                isInstalled = (isInstalled) ? isInstalled : checkUpgradeRequired(c, p, chaincodeId);
                isInstantiated = (isInstantiated) ? isInstantiated : checkInstantiatedChaincode(c, p, chaincodeId);
            }

            // If there's an exact match between this chaincode and what's already instantiated on the peers do nothing
            if (!isInstantiated) {

                // If the chaincode name is there then something was already instantiated and this is an upgrade
                if (isInstalled) {
                    upgrade(client, c, chaincodeId);
                } else { // Nothing matched that name, so this is the first time through - so instantiate
                    instantiate(client, c, chaincodeId);
                }
            }
        }
    }

    /**
     * Instantiate chaincode for a client on a channel
     *
     * @param client
     * @param c
     * @param chaincodeId
     * @throws Exception
     */
    private void instantiate(HFClient client, Channel c, ChaincodeID chaincodeId) throws Exception {
        InstantiateProposalRequest ipr = client.newInstantiationProposalRequest();
        ipr.setProposalWaitTime(ait.getTestconfig().getProposalWaitTime());
        ipr.setChaincodeID(chaincodeId);
        ipr.setFcn(INIT);

        ipr.setArgs(new String[] {"a", "500", "b", "200"});

        Map<String, byte[]> tm = new HashMap<>();
        tm.put(HYPERLEDGERFABRIC, INSTANTIATE_CHAINCODE_JAVA.getBytes(UTF_8));
        tm.put(METHOD, INSTANTIATE_CHAINCODE.getBytes(UTF_8));
        ipr.setTransientMap(tm);

        ipr.setChaincodeEndorsementPolicy(createEndorsementPolicy());

        System.out.println("Sending instantiateProposalRequest to the peers");
        Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();
        Collection<ProposalResponse> responses = c.sendInstantiationProposal(ipr);

        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
                System.out.println(String.format("Succesful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                failed.add(response);
            }
        }

        System.out.println(String.format("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size()));
        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            System.err.println(String.format("Not enough endorsers for instantiate : %d endorser failed with %s. Was verified:%s", successful.size(), first.getMessage(),
                            first.isVerified() ? "true" : "false"));
        }

        System.out.println("Sending instantiateTransaction to orderers");
        c.sendTransaction(successful, c.getOrderers()).get();
    }

    /**
     * Upgrade the chaincode for a client on a channel.
     *
     * @param chaincodeId
     * @param channel
     * @throws Exception
     */
    private void upgrade(HFClient client, Channel c, ChaincodeID chaincodeId) throws Exception {
        UpgradeProposalRequest ipr = client.newUpgradeProposalRequest();
        ipr.setProposalWaitTime(ait.getTestconfig().getProposalWaitTime());
        ipr.setChaincodeID(chaincodeId);
        ipr.setFcn(INIT);
        ipr.setArgs(new String[] {"a", "500", "b", "200"});

        Map<String, byte[]> tm = new HashMap<>();
        tm.put(HYPERLEDGERFABRIC, INSTANTIATE_CHAINCODE_JAVA.getBytes(UTF_8));
        tm.put(METHOD, INSTANTIATE_CHAINCODE.getBytes(UTF_8));
        ipr.setTransientMap(tm);

        ipr.setChaincodeEndorsementPolicy(createEndorsementPolicy());

        System.out.println("Sending upgradeProposalRequest to the peers");
        Collection<ProposalResponse> successful = new ArrayList<>();
        Collection<ProposalResponse> failed = new ArrayList<>();
        Collection<ProposalResponse> responses = c.sendUpgradeProposal(ipr);

        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
                System.out.println(String.format("Succesful upgrade proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName()));
            } else {
                failed.add(response);
            }
        }

        System.out.println(String.format("Received %s upgrade proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size()));
        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            System.err.println(String.format("Not enough endorsers for upgrade : %d endorser failed with %s. Was verified:%s", successful.size(), first.getMessage(),
                            first.isVerified() ? "true" : "false"));
        }

        System.out.println("Sending upgradeTransaction to orderers");
        c.sendTransaction(successful, c.getOrderers()).get();
    }

    /**
     * <p>
     * To read a chaincode endorsement policy in anything approaching human readable format requires a yaml file.
     * </p>
     * <p>
     * This pulls it from a known location. Usually you'd have a policy for each chaincode (and possibly version) but this suffices for an example.
     * </p>
     */
    private ChaincodeEndorsementPolicy createEndorsementPolicy() throws Exception {
        Path p = Paths.get(BASEPATH, POLICY_YAML);
        ChaincodeEndorsementPolicy ccep = new ChaincodeEndorsementPolicy();
        ccep.fromYamlFile(p.toFile());
        return ccep;
    }

    private static boolean checkUpgradeRequired(Channel channel, Peer peer, ChaincodeID ccid) throws InvalidArgumentException, ProposalException {
        System.out.println(String.format("Checking instantiated chaincode: %s on peer: %s to see if we need to upgrade", ccid.getName(), peer.getName()));
        return channel.queryInstantiatedChaincodes(peer).stream().filter(o -> ccid.getName().equals(o.getName())).findFirst().isPresent();
    }

    private static boolean checkInstalledChaincode(HFClient client, Peer peer, ChaincodeID ccid) throws InvalidArgumentException, ProposalException {
        System.out.println(String.format("Checking installed chaincode: %s, at version: %s, on peer: %s", ccid.getName(), ccid.getVersion(), peer.getName()));
        return matchChaincode(client.queryInstalledChaincodes(peer), ccid);
    }

    private static boolean checkInstantiatedChaincode(Channel channel, Peer peer, ChaincodeID ccid) throws InvalidArgumentException, ProposalException {
        System.out.println(String.format("Checking instantiated chaincode: %s, at version: %s, on peer: %s", ccid.getName(), ccid.getVersion(), peer.getName()));
        return matchChaincode(channel.queryInstantiatedChaincodes(peer), ccid);
    }

    private static boolean matchChaincode(Collection<ChaincodeInfo> infos, ChaincodeID ccid) throws InvalidArgumentException, ProposalException {
        return infos.stream().filter(o -> ccid.getName().equals(o.getName()) && ccid.getPath().equals(o.getPath()) && ccid.getVersion().equals(o.getVersion())).findFirst().isPresent();
    }

    /**
     * Invoke the Chaincode
     *
     * @param client
     * @param channel
     * @param id
     * @param action
     * @param parameters
     * @return
     * @throws Exception
     */
    public TransactionEvent invoke(HFClient client, Channel channel, ChaincodeID id, String action, ArrayList<String> parameters) throws Exception {
        List<ProposalResponse> successful = new LinkedList<>();
        List<ProposalResponse> failed = new LinkedList<>();

        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(id);
        transactionProposalRequest.setFcn(action);
        transactionProposalRequest.setProposalWaitTime(ait.getTestconfig().getProposalWaitTime());
        transactionProposalRequest.setArgs(parameters);

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put(HYPERLEDGERFABRIC, INVOKE_CHAINCODE_JAVA.getBytes(UTF_8));
        tm2.put(METHOD, INVOKE_CHAINCODE.getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8)); /// This should be returned see chaincode.
        transactionProposalRequest.setTransientMap(tm2);

        System.out.println(String.format("Sending transactionProposal to all peers with arguments: %s", transactionProposalRequest.getArgs().toString()));

        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());

        // Check that all the proposals are consistent with each other. We should have only one set where all the proposals above are consistent.
        Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
        if (proposalConsistencySets.size() != 1) {
            System.err.println(String.format("Expected only one set of consistent proposal responses but got %d", proposalConsistencySets.size()));
        }

        // Ok so it is a consistent set of responses. Now let us see if any passed
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                System.out.println(String.format("Successful transaction proposal response %s from %s", response.getTransactionID(), response.getPeer().getName()));
                successful.add(response);
            } else {
                System.err.println(String.format("Unsuccessful transaction proposal response %s with status %s from %s", response.getTransactionID(), response.getStatus().toString(),
                                response.getPeer().getName()));
                failed.add(response);
            }
        }

        System.out.println(String.format("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d", transactionPropResp.size(), successful.size(), failed.size()));
        if (failed.size() > 0) {
            ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
            String errorMessage = String.format("Not enough endorsers for invoke: %s. %d endorser(s) returned with an error: %s.", action, failed.size(),
                            firstTransactionProposalResponse.getMessage());
            System.err.println(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        System.out.println("Successfully received transaction proposal responses.");

        if (successful.size() == 0) {
            System.err.println("Successful response list is empty");
            throw new RuntimeException("No successful responses");
        }
        ProposalResponse resp = successful.get(0);
        if (resp.getChaincodeActionResponseStatus() != 200) {
            throw new RuntimeException(String.format("Chaincode status should be 200, instead it was %d", resp.getChaincodeActionResponseStatus()));
        }

        TxReadWriteSetInfo readWriteSetInfo = resp.getChaincodeActionResponseReadWriteSetInfo();
        if (readWriteSetInfo == null) {
            throw new RuntimeException("Missing chaincode RW set");
        }
        if (readWriteSetInfo.getNsRwsetCount() == 0) {
            throw new RuntimeException("Empty chaincode RW set");
        }

        ChaincodeID cid = resp.getChaincodeID();
        if (cid == null) {
            throw new RuntimeException("No chaincode id returned");
        }

        if (cid.getPath().equals(id.getPath()) && cid.getName().equals(id.getName()) && cid.getVersion().equals(id.getVersion())) {
            System.out.println("Sending transaction to orderer with wait time " + ait.getTestconfig().getTransactionWaitTime() + "s");
            TransactionEvent te = channel.sendTransaction(successful).get(ait.getTestconfig().getTransactionWaitTime(), TimeUnit.SECONDS);
            System.out.println("Send transaction returned");
            if (te != null) {
                System.out.println(String.format("Finished transaction %s. Status was %s", te.getTransactionID(), te.isValid() ? "SUCCESS" : "FAIL"));
                te.getValidationCode();
                return te;
            } else {
                System.err.println("NULL TE?");
            }
        } else {
            System.err.println("Chaincode ids don't match");
            throw new RuntimeException("Chaincode ids don't match");
        }
        return null;
    }

    /**
     * Query a fabric chaincode
     *
     * @param client
     * @param channel
     * @return
     */
    public String query(HFClient client, Channel channel, ChaincodeID id, String action, ArrayList<String> parameters) {
        String payload = "failure";
        String peers = channel.getPeers().stream().map(p -> p.getName()).collect(Collectors.joining(","));
        try {
            QueryByChaincodeRequest tpr = client.newQueryProposalRequest();
            tpr.setChaincodeID(id);
            tpr.setFcn(action);
            tpr.setProposalWaitTime(ait.getTestconfig().getProposalWaitTime());
            tpr.setArgs(parameters);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put(HYPERLEDGERFABRIC, QUERY_CHAINCODE_JAVA.getBytes(UTF_8));
            tm2.put(METHOD, QUERY_CHAINCODE.getBytes(UTF_8));
            tpr.setTransientMap(tm2);

            System.out.println(String.format("Sending query to peers %s with arguments: %s", peers, tpr.getArgs().toString()));

            Collection<ProposalResponse> queryProposals = channel.queryByChaincode(tpr, channel.getPeers());
            for (ProposalResponse proposalResponse : queryProposals) {
                if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                    String failMsg = String.format("Failed query proposal from peer %s. Status: %s. Messages: %s. Verified: %s", proposalResponse.getPeer().getName(),
                                    proposalResponse.getStatus().toString(), proposalResponse.getMessage(), proposalResponse.isVerified() ? "true" : "false");
                    System.out.println(failMsg);
                    return payload;
                } else {
                    payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                    System.out.println(String.format("Query payload of %s from peer %s returned %s", tpr.getArgs(), proposalResponse.getPeer().getName(), payload));
                }
            }
        } catch (Exception e) {
            System.err.println("Caught exception while running query");
            e.printStackTrace();
        }
        return payload;
    }

}
