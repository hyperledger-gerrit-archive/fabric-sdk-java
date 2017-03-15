package org.hyperledger.fabric.sdkintegration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

/** Sample Org Representation

 */
public class SampleOrg {
    HFCAClient caClient;
    final String name;

    public SampleUser getAdmin() {
        return admin;
    }

    private SampleUser admin;

    public void setAdmin(SampleUser admin) {
        this.admin = admin;
    }

    public String getMSPID() {
        return mspid;
    }

    final String mspid;
    String adminUserName;
    String adminUserSecret;
 //   Set<String > peerNames = new  HashSet<>();
    Map<String, User> userMap = new HashMap<>();

    Map<String, String> peerLocations = new HashMap<>();
    Map<String, String> ordererLocations = new HashMap<>();
    Map<String, String> eventHubLocations = new HashMap<>();



    private String caLocation;


    public SampleOrg(String name, String mspid) {
        this.name = name;
        this.mspid = mspid;
    }
    public void setAdminUser( String name, String secret){

        adminUserName = name;
        adminUserSecret = secret;
    }

//    public void setPeerNames(String peerNamess) {
//
//        String[] pns = peerNamess.split("[ \t]*,[ \t]*");
//        for(String p : pns){
//            peerNames.add(p);
//        };
//

//    }

    public void setCALocation(String caLocation) {
        this.caLocation = caLocation;
    }

    public String getCALocation() {
        return this.caLocation;
    }

    public void addPeerLocation(String name, String location) {

        peerLocations.put(name, location);
    }

    public void addOrdererLocation(String name, String location) {

        ordererLocations.put(name, location);
    }

    public void addEventHubLocation(String name, String location) {

        eventHubLocations.put(name, location);
    }

    public String getPeerLocation(String name){
        return peerLocations.get(name);

    }

    public Set<String> getPeerNames(){

       return  Collections.unmodifiableSet( peerLocations.keySet());
    }

    public void setCAClient(HFCAClient caClient) {

        this.caClient = caClient;
    }

    public HFCAClient getCAClient( ) {

        return caClient;
    }

    public String getName() {
        return name;
    }

    public void addUser(SampleUser user) {
        userMap.put(user.getName(),user);
    }

    public User getUser(String name){
        return  userMap.get(name);
    }

    public Collection<String> getOrdererLocations() {
        return Collections.unmodifiableCollection(ordererLocations.values());
    }

    public Collection<String> getEventHubLocations() {
        return Collections.unmodifiableCollection(eventHubLocations.values());
    }
    Set<Peer> peers = new HashSet<>();

    public Set<Peer> getPeers() {
        return Collections.unmodifiableSet(peers);
    }

    public void addPeer(Peer peer) {
        peers.add(peer);
    }

}
