/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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


import java.net.MalformedURLException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.events.EventHub;
import org.hyperledger.fabric.sdk.exception.EnrollmentException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.RegistrationException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

public class HFClient {

    private static final int DEFAULT_SECURITY_LEVEL = 256;  //TODO make configurable //Right now by default FAB services is using
    private static final String DEFAULT_HASH_ALGORITHM = "SHA2";  //Right now by default FAB services is using SHA2

    static {

        if (null == System.getProperty("org.hyperledger.fabric.sdk.logGRPC")) {
            // Turn this off by default!
            Logger.getLogger("io.netty").setLevel(Level.OFF);
            Logger.getLogger("io.grpc").setLevel(Level.OFF);

        }
    }

    private static final Log logger = LogFactory.getLog(HFClient.class);

    private final Map<String, Chain> chains = new HashMap<>();

    public User getUserContext() {
        return userContext;
    }

    private User userContext;

    // The key-val store used for this chain
    private KeyValStore keyValStore;

    // The member services used for this chain
    private MemberServices memberServices;

    private HFClient() {

    }

    public CryptoPrimitives getCryptoPrimitives() {
        return cryptoPrimitives;
    }

//    public void setCryptoPrimitives(CryptoPrimitives cryptoPrimitives) {
//        this.cryptoPrimitives = cryptoPrimitives;
//    }

    private CryptoPrimitives cryptoPrimitives = new CryptoPrimitives(DEFAULT_HASH_ALGORITHM, DEFAULT_SECURITY_LEVEL);

    /**
     * Get the key val store implementation (if any) that is currently associated with this chain.
     *
     * @return The current KeyValStore associated with this chain, or undefined if not set.
     */
    public KeyValStore getKeyValStore() {
        return this.keyValStore;
    }

    /**
     * Set the key value store implementation.
     */
    public void setKeyValStore(KeyValStore keyValStore) {
        this.keyValStore = keyValStore;
    }

    public static HFClient createNewInstance() {
        return new HFClient();
    }

    public Chain newChain(String name) throws InvalidArgumentException {
        logger.trace("Creating chain :" + name);
        Chain newChain = Chain.createNewInstance(name, this);
        chains.put(name, newChain);
        return newChain;
    }

    public Chain newChain(String name, Orderer orderer, ChainConfiguration chainConfiguration) throws TransactionException, InvalidArgumentException {

        logger.trace("Creating chain :" + name);
        Chain newChain = Chain.createNewInstance(name, this, orderer, chainConfiguration );
        chains.put(name, newChain);
        return newChain;
    }

    public Peer newPeer(String name) throws InvalidArgumentException {
        return Peer.createNewInstance(name, null);
    }

    public Peer newPeer(String url, String pem) throws InvalidArgumentException {
        return Peer.createNewInstance(url, pem);
    }

    public Orderer newOrderer(String url) throws InvalidArgumentException {
        return Orderer.createNewInstance(url, null);
    }

    /**
     * Set the member services URL
     *
     * @param url User services URL of the form: "grpc://host:port" or "grpcs://host:port"
     * @param pem
     * @throws CertificateException
     */
    public void setMemberServicesUrl(String url, String pem) throws CertificateException, MalformedURLException {
        this.setMemberServices(new MemberServicesFabricCAImpl(url, pem));
    }

    /**
     * Get the member service associated this chain.
     *
     * @return MemberServices associated with the chain, or undefined if not set.
     */
    public MemberServices getMemberServices() {
        return this.memberServices;
    }


    /**
     * Set the member service
     *
     * @param memberServices The MemberServices instance
     */
    public void setMemberServices(MemberServices memberServices) {
        this.memberServices = memberServices;
    }


    public Chain getChain(String name) {
        return chains.get(name);
    }

    public InstallProposalRequest newInstallProposalRequest() {
        return new InstallProposalRequest();
    }

    public InstantiateProposalRequest newInstantiationProposalRequest() {
        return new InstantiateProposalRequest();
    }

    public InvokeProposalRequest newInvokeProposalRequest() {
        return InvokeProposalRequest.newInstance();
    }

    public QueryProposalRequest newQueryProposalRequest() {
        return QueryProposalRequest.newInstance();
    }

    public void setUserContext(User userContext) {
        this.userContext = userContext;
    }

    public EventHub newEventHub(String eventHub) {
        return EventHub.createNewInstance(eventHub, null);
    }




    private final Map<String, User> members = new HashMap<>();

    /**
     * Register a user or other user type with the chain.
     *
     * @param registrationRequest Registration information.
     * @throws RegistrationException if the registration fails
     */
    public User register(RegistrationRequest registrationRequest) throws RegistrationException {
        User user = getMember(registrationRequest.getEnrollmentID());
        user.register(registrationRequest);
        return user;
    }

    /**
     * Enroll a user or other identity which has already been registered.
     *
     * @param name   The name of the user or other member to enroll.
     * @param secret The enrollment secret of the user or other member to enroll.
     * @throws EnrollmentException
     */

    public User enroll(String name, String secret) throws EnrollmentException {
        User user = getMember(name);
        if (!user.isEnrolled()) {
            user.enroll(secret);
        }

        members.put(name, user);

        return user;
    }

    /**
     * Register and enroll a user or other member type.
     * This assumes that a registrar with sufficient privileges has been set.
     *
     * @param registrationRequest Registration information.
     * @throws RegistrationException
     * @throws EnrollmentException
     */
    public User registerAndEnroll(RegistrationRequest registrationRequest) throws RegistrationException, EnrollmentException {
        User user = getMember(registrationRequest.getEnrollmentID());
        user.registerAndEnroll(registrationRequest);
        return user;
    }

    /**
     * Get the user with a given name
     *
     * @return user
     */
    public User getMember(String name) {
        if (null == keyValStore)
            throw new RuntimeException("No key value store was found.  You must first call Chain.setKeyValStore");
        if (null == memberServices)
            throw new RuntimeException("No user services was found.  You must first call Chain.setMemberServices or Chain.setMemberServicesUrl");

        // Try to get the user state from the cache
        User user = members.get(name);
        if (null != user) return user;

        // Create the user and try to restore it's state from the key value store (if found).
        user = new User(name, this);
        user.restoreState();
        return user;

    }


}
