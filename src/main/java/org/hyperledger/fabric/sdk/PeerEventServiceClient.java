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
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.orderer.Ab.DeliverResponse;
import org.hyperledger.fabric.protos.orderer.AtomicBroadcastGrpc;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.helper.Config;

import static java.lang.String.format;
import static org.hyperledger.fabric.protos.orderer.Ab.DeliverResponse.TypeCase.STATUS;

/**
 * Sample client code that makes gRPC calls to the server.
 */
class PeerEventServiceClient {
    private static final Config config = Config.getConfig();
    private static final long ORDERER_WAIT_TIME = config.getOrdererWaitTime();
    private final String channelName;
    private final ManagedChannelBuilder channelBuilder;
    private boolean shutdown = false;
    private static final Log logger = LogFactory.getLog(PeerEventServiceClient.class);
    private ManagedChannel managedChannel = null;
    private final String name;
    private final String url;
    private final long ordererWaitTimeMilliSecs;

    /**
     * Construct client for accessing Orderer server using the existing managedChannel.
     */
    PeerEventServiceClient(Peer peer, ManagedChannelBuilder<?> channelBuilder, Properties properties) {

        this.channelBuilder = channelBuilder;
        name = peer.getName();
        url = peer.getUrl();
        channelName = peer.getChannel().getName();

        if (null == properties) {

            ordererWaitTimeMilliSecs = ORDERER_WAIT_TIME;

        } else {

            String ordererWaitTimeMilliSecsString = properties.getProperty("ordererWaitTimeMilliSecs", Long.toString(ORDERER_WAIT_TIME));

            long tempOrdererWaitTimeMilliSecs = ORDERER_WAIT_TIME;

            try {
                tempOrdererWaitTimeMilliSecs = Long.parseLong(ordererWaitTimeMilliSecsString);
            } catch (NumberFormatException e) {
                logger.warn(format("Orderer %s wait time %s not parsable.", name, ordererWaitTimeMilliSecsString), e);
            }

            ordererWaitTimeMilliSecs = tempOrdererWaitTimeMilliSecs;
        }

    }

    synchronized void shutdown(boolean force) {

        if (shutdown) {
            return;
        }
        shutdown = true;
        ManagedChannel lchannel = managedChannel;
        managedChannel = null;
        if (lchannel == null) {
            return;
        }
        if (force) {
            lchannel.shutdownNow();
        } else {
            boolean isTerminated = false;

            try {
                isTerminated = lchannel.shutdown().awaitTermination(3, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.debug(e); //best effort
            }
            if (!isTerminated) {
                lchannel.shutdownNow();
            }
        }
    }

    @Override
    public void finalize() {
        shutdown(true);
    }

    DeliverResponse[] connect(Common.Envelope envelope) throws TransactionException {

        if (shutdown) {
            throw new TransactionException("Orderer client is shutdown");
        }

        StreamObserver<Common.Envelope> nso = null;

        ManagedChannel lmanagedChannel = managedChannel;

        if (lmanagedChannel == null || lmanagedChannel.isTerminated() || lmanagedChannel.isShutdown()) {

            lmanagedChannel = channelBuilder.build();
            managedChannel = lmanagedChannel;

        }

        try {

            AtomicBroadcastGrpc.AtomicBroadcastStub broadcast = AtomicBroadcastGrpc.newStub(lmanagedChannel);

            // final DeliverResponse[] ret = new DeliverResponse[1];
            final List<DeliverResponse> retList = new ArrayList<>();
            final List<Throwable> throwableList = new ArrayList<>();
            final CountDownLatch finishLatch = new CountDownLatch(1);

            StreamObserver<DeliverResponse> so = new StreamObserver<DeliverResponse>() {
                boolean done = false;

                @Override
                public void onNext(DeliverResponse resp) {

                    // logger.info("Got Broadcast response: " + resp);
                    logger.debug("resp status value: " + resp.getStatusValue() + ", resp: " + resp.getStatus() + ", type case: " + resp.getTypeCase());

                    if (done) {
                        return;
                    }

                    if (resp.getTypeCase() == STATUS) {
                        done = true;
                        retList.add(0, resp);

                        finishLatch.countDown();

                    } else {
                        logger.trace(format("GOT EVENT: %s", resp.getBlock()));
                        retList.add(resp);
                        finishLatch.countDown();
                    }

                }

                @Override
                public void onError(Throwable t) {
                    if (!shutdown) {
                        logger.error(format("Received error on channel %s, orderer %s, url %s, %s",
                                channelName, name, url, t.getMessage()), t);
                    }
                    throwableList.add(t);
                    finishLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    logger.trace("onCompleted");
                    finishLatch.countDown();
                }
            };

            nso = broadcast.deliver(so);
            nso.onNext(envelope);
            //nso.onCompleted();

            try {
                //   if (!finishLatch.await(ordererWaitTimeMilliSecs, TimeUnit.MILLISECONDS)) {
                if (!finishLatch.await(9999999, TimeUnit.MILLISECONDS)) {
                    TransactionException ex = new TransactionException(format(
                            "Channel %s connect time exceeded for orderer %s, timed out at %d ms.", channelName, name, ordererWaitTimeMilliSecs));
                    logger.error(ex.getMessage(), ex);
                    throw ex;
                }
                logger.trace("Done waiting for reply!");

            } catch (InterruptedException e) {
                logger.error(e);
            }

            if (!throwableList.isEmpty()) {
                Throwable throwable = throwableList.get(0);
                TransactionException e = new TransactionException(format(
                        "Channel %s connect failed on orderer %s. Reason: %s", channelName, name, throwable.getMessage()), throwable);
                logger.error(e.getMessage(), e);
                throw e;
            }

            return retList.toArray(new DeliverResponse[retList.size()]);
        } catch (Throwable t) {
            managedChannel = null;
            throw t;

        } finally {
            if (null != nso) {

                try {
                    nso.onCompleted();
                } catch (Exception e) {  //Best effort only report on debug
                    logger.debug(format("Exception completing connect with channel %s,  name %s, url %s %s",
                            channelName, name, url, e.getMessage()), e);
                }

            }
        }
    }

    boolean isChannelActive() {
        ManagedChannel lchannel = managedChannel;
        return lchannel != null && !lchannel.isShutdown() && !lchannel.isTerminated();
    }
}
