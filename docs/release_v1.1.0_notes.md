# Java SDK for Hyperledger Fabric 1.1 release notes.

The JSDK 1.1 has features added since the 1.0 based release to match those added to the Fabric 1.1 release.

## Fabric v1.0 considerations
The SDK is mostly backward compatible with the v1.0 based Fabric with the following considerations
- New peer eventing service is the default but the v1.0 Fabric does not support this. To address in applications that are
  connecting to Fabric 1.0 you must when adding or joining a peer to a channel provide a PeerRole option. You can see an example of this 
  in [End2endIT.java#L732](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endIT.java#L732)
  and in [End2endAndBackAgainIT.java#L597](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endAndBackAgainIT.java#L597)
  
  
## v1.1 Fabric features
 

### [FAB-6066 JSDK Channel service for events](https://jira.hyperledger.org/browse/FAB-6066)
The Fabric Peer now implements eventing services on the same endpoint as proposal responses and is no longer necessary to have and EventHub service.  Using Peer
eventing is preferred means for getting events.  Future releases of Fabric may not support Event hubs. When joining or adding Peers to a channel the default is
to have the peer provide the eventing eventing service. Whether a peer is an eventing or non-eventing is controlled by the peer options when adding or joining
a channel.You can see an example of this in [End2endIT.java#L732](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endIT.java#L732)
and in [End2endAndBackAgainIT.java#L597](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endAndBackAgainIT.java#L597)

Peers may be added to a channel with specific roles to help with distributing the workload. PeerRoles are defined in [Peer.java#L328](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/main/java/org/hyperledger/fabric/sdk/Peer.java#L328)
The default is for Peers to have all roles.
            
            
          
### [FAB-7652 JSDK filterblock enablement](https://jira.hyperledger.org/browse/FAB-7652)

Fabric supports on the new Peer eventing service limits to what the events return thourgh ACLs.  The block event may contain the full Block or a FilteredBlock. 
Applications requesting for a full Block without authority will get a permission failure.  Application by default will get the full block. To request
request FiltedBlock when adding or joining peers applicaitons can add via PeerOptions.registerEventsForFilteredBlocks(). An example of this is seen in
[End2endAndBackAgainIT.java#L592](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endAndBackAgainIT.java#L592)

Application's that register block listeners need to be written to check for *isFiltered()" to know if the block is full or filtered. 

Filtered blocks are limited to the following methods.

FilteredBlocks  
 - isFiltered should return true
 - getChannelId the channel name
 - getFilteredBlock the raw filtered block
 - getBlockNumber blocknumber
 - getEnvelopeCount number of envelopes
 - getEnvelopeInfo index into envelopes.
 - getEnvelopeInfos interator on envelopes
 
 EnvelopeInfo
 - getChannelId channel name
 - getTransactionID the transaction id
 - isValid was the transaction valid.
 - getType the type of envelope
 
 TransactionEnvelopeInfo all the methods on EnvelopeInfo
 - getTransactionActionInfoCount number transactions 
 - getTransactionActionInfos an integrater over all the TransactionAction
 
 TransactionActionInfo
 - getEvent chaincode events
 
 
### [FAB-6603 Java SDK CryptoPrimitives should perform Signature operations using standard JCA/JCE](https://jira.hyperledger.org/browse/FAB-6603)
Changes made to make the Java SDK crypto primitives to use more JCA/JCE compliant methods. These changes are internal and do not
directly affect the application.

### [FAB-5632 Implement "Connection Profile" for java-sdk](https://jira.hyperledger.org/browse/FAB-5632)
Allow creating channels from a yaml or json specified document. Examples of this can be found in [NetworkConfigIT.java#L65](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/NetworkConfigIT.java#L65)

### [FAB-5387 Provide listener for custom chaincode events.](https://jira.hyperledger.org/browse/FAB-5387)
Allow application to register for specific events triggered by chaincode. Example of this can be found in 
[End2endIT.java#L303](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endIT.java#L303)

The registerChaincodeEventListener method on the channel registers a call back that matches via Java pattern on both the event name and the 
chaincodeId.  When ledger block with the event that matches that criteria specified is found by event hub or new peer event service the
callback is called to handle the event.  
### [FAB-6200 Java serialize channels.](https://jira.hyperledger.org/browse/FAB-6200)
Channels can be java serialized and deserialized.  Examples of this can be found throughout the integration tests. Example of serialization
in [End2endIT.java#L257](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endIT.java#L257)
where the sample store stores channel bar. Later in [End2endAndBackAgainIT.java#L562](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endAndBackAgainIT.java#L562)
it's restored.
*Applications using this serialziation means will be tasked with any migrating future changes. The SDK will not do this.*
It's advised to use a different persistene means for saving and restoring channel.s 
 
## v1.1 Fabric/CA features

### [FAB-7383 Implement the Fabric-CA identities and affiliations API](https://jira.hyperledger.org/browse/FAB-7383)
Fabric CA API added apis for manage identies and affiliations. Examples how this can be done with Java SDK on how to 
create, modify, read and delete are in [HFCAClientIT.java#L546-L658](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric_ca/sdkintegration/HFCAClientIT.java#L546-L658)

### [FAB-5827 Support SDK API to request attributes in ECert and handle secret material appropriately](https://jira.hyperledger.org/browse/FAB-5827)
    