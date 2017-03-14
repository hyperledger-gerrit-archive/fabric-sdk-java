/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
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

package org.hyperledger.fabric.sdk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Properties;
import java.util.WeakHashMap;

import javax.net.ssl.SSLException;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.internal.StringUtil;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

import static org.hyperledger.fabric.sdk.helper.SDKUtil.parseGrpcUrl;

public class Endpoint {

    private final String addr;
    private final int port;
    private final Properties properties;
    private ManagedChannelBuilder<?> channelBuilder = null;

//    public Endpoint(String url, String pem) {
//
//        Properties purl = parseGrpcUrl(url);
//        String protocol = purl.getProperty("protocol");
//        this.addr = purl.getProperty("host");
//        this.port = Integer.parseInt(purl.getProperty("port"));
//
//        if (protocol.equalsIgnoreCase("grpc")) {
//            this.channelBuilder = ManagedChannelBuilder.forAddress(addr, port)
//                    .usePlaintext(true);
//        } else if (protocol.equalsIgnoreCase("grpcs")) {
//            if (StringUtil.isNullOrEmpty(pem)) {
//                // use root certificate
//                this.channelBuilder = ManagedChannelBuilder.forAddress(addr, port);
//            } else {
//                try {
//
//                    /*
//                    https://groups.google.com/forum/#!topic/grpc-io/0EdIOpTLWb4
//                    SslCredentialsOptions ssl_opts = {"", "", ""};
//
//      Client greeter(grpc::CreateChannel(std::string(argv[2])+":ABCD", SslCredentials(ssl_opts), ChannelArguments()));
//                     */
//
//                    SslContext sslContext = GrpcSslContexts.forClient().trustManager(new java.io.File(pem)).build();
//                    this.channelBuilder = NettyChannelBuilder.forAddress(addr, port)
//                            .sslContext(sslContext).overrideAuthority("admin");
//                } catch (SSLException sslex) {
//                    throw new RuntimeException(sslex);
//                }
//            }
//        } else {
//            throw new RuntimeException("invalid protocol: " + protocol);
//        }
//    }

   static WeakHashMap<String, String> cnCache = new WeakHashMap<>();

    public Endpoint(String url, Properties properties) {

        this.properties = properties;

        String pem = null;
        String cnn = null;

        Properties purl = parseGrpcUrl(url);
        String protocol = purl.getProperty("protocol");
        this.addr = purl.getProperty("host");
        this.port = Integer.parseInt(purl.getProperty("port"));

        if (properties != null) {
            if ( "grpcs".equals(protocol)) {
                try {
                    pem = properties.getProperty("pem");
                    cnn = properties.getProperty("overrideAuthority");

                    if (cnn == null && "true".equals(properties.getProperty("certificateOverride"))) {

                       File pemF=  new File(pem);
                       String cnKey=   pemF.getAbsolutePath();

                       cnn =  cnCache.get(cnKey);
                       if(cnn == null) {
                           Path path = Paths.get(pem);
                           byte[] data = Files.readAllBytes(path);

                           CryptoPrimitives cp = new CryptoPrimitives();


                           X500Name x500name = new JcaX509CertificateHolder((X509Certificate) cp.bytesToCertificate(data)).getSubject();
                           RDN cn = x500name.getRDNs(BCStyle.CN)[0];
                           //   cnn =  cn +"";
                           AttributeTypeAndValue f = cn.getFirst();
                           cnn = IETFUtils.valueToString(cn.getFirst().getValue());
                           cnCache.put(cnKey, cnn);
                       }


                    }
                }catch (Exception e) {
                    e.printStackTrace();

                }
            }

        }


        if (protocol.equalsIgnoreCase("grpc")) {
            this.channelBuilder = ManagedChannelBuilder.forAddress(addr, port)
                    .usePlaintext(true);
        } else if (protocol.equalsIgnoreCase("grpcs")) {
            if (StringUtil.isNullOrEmpty(pem)) {
                // use root certificate
                this.channelBuilder = ManagedChannelBuilder.forAddress(addr, port);
            } else {
                try {

                    /*
                    https://groups.google.com/forum/#!topic/grpc-io/0EdIOpTLWb4
                    SslCredentialsOptions ssl_opts = {"", "", ""};

      Client greeter(grpc::CreateChannel(std::string(argv[2])+":ABCD", SslCredentials(ssl_opts), ChannelArguments()));
                     */

                    SslContext sslContext = GrpcSslContexts.forClient().trustManager(new java.io.File(pem)).build();
                    this.channelBuilder = NettyChannelBuilder.forAddress(addr, port)
                            .sslContext(sslContext);
                    if (cnn != null) {
                        channelBuilder.overrideAuthority(cnn);
                    }
                } catch (SSLException sslex) {
                    throw new RuntimeException(sslex);
                }
            }
        } else {
            throw new RuntimeException("invalid protocol: " + protocol);
        }

    }

    public ManagedChannelBuilder<?> getChannelBuilder() {
        return this.channelBuilder;
    }

    String getHost() {
        return this.addr;
    }


    int getPort() {
        return this.port;
    }
}
