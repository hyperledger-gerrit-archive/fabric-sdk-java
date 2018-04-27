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

package org.hyperledger.fabric.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import discovery.Protocol;
import gossip.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;

public class ServiceDiscovery {
    private static final Log logger = LogFactory.getLog(ServiceDiscovery.class);
    private static final Random random = new Random();

    public void fullDiscovery(Peer serviceDiscoveryPeer, TransactionContext transactionContext, String channelName, String chaincodeName) {
        try {

            final byte[] clientTLSCertificateDigest = serviceDiscoveryPeer.getClientTLSCertificateDigest();

            logger.info(format("Doing discovery on peer: %s", serviceDiscoveryPeer));

            if (null == clientTLSCertificateDigest) {
                throw new RuntimeException(format("Peer %s requires mutual tls for service discovery.", channelName));
            }

            ByteString clientIdent = transactionContext.getIdentity().toByteString();
            ByteString tlshash = ByteString.copyFrom(clientTLSCertificateDigest);
            Protocol.AuthInfo authentication = Protocol.AuthInfo.newBuilder().setClientIdentity(clientIdent).setClientTlsCertHash(tlshash).build();

            List<Protocol.Query> fq = new ArrayList<>(10);

            fq.add(discovery.Protocol.Query.newBuilder().setChannel(channelName).setConfigQuery(Protocol.ConfigQuery.newBuilder().build()).build());

            fq.add(discovery.Protocol.Query.newBuilder().setChannel(channelName).setPeerQuery(Protocol.PeerMembershipQuery.newBuilder().build()).build());

            List<Protocol.ChaincodeInterest> cinn = new ArrayList<>(10);
            Protocol.ChaincodeInterest cci = Protocol.ChaincodeInterest.newBuilder().addAllChaincodeNames(Arrays.asList(chaincodeName)).build();
            cinn.add(cci);
            Protocol.ChaincodeQuery chaincodeQuery = Protocol.ChaincodeQuery.newBuilder().addAllInterests(cinn).build();

            fq.add(discovery.Protocol.Query.newBuilder().setChannel(channelName).setCcQuery(chaincodeQuery).build());

            fq.add(discovery.Protocol.Query.newBuilder().setLocalPeers(Protocol.LocalPeerQuery.newBuilder().build()).build()); // needs no channel names.

            Protocol.Request request = Protocol.Request.newBuilder().addAllQueries(fq).setAuthentication(authentication).build();
            ByteString payloadBytes = request.toByteString();
            ByteString signatureBytes = transactionContext.signByteStrings(payloadBytes);
            Protocol.SignedRequest sr = Protocol.SignedRequest.newBuilder()
                    .setPayload(payloadBytes).setSignature(signatureBytes).build();

            final Protocol.Response response = serviceDiscoveryPeer.sendDiscoveryRequestAsync(sr).get(20, TimeUnit.SECONDS);
            final List<Protocol.QueryResult> resultsList = response.getResultsList();
            Protocol.QueryResult queryResult;
            queryResult = resultsList.get(0);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));
            }
            Protocol.ConfigResult configResult = queryResult.getConfigResult();

            queryResult = resultsList.get(1);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));

            }
            Set ordererEndpoints = new HashSet();
            Map<String, Protocol.Endpoints> orderersMap = configResult.getOrderersMap();
            for (Map.Entry<String, Protocol.Endpoints> i : orderersMap.entrySet()) {
                String key = i.getKey();
                Protocol.Endpoints value = i.getValue();
                for (Protocol.Endpoint l : value.getEndpointList()) {
                    String endpoint = l.getHost() + ":" + l.getPort();

                    ordererEndpoints.add(endpoint.trim().toLowerCase());

                }
            }

            Protocol.PeerMembershipResult membership = queryResult.getMembers();
            final Map<String, Protocol.Peers> peersByOrgMap = membership.getPeersByOrgMap();

            queryResult = resultsList.get(2);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));
            }

            Protocol.ChaincodeQueryResult ccQueryRes = queryResult.getCcQueryRes();
            if (ccQueryRes.getContentList().isEmpty()) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));
            }

            Map<String, SDChaindcode> chaindcodeMap = new HashMap<>();

            for (Protocol.EndorsementDescriptor es : ccQueryRes.getContentList()) {
                es.getChaincode();
                List<SDLayout> layouts = new LinkedList<>();
                for (Protocol.Layout layout : es.getLayoutsList()) {
                    Map<String, Integer> quantitiesByGroupMap = layout.getQuantitiesByGroupMap();
                    for (Map.Entry<String, Integer> qmap : quantitiesByGroupMap.entrySet()) {
                        final String key = qmap.getKey();
                        final int quantity = qmap.getValue();
                        Protocol.Peers peers = es.getEndorsersByGroupsMap().get(key);

                        List<SDEndorser> sdEndorsers = new LinkedList<>();

                        for (Protocol.Peer pp : peers.getPeersList()) {
                            SDEndorser ppp = new SDEndorser(pp);
                            ppp.getEndpoint();
                            ppp.getLedgerHeight();
                            sdEndorsers.add(ppp);

                        }
                        layouts.add(new SDLayout(quantity, sdEndorsers));
                    }
                }
                chaindcodeMap.put(es.getChaincode(), new SDChaindcode(es.getChaincode(), layouts));
            }

//            String cb = chaincodeBytes.toStringUtf8();
//            final String chaincode = content.getChaincode();
            //  Protocol.SDLayout layouts = content.getLayouts(0);

//            SDEndorser SDEndorser = new SDEndorser(peerRet);
//            long ledgerHeight = SDEndorser.getLedgerHeight();
//            String endpoint = SDEndorser.getEndpoint();

            queryResult = resultsList.get(3);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));

            }
            Protocol.PeerMembershipResult lmembership = queryResult.getMembers();

            System.err.println(response);

        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException(e);
        }

    }

    class SDNetwork {
        Set<String> peerEndpoints = Collections.emptySet();
        Set<String> ordererEndpoints = Collections.emptySet();

        Set<String> getOrdererEndpoints() {
            return ordererEndpoints;
        }

        Set<String> getPeerEndpoints() {

            return peerEndpoints;
        }

    }

    public SDNetwork networkDiscovery(Peer serviceDiscoveryPeer, TransactionContext transactionContext, String channelName) {
        try {

            SDNetwork ret = new SDNetwork();

            final byte[] clientTLSCertificateDigest = serviceDiscoveryPeer.getClientTLSCertificateDigest();

            logger.info(format("Doing discovery on peer: %s", serviceDiscoveryPeer));

            if (null == clientTLSCertificateDigest) {
                throw new RuntimeException(format("Peer %s requires mutual tls for service discovery.", channelName));
            }

            ByteString clientIdent = transactionContext.getIdentity().toByteString();
            ByteString tlshash = ByteString.copyFrom(clientTLSCertificateDigest);
            Protocol.AuthInfo authentication = Protocol.AuthInfo.newBuilder().setClientIdentity(clientIdent).setClientTlsCertHash(tlshash).build();

            List<Protocol.Query> fq = new ArrayList<>(2);

            fq.add(discovery.Protocol.Query.newBuilder().setChannel(channelName).setConfigQuery(Protocol.ConfigQuery.newBuilder().build()).build());

            fq.add(discovery.Protocol.Query.newBuilder().setChannel(channelName).setPeerQuery(Protocol.PeerMembershipQuery.newBuilder().build()).build());

            Protocol.Request request = Protocol.Request.newBuilder().addAllQueries(fq).setAuthentication(authentication).build();
            ByteString payloadBytes = request.toByteString();
            ByteString signatureBytes = transactionContext.signByteStrings(payloadBytes);
            Protocol.SignedRequest sr = Protocol.SignedRequest.newBuilder()
                    .setPayload(payloadBytes).setSignature(signatureBytes).build();

            final Protocol.Response response = serviceDiscoveryPeer.sendDiscoveryRequestAsync(sr).get(20, TimeUnit.SECONDS);
            final List<Protocol.QueryResult> resultsList = response.getResultsList();
            Protocol.QueryResult queryResult;

            queryResult = resultsList.get(0);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));
            }
            Protocol.ConfigResult configResult = queryResult.getConfigResult();

            Set<String> ordererEndpoints = new HashSet();
            Map<String, Protocol.Endpoints> orderersMap = configResult.getOrderersMap();
            for (Map.Entry<String, Protocol.Endpoints> i : orderersMap.entrySet()) {
                String key = i.getKey();
                Protocol.Endpoints value = i.getValue();
                for (Protocol.Endpoint l : value.getEndpointList()) {
                    String endpoint = l.getHost() + ":" + l.getPort();

                    ordererEndpoints.add(endpoint.trim().toLowerCase());

                }
            }
            ret.ordererEndpoints = ordererEndpoints;

            queryResult = resultsList.get(1);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));

            }

            Protocol.PeerMembershipResult membership = queryResult.getMembers();

            Set<String> peerEndpoints = new HashSet();
            for (Map.Entry<String, Protocol.Peers> peerses : membership.getPeersByOrgMap().entrySet()) {
                String org = peerses.getKey();
                Protocol.Peers peers = peerses.getValue();

                for (Protocol.Peer pp : peers.getPeersList()) {
                    SDEndorser ppp = new SDEndorser(pp);

                    peerEndpoints.add(ppp.getEndpoint());

                }

            }
            ret.peerEndpoints = peerEndpoints;

            return ret;

        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException(e);
        }

    }

    public SDChaindcode discoverEndorserEndpoints(Peer serviceDiscoveryPeer, TransactionContext transactionContext, String channelName, String chaincodeName) {
        try {

            final byte[] clientTLSCertificateDigest = serviceDiscoveryPeer.getClientTLSCertificateDigest();

            logger.info(format("Doing discovery on peer: %s", serviceDiscoveryPeer));

            if (null == clientTLSCertificateDigest) {
                throw new RuntimeException(format("Peer %s requires mutual tls for service discovery.", channelName));
            }

            ByteString clientIdent = transactionContext.getIdentity().toByteString();
            ByteString tlshash = ByteString.copyFrom(clientTLSCertificateDigest);
            Protocol.AuthInfo authentication = Protocol.AuthInfo.newBuilder().setClientIdentity(clientIdent).setClientTlsCertHash(tlshash).build();

            List<Protocol.Query> fq = new ArrayList<>(1);

            List<Protocol.ChaincodeInterest> cinn = new ArrayList<>(1);
            Protocol.ChaincodeInterest cci = Protocol.ChaincodeInterest.newBuilder().addAllChaincodeNames(Arrays.asList(chaincodeName)).build();
            cinn.add(cci);
            Protocol.ChaincodeQuery chaincodeQuery = Protocol.ChaincodeQuery.newBuilder().addAllInterests(cinn).build();

            fq.add(discovery.Protocol.Query.newBuilder().setChannel(channelName).setCcQuery(chaincodeQuery).build());

            Protocol.Request request = Protocol.Request.newBuilder().addAllQueries(fq).setAuthentication(authentication).build();
            ByteString payloadBytes = request.toByteString();
            ByteString signatureBytes = transactionContext.signByteStrings(payloadBytes);
            Protocol.SignedRequest sr = Protocol.SignedRequest.newBuilder()
                    .setPayload(payloadBytes).setSignature(signatureBytes).build();

            final Protocol.Response response = serviceDiscoveryPeer.sendDiscoveryRequestAsync(sr).get(20, TimeUnit.SECONDS);
            final List<Protocol.QueryResult> resultsList = response.getResultsList();
            Protocol.QueryResult queryResult;

            queryResult = resultsList.get(0);
            if (queryResult.getResultCase().getNumber() == Protocol.QueryResult.ERROR_FIELD_NUMBER) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));
            }

            if (queryResult.getResultCase().getNumber() != Protocol.QueryResult.CC_QUERY_RES_FIELD_NUMBER) {
                throw new RuntimeException(format("Error  expected chaincode endorsement query but go:t %s : ", queryResult.getResultCase().toString()));
            }

            Protocol.ChaincodeQueryResult ccQueryRes = queryResult.getCcQueryRes();
            if (ccQueryRes.getContentList().isEmpty()) {
                throw new RuntimeException(format("Error %s", queryResult.getError().getContent()));
            }

            Map<String, SDChaindcode> chaindcodeMap = new HashMap<>();

            for (Protocol.EndorsementDescriptor es : ccQueryRes.getContentList()) {
                es.getChaincode();
                List<SDLayout> layouts = new LinkedList<>();
                for (Protocol.Layout layout : es.getLayoutsList()) {
                    Map<String, Integer> quantitiesByGroupMap = layout.getQuantitiesByGroupMap();
                    for (Map.Entry<String, Integer> qmap : quantitiesByGroupMap.entrySet()) {
                        final String key = qmap.getKey();
                        final int quantity = qmap.getValue();
                        Protocol.Peers peers = es.getEndorsersByGroupsMap().get(key);

                        List<SDEndorser> sdEndorsers = new LinkedList<>();

                        for (Protocol.Peer pp : peers.getPeersList()) {
                            SDEndorser ppp = new SDEndorser(pp);
                            sdEndorsers.add(ppp);

                        }
                        layouts.add(new SDLayout(quantity, sdEndorsers));
                    }
                }
                chaindcodeMap.put(es.getChaincode(), new SDChaindcode(es.getChaincode(), layouts));
            }

            SDChaindcode sdChaindcode = chaindcodeMap.get(chaincodeName);
            return sdChaindcode;

        } catch (Exception e) {
            logger.error(e);
            throw new RuntimeException(e);
        }

    }

    public interface EndorsementSelection {
        String[] endorserSelector(SDChaindcode sdChaindcode);
    }

    /**
     * Endorsement selection by layout group that has least required and block height is the highest (most up to date).
     */

    public static final EndorsementSelection ENDORSEMENT_SELECTION_LEAST_REQUIRED_BLOCKHEIGHT = sdChaindcode -> {
        List<SDLayout> layouts = sdChaindcode.getLayouts();

        SDLayout pickedLayout = layouts.get(0);

        if (layouts.size() > 1) { // pick layout by least number of endorsers ..  least number of peers hit and smaller block!

            ArrayList<SDLayout> leastEndorsers = new ArrayList<>();
            for (SDLayout sdLayout : layouts) {

                if (leastEndorsers.size() == 0 || leastEndorsers.get(0).getRequired() > sdLayout.getRequired()) {
                    leastEndorsers = new ArrayList<>();
                    leastEndorsers.add(sdLayout);
                } else if (leastEndorsers.get(0).getRequired() == sdLayout.getRequired()) {
                    leastEndorsers.add(sdLayout);

                }
            }
            if (leastEndorsers.size() == 1) {
                pickedLayout = leastEndorsers.get(0);
            } else {
                long maxHeight = -1L; // go with the highest total block height of the required.

                for (SDLayout layout : leastEndorsers) { // means required was the same.
                    List<SDEndorser> sdEndorsers = topNbyHeight(layout.getRequired(), layout.getSDEndorsers());
                    long score = 0;
                    for (SDEndorser sdEndorser : sdEndorsers) {
                        score += sdEndorser.getLedgerHeight();
                    }
                    if (score > maxHeight) {
                        maxHeight = score;
                        pickedLayout = layout;
                    }
                }
            }
        }

        List<SDEndorser> top = topNbyHeight(pickedLayout.getRequired(), pickedLayout.getSDEndorsers());
        ArrayList<String> retlist = new ArrayList<>(pickedLayout.getRequired());
        top.forEach(sdEndorser -> retlist.add(sdEndorser.getEndpoint()));

        return retlist.toArray(new String[retlist.size()]);
    };

    public static final EndorsementSelection DEFAULT_ENDORSEMENT_SELECTION = ENDORSEMENT_SELECTION_LEAST_REQUIRED_BLOCKHEIGHT;

    /**
     * Endorsement selection by random layout group and random endorsers there in.
     */
    public static final EndorsementSelection ENDORSEMENT_SELECTION_RANDOM = sdChaindcode -> {
        List<SDLayout> layouts = sdChaindcode.getLayouts();

        SDLayout pickedLayout = layouts.get(0);

        if (layouts.size() > 1) {
            pickedLayout = layouts.get(random.nextInt(layouts.size()));
        }

        List<SDEndorser> pickedEndorsers = pickedLayout.getSDEndorsers();
        final int required = pickedLayout.getRequired();

        if (required != pickedEndorsers.size()) {
            List<SDEndorser> shuffle = new ArrayList<>(pickedEndorsers);
            Collections.shuffle(shuffle);
            pickedEndorsers = shuffle.subList(0, required);
        }
        List<String> retlist = new ArrayList<>(required);
        pickedEndorsers.forEach(sdEndorser -> retlist.add(sdEndorser.getEndpoint()));

        return retlist.toArray(new String[retlist.size()]);
    };

    public static class SDChaindcode {
        String name;

        public List<SDLayout> getLayouts() {
            return layouts;
        }

        List<SDLayout> layouts;

        SDChaindcode(String name, List<SDLayout> layouts) {
            this.name = name;
            this.layouts = layouts;
        }
    }

    public static class SDLayout {

        final List<SDEndorser> SDEndorsers;
        final int required;

        SDLayout(int quantity, List<SDEndorser> protocolPeers) {
            required = quantity;
            this.SDEndorsers = protocolPeers;
        }

        public List<SDEndorser> getSDEndorsers() {
            return SDEndorsers;
        }

        public int getRequired() {
            return required;
        }
    }

    public static class SDEndorser {

        private final Protocol.Peer proto;
        private String endPoint = null;
        private long ledgerHeight = -1L;

        SDEndorser(Protocol.Peer peerRet) {
            proto = peerRet;

            getEndpoint();
            getLedgerHeight();
        }

        String getEndpoint() throws InvalidProtocolBufferRuntimeException {

            if (null == endPoint) {
                try {
                    Message.Envelope membershipInfo = proto.getMembershipInfo();
                    final ByteString membershipInfoPayloadBytes = membershipInfo.getPayload();
                    final Message.GossipMessage gossipMessageMemberInfo = Message.GossipMessage.parseFrom(membershipInfoPayloadBytes);

                    if (Message.GossipMessage.ContentCase.ALIVE_MSG.getNumber() != gossipMessageMemberInfo.getContentCase().getNumber()) {
                        throw new RuntimeException(format("Error %s", "bad"));
                    }
                    Message.AliveMessage aliveMsg = gossipMessageMemberInfo.getAliveMsg();
                    endPoint = aliveMsg.getMembership().getEndpoint();
                    if (endPoint != null) {
                        endPoint = endPoint.toLowerCase().trim(); //makes easier on comparing.
                    }
                } catch (InvalidProtocolBufferException e) {
                    throw new InvalidProtocolBufferRuntimeException(e);
                }

            }
            return endPoint;

        }

        long getLedgerHeight() throws InvalidProtocolBufferRuntimeException {

            if (-1L == ledgerHeight) {
                try {
                    Message.Envelope stateInfo = proto.getStateInfo();
                    final Message.GossipMessage stateInfoGossipMessage = Message.GossipMessage.parseFrom(stateInfo.getPayload());
                    Message.GossipMessage.ContentCase contentCase = stateInfoGossipMessage.getContentCase();
                    if (contentCase.getNumber() != Message.GossipMessage.ContentCase.STATE_INFO.getNumber()) {
                        throw new RuntimeException("" + contentCase.getNumber());
                    }
                    Message.StateInfo stateInfo1 = stateInfoGossipMessage.getStateInfo();
                    ledgerHeight = stateInfo1.getProperties().getLedgerHeight();
                } catch (InvalidProtocolBufferException e) {
                    throw new InvalidProtocolBufferRuntimeException(e);
                }
            }

            return ledgerHeight;

        }
    }

    static List<SDEndorser> topNbyHeight(int required, List<SDEndorser> endorsers) {
        ArrayList<SDEndorser> ret = new ArrayList<>(endorsers);
        Collections.sort(ret, Comparator.comparingLong(SDEndorser::getLedgerHeight));
        return ret.subList(Math.max(ret.size() - required, 0), ret.size());
    }

}
