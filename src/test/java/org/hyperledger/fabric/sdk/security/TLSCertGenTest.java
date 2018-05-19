/*
 *
 *  Copyright 2018 IBM - All Rights Reserved.
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


package org.hyperledger.fabric.sdk.security;
import io.grpc.*;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import junit.framework.TestCase;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.hyperledger.fabric.protos.peer.EndorserGrpc;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.protos.peer.FabricProposalResponse;
import org.hyperledger.fabric.sdk.security.certgen.TLSCertificateKeyPair;
import org.hyperledger.fabric.sdk.security.certgen.TLSCertificateBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class TLSCertGenTest {

    private static List<File> files2Cleanup = new LinkedList<>();

    @AfterClass
    public static void tearDown() {
        files2Cleanup.forEach(f -> f.delete());
    }

    @Test
    public void selfSignedTLSCertTest() throws Exception {
        AtomicBoolean handshakeOccured = new AtomicBoolean(false);
        TLSCertificateBuilder certBuilder = new TLSCertificateBuilder();

        TLSCertificateKeyPair serverCert = certBuilder.serverCert("localhost");
        File serverCertFile = createFile("server-cert.pem", serverCert.getCertPEMBytes());
        File serverKeyFile  = createFile("server-key.pem", serverCert.getKeyPemBytes());

        TLSCertificateKeyPair clientCert = certBuilder.clientCert();
        File clientCertFile = createFile("client-cert.pem", clientCert.getCertPEMBytes());
        File clientKeyFile  = createFile("client-key.pem", clientCert.getKeyPemBytes());
        String certFileSubjectName = subjectNameFromPEM(clientCert.getCertPEMBytes());


        Server server = NettyServerBuilder.forPort(0).addService(new MockEndorser()).
                intercept(mutualTLSInterceptor(certFileSubjectName, handshakeOccured))
                .sslContext(GrpcSslContexts.forServer(serverCertFile, serverKeyFile)
                        .trustManager(clientCertFile)
                        .clientAuth(ClientAuth.REQUIRE)
                        .build()).build();

        server.start();

        NettyChannelBuilder channelBuilder = NettyChannelBuilder
                .forAddress("localhost", server.getPort())
                .sslContext(getSslContextBuilder(clientCertFile, clientKeyFile, serverCertFile).build())
                .negotiationType(NegotiationType.TLS);
        ManagedChannel chan = channelBuilder.build();
        FabricProposal.SignedProposal prop = FabricProposal.SignedProposal.getDefaultInstance();
        EndorserGrpc.newBlockingStub(chan).processProposal(prop);
        // Ensure that TLS handshake occurred
        Assert.assertTrue("Handshake didn't occur", handshakeOccured.get());
        server.shutdown();
    }

    SslContextBuilder getSslContextBuilder(File clientCertFile, File clientKeyFile, File serverCertFile) {
        SslProvider sslprovider = SslProvider.OPENSSL;
        SslContextBuilder ctxBuilder = SslContextBuilder.forClient().trustManager(serverCertFile);
        SslContextBuilder clientContextBuilder = GrpcSslContexts.configure(ctxBuilder, sslprovider);
        clientContextBuilder = clientContextBuilder.keyManager(clientCertFile, clientKeyFile);
        return clientContextBuilder;
    }

    private ServerInterceptor mutualTLSInterceptor(String expectedSubjectName, AtomicBoolean toggleHandshakeOccured) {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
                SSLSession sslSession = serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_SSL_SESSION);
                try {
                    String subjName = sslSession.getPeerCertificateChain()[0].getSubjectDN().getName();
                    // Ensure the client's subject name as seen in the file
                    // is equal to the subject name received from the certificate chain
                    Assert.assertEquals("Expected subject name and actual subject name didn't match", subjName, expectedSubjectName);
                    toggleHandshakeOccured.set(true);
                } catch(Exception e) {
                    e.printStackTrace();
                }
                return serverCallHandler.startCall(serverCall, metadata);
            }
        };
    }

    private static File createFile(String path, byte[] data) throws IOException {
        FileOutputStream key = new FileOutputStream(path);
        key.write(data);
        key.flush();
        key.close();
        File f = new File(path);
        files2Cleanup.add(f);
        return f;
    }

    private static class MockEndorser extends EndorserGrpc.EndorserImplBase {
        public void processProposal(org.hyperledger.fabric.protos.peer.FabricProposal.SignedProposal request,
                                    io.grpc.stub.StreamObserver<org.hyperledger.fabric.protos.peer.FabricProposalResponse.ProposalResponse> responseObserver) {
        responseObserver.onNext(FabricProposalResponse.ProposalResponse.newBuilder().getDefaultInstanceForType());
        responseObserver.onCompleted();
        }
    }

    private static String subjectNameFromPEM(byte[] pemBytes) throws IOException {
        PEMParser parser = new PEMParser(new StringReader(new String(pemBytes)));
        X509CertificateHolder holder = (X509CertificateHolder) parser.readObject();
        return holder.getSubject().toString();
    }
}
