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

import java.lang.ref.WeakReference;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.common.Configtx;
import org.hyperledger.fabric.protos.common.Configtx.ConfigGroup;
import org.hyperledger.fabric.protos.msp.MspConfig;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;

class ConfigTxEnvelopeDataDeserializer {

    private final ByteString byteString;
    private WeakReference<Configtx.ConfigEnvelope> configEnvelopeWeakReference;

    public ConfigTxEnvelopeDataDeserializer(ByteString byteString) {
        this.byteString = byteString;

    }

    Configtx.ConfigEnvelope getConfigTxEnvelopeDataDeserializer() {
        Configtx.ConfigEnvelope ret = null;

        if (configEnvelopeWeakReference != null) {
            ret = configEnvelopeWeakReference.get();

        }
        if (ret == null) {

            try {
                ret = Configtx.ConfigEnvelope.parseFrom(byteString);
            } catch (InvalidProtocolBufferException e) {
                throw new InvalidProtocolBufferRuntimeException(e);
            }

            configEnvelopeWeakReference = new WeakReference<>(ret);
        }

        final ConfigGroup channelGroup = ret.getConfig().getChannelGroup();
        //  channelGroup.get

        return ret;

    }

    private Map<String, Channel.MSP> traverseConfigGroupsMSP(String name, ConfigGroup configGroup, Map<String, Channel.MSP> msps) throws InvalidProtocolBufferException {

        Configtx.ConfigValue mspv = configGroup.getValuesMap().get("MSP");
        if (null != mspv) {
            if (!msps.containsKey(name)) {

                MspConfig.MSPConfig mspConfig = MspConfig.MSPConfig.parseFrom(mspv.getValue());

                MspConfig.FabricMSPConfig fabricMSPConfig = MspConfig.FabricMSPConfig.parseFrom(mspConfig.getConfig());

                //   msps.put(name, new Channel.MSP(name, fabricMSPConfig));

            }
        }

        for (Map.Entry<String, ConfigGroup> gm : configGroup.getGroupsMap().entrySet()) {
            traverseConfigGroupsMSP(gm.getKey(), gm.getValue(), msps);
        }

        return msps;
    }





    /*
protected void parseConfigBlock() throws TransactionException {

        try {

            final Block configBlock = getConfigurationBlock();

            logger.debug(format("Channel %s Got config block getting MSP data and anchorPeers data", name));

            Envelope envelope = Envelope.parseFrom(configBlock.getData().getData(0));
            Payload payload = Payload.parseFrom(envelope.getPayload());
            ConfigEnvelope configEnvelope = ConfigEnvelope.parseFrom(payload.getData());
            ConfigGroup channelGroup = configEnvelope.getConfig().getChannelGroup();
            Map<String, MSP> newMSPS = traverseConfigGroupsMSP("", channelGroup, new HashMap<>(20));

            msps = Collections.unmodifiableMap(newMSPS);

            anchorPeers = Collections.unmodifiableSet(traverseConfigGroupsAnchors("", channelGroup, new HashSet<>()));

        } catch (TransactionException e) {
            logger.error(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new TransactionException(e);
        }

    }
 */

}




