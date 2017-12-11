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

package org.hyperledger.fabric.sdk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.internal.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.common.Common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Common.Envelope;
import org.hyperledger.fabric.protos.common.Common.Payload;
import org.hyperledger.fabric.protos.common.Common.SignatureHeader;
import org.hyperledger.fabric.protos.orderer.Ab;
import org.hyperledger.fabric.protos.orderer.Ab.DeliverResponse;
import org.hyperledger.fabric.protos.orderer.Ab.SeekInfo;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.PeerException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.helper.Utils.checkGrpcUrl;
import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.createChannelHeader;
import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.getCurrentFabricTimestamp;

/**
 * The Peer class represents a peer to which SDK sends deploy, or query proposals requests.
 */
public class Peer implements Serializable {

    private static final Log logger = LogFactory.getLog(Peer.class);
    private static final long serialVersionUID = -5273194649991828876L;
    private transient volatile EndorserClient endorserClent;
    private final Properties properties;
    private final String name;
    private final String url;
    private transient boolean shutdown = false;
    private Channel channel;

    Peer(String name, String grpcURL, Properties properties) throws InvalidArgumentException {

        Exception e = checkGrpcUrl(grpcURL);
        if (e != null) {
            throw new InvalidArgumentException("Bad peer url.", e);

        }

        if (StringUtil.isNullOrEmpty(name)) {
            throw new InvalidArgumentException("Invalid name for peer");
        }

        this.url = grpcURL;
        this.name = name;
        this.properties = properties == null ? null : (Properties) properties.clone(); //keep our own copy.

    }

    /**
     * Peer's name
     *
     * @return return the peer's name.
     */

    public String getName() {

        return name;
    }

    public Properties getProperties() {

        return properties == null ? null : (Properties) properties.clone();
    }

    /**
     * Set the channel the peer is on.
     *
     * @param channel
     */

    void setChannel(Channel channel) throws InvalidArgumentException {

        if (null != this.channel) {
            throw new InvalidArgumentException(format("Can not add peer %s to channel %s because it already belongs to channel %s.",
                    name, channel.getName(), this.channel.getName()));
        }

        this.channel = channel;

    }

    void unsetChannel() {
        channel = null;

    }

    /**
     * The channel the peer is set on.
     *
     * @return
     */

    Channel getChannel() {

        return channel;

    }

    /**
     * Get the URL of the peer.
     *
     * @return {string} Get the URL associated with the peer.
     */
    public String getUrl() {

        return this.url;
    }

    /**
     * for use in list of peers comparisons , e.g. list.contains() calls
     *
     * @param otherPeer the peer instance to compare against
     * @return true if both peer instances have the same name and url
     */
    @Override
    public boolean equals(Object otherPeer) {
        if (this == otherPeer) {
            return true;
        }
        if (otherPeer == null) {
            return false;
        }
        if (!(otherPeer instanceof Peer)) {
            return false;
        }
        Peer p = (Peer) otherPeer;
        return Objects.equals(this.name, p.name) && Objects.equals(this.url, p.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }

    ListenableFuture<FabricProposalResponse.ProposalResponse> sendProposalAsync(FabricProposal.SignedProposal proposal)
            throws PeerException, InvalidArgumentException {
        checkSendProposal(proposal);

        logger.debug(format("peer.sendProposalAsync name: %s, url: %s", name, url));

        EndorserClient localEndorserClient = endorserClent; //work off thread local copy.

        if (null == localEndorserClient || !localEndorserClient.isChannelActive()) {
            endorserClent = new EndorserClient(new Endpoint(url, properties).getChannelBuilder());
            localEndorserClient = endorserClent;
        }

        try {
            return localEndorserClient.sendProposalAsync(proposal);
        } catch (Throwable t) {
            endorserClent = null;
            throw t;
        }
    }

    FabricProposalResponse.ProposalResponse sendProposal(FabricProposal.SignedProposal proposal)
            throws PeerException, InvalidArgumentException {
        checkSendProposal(proposal);

        logger.debug(format("peer.sendProposalAsync name: %s, url: %s", name, url));

        EndorserClient localEndorserClient = endorserClent; //work off thread local copy.

        if (null == localEndorserClient || !localEndorserClient.isChannelActive()) {
            endorserClent = new EndorserClient(new Endpoint(url, properties).getChannelBuilder());
            localEndorserClient = endorserClent;
        }

        try {
            return localEndorserClient.sendProposal(proposal);
        } catch (Throwable t) {
            endorserClent = null;
            throw t;
        }
    }

    private void checkSendProposal(FabricProposal.SignedProposal proposal) throws PeerException, InvalidArgumentException {

        if (shutdown) {
            throw new PeerException(format("Peer %s was shutdown.", name));
        }
        if (proposal == null) {
            throw new PeerException("Proposal is null");
        }
        Exception e = checkGrpcUrl(url);
        if (e != null) {
            throw new InvalidArgumentException("Bad peer url.", e);

        }
    }

    static Peer createNewInstance(String name, String grpcURL, Properties properties) throws InvalidArgumentException {

        return new Peer(name, grpcURL, properties);
    }

    synchronized void shutdown(boolean force) {
        if (shutdown) {
            return;
        }
        shutdown = true;
        channel = null;

        EndorserClient lendorserClent = endorserClent;

        //allow resources to finalize

        endorserClent = null;

        if (lendorserClent == null) {
            return;
        }

        lendorserClent.shutdown(force);
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown(true);
        super.finalize();
    }

    //=========================================================
    // Peer eventing
    void peerVent(TransactionContext transactionContext) throws TransactionException {

        final PeerEventServiceClient peerEventServiceClient = new PeerEventServiceClient(this,
                new Endpoint(url, properties).getChannelBuilder(), properties);

        getLatestBlock(peerEventServiceClient, transactionContext);

    }

    private void getLatestBlock(PeerEventServiceClient orderer, TransactionContext transactionContext) throws TransactionException {

        logger.debug(format("getConfigurationBlock for channel %s", name));

        Ab.SeekPosition seekPosition = Ab.SeekPosition.newBuilder()
                .setNewest(Ab.SeekNewest.getDefaultInstance())
                .build();

        Ab.SeekPosition wayout = Ab.SeekPosition.newBuilder()
                .setSpecified(Ab.SeekSpecified.newBuilder().setNumber(Long.MAX_VALUE).build())
                .build();

        SeekInfo seekInfo = SeekInfo.newBuilder()
                .setStart(seekPosition)
                .setStop(wayout)
                .setBehavior(SeekInfo.SeekBehavior.BLOCK_UNTIL_READY)
                .build();

        ArrayList<DeliverResponse> deliverResponses = new ArrayList<>();

        seekBlock(transactionContext, seekInfo, deliverResponses, orderer);

//        DeliverResponse blockresp = deliverResponses.get(1);
//
//        Block latestBlock = blockresp.getBlock();
//
//        if (latestBlock == null) {
//            throw new TransactionException(format("newest block for channel %s fetch bad deliver returned null:", name));
//        }
//
//        logger.trace(format("Received latest  block for channel %s, block no:%d", name, latestBlock.getHeader().getNumber()));
//        return latestBlock;
    }

    int seekBlock(TransactionContext transactionContext, SeekInfo seekInfo, List<DeliverResponse> deliverResponses, PeerEventServiceClient peerEventServiceClient) throws TransactionException {

        logger.trace(format("seekBlock for channel %s", name));
        final long start = System.currentTimeMillis();
        @SuppressWarnings ("UnusedAssignment")
        int statusRC = 404;

        try {

            //    do {

            statusRC = 404;

            //        final PeerEventServiceClient peerEventServiceClient = ordererIn;

            //  TransactionContext transactionContext = channel.getTransactionContext();

            ChannelHeader seekInfoHeader = createChannelHeader(Common.HeaderType.DELIVER_SEEK_INFO,
                    transactionContext.getTxID(), channel.getName(), transactionContext.getEpoch(), getCurrentFabricTimestamp(), null);

            SignatureHeader signatureHeader = SignatureHeader.newBuilder()
                    .setCreator(transactionContext.getIdentity().toByteString())
                    .setNonce(transactionContext.getNonce())
                    .build();

            Common.Header seekHeader = Common.Header.newBuilder()
                    .setSignatureHeader(signatureHeader.toByteString())
                    .setChannelHeader(seekInfoHeader.toByteString())
                    .build();

            Payload seekPayload = Payload.newBuilder()
                    .setHeader(seekHeader)
                    .setData(seekInfo.toByteString())
                    .build();

            Envelope envelope = Envelope.newBuilder().setSignature(transactionContext.signByteString(seekPayload.toByteArray()))
                    .setPayload(seekPayload.toByteString())
                    .build();

            DeliverResponse[] deliver = peerEventServiceClient.connect(envelope);

//            if (deliver.length < 1) {
//                logger.warn(format("Genesis block for channel %s fetch bad deliver missing status block only got blocks:%d", name, deliver.length));
//                //odd so lets try again....
//                statusRC = 404;
//
//            } else {
//
//                DeliverResponse status = deliver[0];
//                statusRC = status.getStatusValue();
//
//                if (statusRC == 404 || statusRC == 503) { //404 - block not found.  503 - service not available usually means kafka is not ready but starting.
//                    logger.warn(format("Bad deliver expected status 200  got  %d, Channel %s", status.getStatusValue(), name));
//                    // keep trying... else
//                    statusRC = 404;
//
//                } else if (statusRC != 200) { // Assume for anything other than 200 we have a non retryable situation
//                    throw new TransactionException(format("Bad newest block expected status 200  got  %d, Channel %s", status.getStatusValue(), name));
//                } else {
//                    if (deliver.length < 2) {
//                        throw new TransactionException(format("Newest block for channel %s fetch bad deliver missing genesis block only got %d:", name, deliver.length));
//                    } else {
//
//                        deliverResponses.addAll(Arrays.asList(deliver));
//                    }
//                }
//
//            }
//
//            // Not 200 so sleep to try again
//
//            if (200 != statusRC) {
//                long duration = System.currentTimeMillis() - start;
//
//                if (duration > 90000000) {
//                    throw new TransactionException(format("Getting block time exceeded %s seconds for channel %s", Long.toString(TimeUnit.MILLISECONDS.toSeconds(duration)), name));
//                }
//                try {
//                    Thread.sleep(1000); //try again
//                } catch (InterruptedException e) {
//                    TransactionException te = new TransactionException("seekBlock thread Sleep", e);
//                    logger.warn(te.getMessage(), te);
//                }
//            }

            //     } while (statusRC != 200);

        } catch (TransactionException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TransactionException(e);
        }

        return statusRC;

    }

} // end Peer
