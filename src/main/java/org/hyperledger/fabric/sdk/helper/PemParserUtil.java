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

package org.hyperledger.fabric.sdk.helper;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Reader;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import org.apache.commons.compress.utils.IOUtils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import sun.misc.BASE64Encoder;

//import java.security.spec.ECPrivateKeySpec;

public class PemParserUtil {

    static final String readfrom = "src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/keystore/78f50c6aec2ee47f936374da94df455d39f57873ab45b7cbed4e0fcb85226676_sk";

    public static void main(String[] args) throws Exception {

        //      PrivateKeyInfo privateKeyinfo;
        //   BouncyCastleProvider.getPrivateKey(privateKeyinfo);

        bcc4();

    }

    static void bcc2() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        byte[] keybytes = IOUtils.toByteArray(new FileInputStream(readfrom));

        Reader reader = new FileReader(readfrom);
        PEMParser pemParser = new PEMParser(reader);
        PemObject pemObject = pemParser.readPemObject();

        System.out.println("pem object type: " + pemObject.getType());
        PemObject generate = pemObject.generate();

        byte[] content = pemObject.getContent();
        List headers = pemObject.getHeaders();

        System.out.println("keybytes:" + new String(keybytes, "UTF-8"));
        System.out.println("content:" + new BASE64Encoder().encode(content));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(content);

        KeyFactory bc = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
        PrivateKey privateKey = bc.generatePrivate(spec);

    }

//    static void bcc() throws Exception{
//
//        byte[] privateKeyBytes = IOUtils.toByteArray(new FileInputStream(readfrom));
//
//
//
//        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp192r1");
//
//        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(new BigInteger(1, privateKeyBytes), spec);
//
//        ECNamedCurveSpec params = new ECNamedCurveSpec("secp192r1", spec.getCurve(), spec.getG(), spec.getN());
//        java.security.spec.ECPoint w = new java.security.spec.ECPoint(new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 0, 24)), new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 24, 48)));
//        PublicKey publicKey = factory.generatePublic(new java.security.spec.ECPublicKeySpec(w, params));
//
//    }

    public static PrivateKey bcc4() throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        byte[] data = IOUtils.toByteArray(new FileInputStream(readfrom));

        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime256v1");
        //org.bouncycastle.jce.spec.ECParameterSpec;
        ECPrivateKeySpec prvkey = new ECPrivateKeySpec(new BigInteger(data), params);
      //  KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");'
        //ECDSA
                                                //ECDH
                                               //"ECDSA"
        KeyFactory kf = KeyFactory.getInstance("EC", "BC");
        return kf.generatePrivate(prvkey);
    }
}



