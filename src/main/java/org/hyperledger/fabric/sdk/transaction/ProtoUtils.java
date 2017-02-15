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
package org.hyperledger.fabric.sdk.transaction;

import org.hyperledger.fabric.protos.common.Common.ChannelHeader;
import org.hyperledger.fabric.protos.common.Common.HeaderType;
import org.hyperledger.fabric.protos.peer.FabricProposal.ChaincodeHeaderExtension;


public class ProtoUtils {

    /**
     * createChannelHeader create chainHeader
     *
     * @param type
     * @param txID
     * @param chainID
     * @param epoch
     * @param chaincodeHeaderExtension
     * @return
     */
     static ChannelHeader createChannelHeader(HeaderType type, String txID, String chainID, long epoch, ChaincodeHeaderExtension chaincodeHeaderExtension) {

        ChannelHeader.Builder ret = ChannelHeader.newBuilder()
                .setType(type.getNumber())
                .setVersion(0)
                .setTxId(txID)
                .setChannelId(chainID)
                .setEpoch(epoch);
        if (null != chaincodeHeaderExtension) {
            ret.setExtension(chaincodeHeaderExtension.toByteString());
        }

        return ret.build();

    }
}
