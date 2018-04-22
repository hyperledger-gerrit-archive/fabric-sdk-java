/*
 *  Copyright 2018 Mediaocean - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdkintegration.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


/**
 * Interface to configtxlator process for updating configuration.
 *
 * @author alana
 *
 */
public class ConfigtxlatorHelper {
    private final String TXURL;

    private static final String DECODE_JSON = "/protolator/decode/common.Config";
    private static final String DECODE_JSON_ENVELOPE = "/protolator/decode/common.Envelope";
    private static final String ENCODE_JSON = "/protolator/encode/common.Config";
    private static final String UPDATE = "/configtxlator/compute/update-from-configs";

    public ConfigtxlatorHelper(String url) {
        TXURL = url;
    }

    /**
     * Call configtxlator to decode a pb to JSON
     *
     * @param ccb
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String decodeJSON(String proto, byte[] ccb) throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(proto);
        httppost.setEntity(new ByteArrayEntity(ccb));

        HttpResponse htr = httpclient.execute(httppost);
        int statuscode = htr.getStatusLine().getStatusCode();
        System.out.println(String.format("Got %d status for decoding current channel config bytes", statuscode));

        // TODO: Throw exception if invalid?
        return EntityUtils.toString(htr.getEntity());
    }

    /**
     * Call configtxlator to decode a pb to JSON
     *
     * @param ccb
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public String decodeJSON(byte[] ccb) throws ClientProtocolException, IOException {
        return decodeJSON(TXURL + DECODE_JSON, ccb);
    }

    public String decodeJSONEnvelope(byte[] ccb) throws ClientProtocolException, IOException {
        return decodeJSON(TXURL + DECODE_JSON_ENVELOPE, ccb);
    }

    /**
     * Call configtxlator to encode JSON to pb
     *
     * @param cas
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    public byte[] encodeJSON(String cas) throws ClientProtocolException, IOException {
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(TXURL + ENCODE_JSON);
        httppost.setEntity(new StringEntity(cas));

        HttpResponse htr = httpclient.execute(httppost);
        int statuscode = htr.getStatusLine().getStatusCode();
        // TODO: Exception if invalid?
        System.out.println(String.format("Got %d status for encoding current channel config bytes", statuscode));
        return EntityUtils.toByteArray(htr.getEntity());
    }

    /**
     * Send to configtxlator multipart form post with original config bytes, updated config bytes and channel name
     *
     * @param channelName
     * @param original original config
     * @param updated updated config
     * @return difference for an update transaction
     * @throws ClientProtocolException
     * @throws IOException
     */
    public byte[] update(String channelName, byte[] original, byte[] updated) throws ClientProtocolException, IOException {
        HttpPost httppost = new HttpPost(TXURL + UPDATE);

        //@formatter:off
        HttpEntity multipartEntity = MultipartEntityBuilder.create().setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                        .addBinaryBody("original", original, ContentType.APPLICATION_OCTET_STREAM, "originalFakeFilename")
                        .addBinaryBody("updated", updated, ContentType.APPLICATION_OCTET_STREAM, "updatedFakeFilename")
                        .addBinaryBody("channel", channelName.getBytes()).build();
        //@formatter:on

        httppost.setEntity(multipartEntity);

        HttpClient httpclient = HttpClients.createDefault();
        HttpResponse rs = httpclient.execute(httppost);
        int statuscode = rs.getStatusLine().getStatusCode();
        System.out.println(String.format("Got %d status for updated config bytes needed for updateChannelConfiguration", statuscode));

        return EntityUtils.toByteArray(rs.getEntity());
    }

    /**
     * String to map mapping - uses jackson
     *
     * @param s
     * @return
     * @throws Exception
     */
    public Map<String, Object> map(String s) throws Exception {
        MapType type = TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> result = mapper.readValue(s, type);
        return result;
    }

    /**
     * Map to string mapping - uses jackson
     *
     * @param map
     * @return
     * @throws Exception
     */
    public String toString(Map<String, Object> map) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }

}
