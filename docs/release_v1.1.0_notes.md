# Java SDK for Hyperledger Fabric 1.1 release notes.

The JSDK 1.1 has features added since the 1.0 based release to match those added to the Fabric 1.1 release.

## Fabric v1.0 considerations
The SDK is mostly backward compatible with the v1.0 based Fabric with the following considerations
- New peer eventing service is the default but the v1.0 Fabric does not support this. To address in applications that are
  connecting to Fabric 1.0 you must when adding or joining a peer to a channel provide a PeerRole option. You can see an example of this 
  in [End2endIT.java#L732](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endIT.java#L732)
  and in [End2endAndBackAgainIT.java#L597](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endAndBackAgainIT.java#L597)
  
  
## v1.1 Fabric features
 
### [FAB-7652 JSDK filterblock enablement](https://jira.hyperledger.org/browse/FAB-7652)
### [FAB-6066 JSDK Channel service for events](https://jira.hyperledger.org/browse/FAB-6066)
### [FAB-6603 Java SDK CryptoPrimitives should perform Signature operations using standard JCA/JCE](https://jira.hyperledger.org/browse/FAB-6603)
### [FAB-5632 Implement "Connection Profile" for java-sdk](https://jira.hyperledger.org/browse/FAB-5632)
### [FAB-5387 Provide listener for custom chaincode events.](https://jira.hyperledger.org/browse/FAB-5387)
### [FAB-6200 Java serialize channels.](https://jira.hyperledger.org/browse/FAB-6200)
 
## v1.1 Fabric/CA features

### [FAB-7383 Implement the Fabric-CA identities and affiliations API](https://jira.hyperledger.org/browse/FAB-7383)
### [FAB-5827 Support SDK API to request attributes in ECert and handle secret material appropriately](https://jira.hyperledger.org/browse/FAB-5827)
    