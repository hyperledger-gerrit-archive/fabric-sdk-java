/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import java.util.Objects;
import java.util.Properties;

import com.google.common.util.concurrent.ListenableFuture;
import io.netty.util.internal.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.PeerException;
import org.hyperledger.fabric.sdk.helper.SDKUtil;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.PeerException;

import static org.hyperledger.fabric.sdk.helper.SDKUtil.checkGrpcUrl;

/**
 * The Peer class represents a peer to which SDK sends deploy, or query requests.
 */
public class Peer {
    private static final Log logger = LogFactory.getLog(Peer.class);
    private final EndorserClient endorserClent;
    private final Properties properties;
    private final String name;
    private final String url;

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


        this.endorserClent = new EndorserClient(new Endpoint(url, this.properties).getChannelBuilder());


    }

    public String getName() {
        return name;
    }

    public Properties getProperties() {
        return properties == null ? null : (Properties) properties.clone();
    }


    /**
     * Set the chain the peer is on.
     *
     * @param chain
     */

    void setChain(Chain chain) throws InvalidArgumentException {
        if (chain == null) {
            throw new InvalidArgumentException("Chain can not be null");
        }

        this.chain = chain;
    }

    private Chain chain;


    /**
     * Get the chain of which this peer is a member.
     *
     * @return {Chain} The chain of which this peer is a member.
     */
    public Chain getChain() {
        return this.chain;
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
        if (this == otherPeer)
            return true;
        if (otherPeer == null)
            return false;
        if (! (otherPeer instanceof Peer))
            return false;
        Peer p = (Peer) otherPeer;
        return Objects.equals(getName(), p.getName()) && Objects.equals(getUrl(), p.getUrl());
    }

    public ListenableFuture<FabricProposalResponse.ProposalResponse> sendProposalAsync(FabricProposal.SignedProposal proposal)
            throws PeerException, InvalidArgumentException {
        checkSendProposal(proposal);

        logger.debug("peer.sendProposalAsync");

        return endorserClent.sendProposalAsync(proposal);
    }

    public FabricProposalResponse.ProposalResponse sendProposal(FabricProposal.SignedProposal proposal)
            throws PeerException, InvalidArgumentException {
        checkSendProposal(proposal);

        logger.debug("peer.sendProposal");

        return endorserClent.sendProposal(proposal);
    }

    private void checkSendProposal(FabricProposal.SignedProposal proposal) throws PeerException, InvalidArgumentException {
        if (proposal == null) {
            throw new PeerException("Proposal is null");
        }
        if (chain == null) {
            throw new PeerException("Chain is null");
        }
        Exception e = checkGrpcUrl(url);
        if (e != null) {
            throw new InvalidArgumentException("Bad peer url.", e);

        }
    }


    static Peer createNewInstance(String name, String grpcURL, Properties properties) throws InvalidArgumentException {

        return new Peer(name, grpcURL, properties);
    }
} // end Peer
