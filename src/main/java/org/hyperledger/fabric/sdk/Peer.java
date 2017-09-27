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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.internal.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.exception.EventHubException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.PeerException;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

import static java.lang.String.format;
import static org.hyperledger.fabric.sdk.helper.Utils.checkGrpcUrl;

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
    private final boolean isEventing;

    // The possible roles that a peer can perform
    // Package private for now pending further analysis on the required functionality
    public enum PeerRole {
        ENDORSING_PEER("endorsingPeer"),
        CHAINCODE_QUERY("chaincodeQuery"),
        LEDGER_QUERY("ledgerQuery"),
        EVENT_SOURCE("eventSource");

        private String propertyName;

        PeerRole(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }

    static final String PEER_BASE_PROPERTY_NAME = "org.hyperledger.fabric.sdk.peer.";

    private Set<PeerRole> roles = EnumSet.allOf(PeerRole.class).clone();

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

        if (null != properties) {
            String role = properties.getProperty(PEER_BASE_PROPERTY_NAME + "roles");

            if (role != null) {
                roles = new HashSet<>();
                final String[] split = role.split("[ \t]*:[ \t]*");
                for (String r : split) {
                    roles.add(PeerRole.valueOf(r));
                }
            }

            role = properties.getProperty(PEER_BASE_PROPERTY_NAME + "remove_roles");

            if (role != null) {
                final String[] split = role.split("[ \t]*:[ \t]*");
                for (String r : split) {
                    roles.remove(PeerRole.valueOf(r));

                }
            }

            if (roles.isEmpty()) {
                throw new InvalidArgumentException(format("Peer %s must have at least one role.", name));
            }
        }

        isEventing = hasRole(PeerRole.EVENT_SOURCE);
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
     * Returns whether or not this peer has the specified role.
     * <p>
     * By default a peer has all available roles, and hence this method will only return false
     * if the role has explicitly been "unset"
     *
     * @param role The role to be checked
     * @return true if this peer has the specified role
     */
    boolean hasRole(PeerRole role) {
        return roles.contains(role);
    }

    private transient PeerEventingClient peerEventingClient;

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

    ExecutorService getExecutorService() {
        return channel.getExecutorService();
    }

    void initiateEventing(TransactionContext transactionContext) throws EventHubException, InvalidArgumentException {

        if (isEventing) {
            if (peerEventingClient == null) {

                peerEventingClient = new PeerEventingClient(this, new HashSet<Channel>(Arrays.asList(new Channel[] {channel})));

                peerEventingClient.connect(transactionContext);

            }
        }

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

        if (lendorserClent != null) {
            lendorserClent.shutdown(force);
        }

        if (null != peerEventingClient) {
            PeerEventingClient lsEventHub = peerEventingClient;
            peerEventingClient = null;
            lsEventHub.shutdown();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        shutdown(true);
        super.finalize();
    }
} // end Peer
