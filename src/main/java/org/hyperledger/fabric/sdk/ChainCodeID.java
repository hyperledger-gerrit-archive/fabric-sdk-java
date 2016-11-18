package org.hyperledger.fabric.sdk;


import org.hyperledger.protos.peer.Chaincode;

/**
 * Wrapper to not expose Fabric's ChainCoodeId

 */
public class ChainCodeID {

     Chaincode.ChaincodeID getFabricChainCodeID() {
        return fabricChainCodeID;
    }

    final Chaincode.ChaincodeID fabricChainCodeID;

     ChainCodeID(Chaincode.ChaincodeID chaincodeID) {
        this.fabricChainCodeID = chaincodeID;
    }
    @Deprecated
    public String getName(){
        return fabricChainCodeID.getName();
    }

}
