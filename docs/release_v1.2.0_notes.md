# Java SDK for Hyperledger Fabric 1.2 release notes.

The JSDK 1.2 has features added since the 1.2 based release to match those added to the Fabric 1.2 release.

## Fabric v1.0 and v1.1 considerations
The SDK is mostly backward compatible with the v1.x based Fabric with the following considerations
- The new Peer eventing service is the default for the SDK however, in v1.0 Fabric peer eventing service is not supported. To address in applications that are
  connecting to Fabric 1.0 you must when adding or joining a peer to a channel provide a PeerRole option.
  A role with `PeerRole.NO_EVENT_SOURCE` has been defined that has the equivalent functionality of a v1.0 peer.
  You can see an example of this
  in [End2endIT.java#L732](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endIT.java#L732)
  and in [End2endAndBackAgainIT.java#L597](https://github.com/hyperledger/fabric-sdk-java/blob/9224fa3f45a70392d1b244c080bf41bd561470d3/src/test/java/org/hyperledger/fabric/sdkintegration/End2endAndBackAgainIT.java#L597)


## v1.2 Fabric features

### [FAB-9680 private data collection support](https://jira.hyperledger.org/browse/FAB-9680)

Private data collection example is in [src/test/java/org/hyperledger/fabric/sdkintegration/PrivateDataIT.java
](https://github.com/hyperledger/fabric-sdk-java/blob/edd54f832351452ef6aea3d9cb505b2f38b12711/src/test/java/org/hyperledger/fabric/sdkintegration/PrivateDataIT.java)
From the SDK perspective there is very little change from installing, instantiating and invoking chaincode.  The only notable change is in
instantiation. In instantiation there is a requirement to pass in the Instantiation proposal a chaincode collection configuration  with the method `setChaincodeCollectionConfiguration` as seen on
this [line](https://github.com/hyperledger/fabric-sdk-java/blob/edd54f832351452ef6aea3d9cb505b2f38b12711/src/test/java/org/hyperledger/fabric/sdkintegration/PrivateDataIT.java#L246).
The [ChaincodeCollectionConfiguration](https://github.com/hyperledger/fabric-sdk-java/blob/edd54f832351452ef6aea3d9cb505b2f38b12711/src/main/java/org/hyperledger/fabric/sdk/ChaincodeCollectionConfiguration.java)
class allows collection configuration to be loaded from YAML or a json file or object.  Sample collection configuration files
exist in [src/test/fixture/collectionProperties](https://github.com/hyperledger/fabric-sdk-java/tree/edd54f832351452ef6aea3d9cb505b2f38b12711/src/test/fixture/collectionProperties).
[PrivateDataIT.yaml](https://github.com/hyperledger/fabric-sdk-java/blob/edd54f832351452ef6aea3d9cb505b2f38b12711/src/test/fixture/collectionProperties/PrivateDataIT.yaml)
has comments that help explain some of the aspects of configuring private collections.

### [FAB-8805 JSDK Service Discovery](https://jira.hyperledger.org/browse/FAB-8805)





## v1.1 Fabric/CA features

### [FAB-10322 HFCAClient needs timeout settings](https://jira.hyperledger.org/browse/FAB-10322)

Configuration timeout properties for HTTP requests to the Fabric CA have been added. The properties
 `org.hyperledger.fabric_ca.sdk.connection.connection_request_timeout`,  `org.hyperledger.fabric_ca.sdk.connection.connect_timeout` and `org.hyperledger.fabric_ca.sdk.connection.socket_timeout`
  can be set and correspond to the HTTP client's equivalent values.
 For more information see [Apache's HTTP RequestConfig.Builder](https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/config/RequestConfig.Builder.html)
