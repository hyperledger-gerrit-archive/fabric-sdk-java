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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.protos.common.Common.Envelope;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;

class EnvelopeDeserializer {
    private final ByteString byteString;
    private WeakReference<Envelope> envelope;
    private WeakReference<PayloadDeserializer> payload;

    EnvelopeDeserializer(ByteString byteString) {
        this.byteString = byteString;
    }

    Envelope getEnvelope() {
        Envelope ret = null;

        if (envelope != null) {
            ret = envelope.get();

        }
        if (ret == null) {

            try {
                ret = Envelope.parseFrom(byteString);
            } catch (InvalidProtocolBufferException e) {
                throw new InvalidProtocolBufferRuntimeException(e);
            }

            envelope = new WeakReference<>(ret);

        }

        //Todo         ret.getSignature();


        return ret;

    }

    PayloadDeserializer getPayload() {

        PayloadDeserializer ret = null;

        if (payload != null) {
            ret = payload.get();

        }
        if (ret == null) {

            ret = new PayloadDeserializer(getEnvelope().getPayload());
            payload = new WeakReference<>(ret);

        }

        return ret;
    }

}
