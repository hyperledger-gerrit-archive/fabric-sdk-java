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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.ChannelGrpc;
import org.hyperledger.fabric.protos.peer.PeerEvents;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.EventHubException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;

/**
 * Class to manage fabric events.
 * <p>
 * Feeds Channel event queues with events
 */

public class PeerEventingClient implements Serializable {

    private static final Log logger = LogFactory.getLog(PeerEventingClient.class);
    private static final Config config = Config.getConfig();
    private static final long EVENTHUB_CONNECTION_WAIT_TIME = config.getEventHubConnectionWaitTime();
    private static final long serialVersionUID = 8351783858482978694L;

    private final String url;
    private final String name;
    private final Properties properties;
    private final Peer peer;
    final PeerEvents.Interest interest;
    private transient ManagedChannel managedChannel;
    private transient boolean connected = false;
    private transient ChannelGrpc.ChannelStub events;
    private transient StreamObserver<PeerEvents.SignedEvent> sender;
    /**
     * Event queue for all events from eventhubs in the channel
     */
    //   transient Channel.ChannelEventQue eventQue;
    private transient long connectedTime = 0L; // 0 := never connected
    private transient boolean shutdown = false;
    //   private Channel channel;
    private transient TransactionContext transactionContext;

    /**
     * Get disconnected time.
     *
     * @return Time in milli seconds disconnect occurred. Zero if never disconnected
     */
    public long getDisconnectedTime() {
        return disconnectedTime;
    }

    private long disconnectedTime;

    /**
     * Is event hub connected.
     *
     * @return boolean if true event hub is connected.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Get last connect time.
     *
     * @return Time in milli seconds the event hub last connected. Zero if never connected.
     */
    public long getConnectedTime() {
        return connectedTime;
    }

    /**
     * Get last attempt time to connect the event hub.
     *
     * @return Last attempt time to connect the event hub in milli seconds. Zero when never attempted.
     */

    public long getLastConnectedAttempt() {
        return lastConnectedAttempt;
    }

    private long lastConnectedAttempt;

    private Set<String> channelNames = new HashSet<>();

    PeerEventingClient(Peer peer, Set<Channel> channels) throws InvalidArgumentException {

        // Exception e = checkGrpcUrl(grpcURL);
//        if (e != null) {
//            throw new InvalidArgumentException("Bad event hub url.", e);
//
//        }
//
//        if (StringUtil.isNullOrEmpty(name)) {
//            throw new InvalidArgumentException("Invalid name for eventHub");
//        }

        this.url = peer.getUrl();
        this.peer = peer;
        this.name = peer.getName();
        this.properties = peer.getProperties() == null ? null : (Properties) peer.getProperties().clone(); //keep our own copy.
        for (Channel channel : channels) {
            channelNames.add(channel.getName());
        }

        PeerEvents.EventType blockPreference = PeerEvents.EventType.BLOCK;

        if (null != properties) {

            String interest = properties.getProperty(Peer.PEER_BASE_PROPERTY_NAME + "eventinterests");
            interest = interest == null ? null : interest.trim();
            if (interest != null && "FILTEREDBLOCK".equals(interest)) {
                blockPreference = PeerEvents.EventType.FILTEREDBLOCK;
            }

        }

        interest = PeerEvents.Interest.newBuilder().setEventType(blockPreference).build();

    }

    /**
     * Create a new instance.
     *
     * @param name
     * @param url
     * @param properties
     * @return
     */

//    static PeerEventingClient createNewInstance(String name, String url, ExecutorService client, Properties properties) throws InvalidArgumentException {
//        return new PeerEventingClient(name, url, client, properties);
//    }

    /**
     * Event hub name
     *
     * @return event hub name
     */

    public String getName() {
        return name;
    }

    /**
     * Event hub properties
     *
     * @return Event hub properties
     * @see HFClient#newEventHub(String, String, Properties)
     */
    public Properties getProperties() {
        return properties == null ? null : (Properties) properties.clone();
    }

    boolean connect() throws EventHubException {

        if (this.transactionContext == null) {
            throw new EventHubException("Eventhub reconnect failed with no user context");
        }

        return connect(this.transactionContext);

    }

    private transient StreamObserver<PeerEvents.Event> eventStream = null; // Saved here to avoid potential garbage collection

    synchronized boolean connect(final TransactionContext transactionContext) throws EventHubException {
        if (connected) {
            logger.warn(format("%s already connected.", toString()));
            return true;
        }
        eventStream = null;
        final CountDownLatch finishLatch = new CountDownLatch(1);
        logger.debug(format("EventHub %s is connecting.", name));
        lastConnectedAttempt = System.currentTimeMillis();
        managedChannel = new Endpoint(url, properties).getChannelBuilder().build();
        ChannelGrpc.newStub(managedChannel);

        events = ChannelGrpc.newStub(managedChannel);
        final ArrayList<Throwable> threw = new ArrayList<>();

        final StreamObserver<PeerEvents.Event> eventStreamLocal = new StreamObserver<PeerEvents.Event>() {
            @Override
            public void onNext(PeerEvents.Event event) {

                logger.debug(format("EventHub %s got  event type: %s", PeerEventingClient.this.name, event.getEventCase().name()));

                if (event.getEventCase() == PeerEvents.Event.EventCase.BLOCK || event.getEventCase() == PeerEvents.Event.EventCase.FILTERED_BLOCK) {
                    try {
                        peer.getChannel().getChannelEventQue().addBEvent(new BlockEvent(peer, event));  //add to channel queue
                    } catch (InvalidProtocolBufferException e) {
                        EventHubException eventHubException = new EventHubException(format("%s onNext error %s", this, e.getMessage()), e);
                        logger.error(eventHubException.getMessage());
                        threw.add(eventHubException);
                    }
//                }
//                if (event.getEventCase() == PeerEvents.Event.EventCase.FILTERED_BLOCK) {
//
//                    final PeerEvents.FilteredBlock filteredBlock = event.getFilteredBlock();
//                    final String channelId = filteredBlock.getChannelId();
//                    final long number = filteredBlock.getNumber();
//                    filteredBlock.getFilteredTxCount();
//
//                    for (PeerEvents.FilteredTransaction transaction : filteredBlock.getFilteredTxList()) {
//                        final ChaincodeEventOuterClass.ChaincodeEvent ccEvent = transaction.getCcEvent();
//
//                        final String txid = transaction.getTxid();
//                        System.err.println("txid:" + txid);
//                        final FabricTransaction.TxValidationCode txValidationCode = transaction.getTxValidationCode();
//
//                        if (transaction.hasCcEvent()) {
//                            System.err.println("hasCC");
//                            System.err.println("ccEvent:" + ccEvent);
//                            final String txIdcc = ccEvent.getTxId();
//                            System.err.println("txIdcc:" + txIdcc);
//
//                            final String chaincodeId = ccEvent.getChaincodeId();
//                            System.err.println("chaincodeId:" + chaincodeId);
//                        }
//
//                    }

                } else if (event.getEventCase() == PeerEvents.Event.EventCase.CHANNEL_SERVICE_RESPONSE) {

                    PeerEvents.ChannelServiceResponse channelServiceResponse = event.getChannelServiceResponse();

                    if (channelServiceResponse.getSuccess()) { // all or nothing.
                        connected = true;
                        connectedTime = System.currentTimeMillis();
                    } else {
                        StringBuilder sb = new StringBuilder();
                        String sep = "";

                        for (PeerEvents.ChannelServiceResult result : channelServiceResponse.getChannelServiceResultsList()) {
                            sb.append(sep).append("Error for registering channel ").append(result.getChannelId()).append(" ").append(result.getErrorMsg());
                            sep = "\n";
                        }
                        threw.add(new EventHubException(sb.toString()));
                    }
                    finishLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable t) {
                if (shutdown) { //IF we're shutdown don't try anything more.
                    logger.trace(format("%s was shutdown.", PeerEventingClient.this.toString()));
                    connected = false;
                    eventStream = null;
                    finishLatch.countDown();
                    return;
                }

                final boolean isTerminated = managedChannel.isTerminated();
                final boolean isChannelShutdown = managedChannel.isShutdown();

                logger.error(format("%s terminated is %b shutdown is %b has error %s ", PeerEventingClient.this.toString(), isTerminated, isChannelShutdown,
                        t.getMessage()), new EventHubException(t));
                threw.add(t);
                finishLatch.countDown();

                //              logger.error("Error in stream: " + t.getMessage(), new EventHubException(t));
                if (t instanceof StatusRuntimeException) {
                    StatusRuntimeException sre = (StatusRuntimeException) t;
                    Status sreStatus = sre.getStatus();
                    logger.error(format("%s :StatusRuntimeException Status %s.  Description %s ", PeerEventingClient.this, sreStatus + "", sreStatus.getDescription()));
                    if (sre.getStatus().getCode() == Status.Code.INTERNAL || sre.getStatus().getCode() == Status.Code.UNAVAILABLE) {

                        connected = false;
                        eventStream = null;
                        disconnectedTime = System.currentTimeMillis();
                        try {
                            if (!isChannelShutdown) {
                                managedChannel.shutdownNow();
                            }
                            if (null != disconnectedHandler) {
                                try {
                                    disconnectedHandler.disconnected(PeerEventingClient.this);
                                } catch (Exception e) {
                                    logger.warn(format("Peer %s  %s", PeerEventingClient.this.name, e.getMessage()), e);
                                    peer.getChannel().getChannelEventQue().eventError(e);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn(format("Peer %s Failed shutdown msg:  %s", PeerEventingClient.this.name, e.getMessage()), e);
                        }
                    }
                }

            }

            @Override
            public void onCompleted() {
                logger.warn(format("Stream completed %s", PeerEventingClient.this.toString()));
                finishLatch.countDown();
            }
        };

        sender = events.chat(eventStreamLocal);
        try {
            blockListen(transactionContext);
        } catch (CryptoException e) {
            throw new EventHubException(e);
        }

        try {
            if (!finishLatch.await(EVENTHUB_CONNECTION_WAIT_TIME, TimeUnit.MILLISECONDS)) {
                EventHubException evh = new EventHubException(format("EventHub %s failed to connect in %s ms.", name, EVENTHUB_CONNECTION_WAIT_TIME));
                logger.debug(evh.getMessage(), evh);

                throw evh;
            }
            logger.trace(format("Peer eventing %s Done waiting for reply!", name));

        } catch (InterruptedException e) {
            logger.error(e);
        }

        if (!threw.isEmpty()) {
            eventStream = null;
            connected = false;
            Throwable t = threw.iterator().next();

            EventHubException
                    evh = new EventHubException(t.getMessage(), t);

            logger.error(format("Peer %s eventing Error in stream. error: " + t.getMessage(), toString()), evh);
            throw evh;
        }
        logger.debug(format("Peer %s eventing connect is done with connect status: %b ", name, connected));

        if (connected) {
            eventStream = eventStreamLocal;
        }

        return connected;

    }

    private void blockListen(TransactionContext transactionContext) throws CryptoException {

        this.transactionContext = transactionContext;

        PeerEvents.RegisterChannel register = PeerEvents.RegisterChannel.newBuilder()
                .addAllChannelIds(channelNames)
                .addEvents(interest).build();

        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            String sep = "";
            for (String cn : channelNames) {
                sb.append(sep).append(cn);
                sep = ", ";

            }
            logger.debug(format("Peer %s eventing service registering block interest type: %s for channels: %s", name, interest.getEventType().name(), sb.toString()));
        }

        ByteString blockEventByteString = PeerEvents.Event.newBuilder().setRegisterChannel(register)
                .setCreator(transactionContext.getIdentity().toByteString())
                .build().toByteString();
        PeerEvents.SignedEvent signedBlockEvent = PeerEvents.SignedEvent.newBuilder()
                .setEventBytes(blockEventByteString)
                .setSignature(transactionContext.signByteString(blockEventByteString.toByteArray()))
                .build();
        sender.onNext(signedBlockEvent);
    }

    /**
     * Get the GRPC URL used to connect.
     *
     * @return GRPC URL.
     */
    public String getUrl() {
        return url;
    }

//    @Override
//    public String toString() {
//        return "Peer Eventing Client:" + getName();
//    }

    public void shutdown() {
        shutdown = true;
        connected = false;
        disconnectedHandler = null;
        //channel = null;
        eventStream = null;
        managedChannel.shutdownNow();
    }

//    void setChannel(Channel channel) throws InvalidArgumentException {
//        if (channel == null) {
//            throw new InvalidArgumentException("setChannel Channel can not be null");
//        }
//
//        if (null != this.channel) {
//            throw new InvalidArgumentException(format("Can not add event hub  %s to channel %s because it already belongs to channel %s.",
//                    name, channel.getName(), this.channel.getName()));
//        }
//
//        this.channel = channel;
//    }

    /**
     * Eventhub disconnection notification interface
     */
    public interface EventHubDisconnected {

        /**
         * Called when a disconnect is detected.
         *
         * @param eventHub
         * @throws EventHubException
         */
        void disconnected(PeerEventingClient eventHub) throws EventHubException;

    }

    /**
     * Default reconnect event hub implementation.  Applications are free to replace
     */

    protected transient EventHubDisconnected disconnectedHandler = new PeerEventingClient.EventHubDisconnected() {
        @Override
        public synchronized void disconnected(final PeerEventingClient eventHub) throws EventHubException {
            logger.info(format("Detected disconnect %s", eventHub.toString()));

            if (eventHub.connectedTime == 0) { //means event hub never connected
                logger.error(format("%s failed on first connect no retries", eventHub.toString()));

                eventHub.setEventHubDisconnectedHandler(null); //don't try again

                //event hub never connected.
                throw new EventHubException(format("%s never connected.", eventHub.toString()));
            }

            peer.getExecutorService().execute(() -> {

                try {
                    Thread.sleep(500);

                    if (eventHub.connect()) {
                        logger.info(format("Successful reconnect %s", eventHub.toString()));
                    } else {
                        logger.info(format("Failed reconnect %s", eventHub.toString()));
                    }

                } catch (Exception e) {

                    logger.debug(format("Failed %s to reconnect. %s", toString(), e.getMessage()));

                }

            });

        }
    };

    /**
     * Set class to handle Event hub disconnects
     *
     * @param newEventHubDisconnectedHandler New handler to replace.  If set to null no retry will take place.
     * @return the old handler.
     */

    public EventHubDisconnected setEventHubDisconnectedHandler(EventHubDisconnected newEventHubDisconnectedHandler) {
        EventHubDisconnected ret = disconnectedHandler;
        disconnectedHandler = newEventHubDisconnectedHandler;
        return ret;
    }

}
