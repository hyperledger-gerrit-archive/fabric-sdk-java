package org.hyperledger.fabric.sdk;

import com.google.common.util.concurrent.ListenableFuture;
import com.spotify.futures.CompletableFuturesExtra;
import io.grpc.ManagedChannel;
import org.hyperledger.fabric.protos.token.ProverGrpc;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.hyperledger.fabric.protos.token.ProverPeer.SignedCommand;
import static org.hyperledger.fabric.protos.token.ProverPeer.SignedCommandResponse;


public class ProverClient {

    // NOTE try to reuse EndorerClient as much as possible

    private final ProverGrpc.ProverBlockingStub blockingStub;
    private final ProverGrpc.ProverFutureStub stub;


    public static ProverClient createProverClient(String url, Properties properties) {
        Endpoint endpoint = Endpoint.createEndpoint(url, properties);
        return new ProverClient(endpoint.getChannelBuilder().build());
    }

    // we need to get the grpcChannel from the peer
    private ProverClient(ManagedChannel grpcChannel) {
        this.blockingStub = ProverGrpc.newBlockingStub(grpcChannel);
        this.stub = ProverGrpc.newFutureStub(grpcChannel);
    }

    public CompletableFuture<SignedCommandResponse> sendCommandAsync(SignedCommand sc) {
            ListenableFuture<SignedCommandResponse> signedResponseFuture = stub.processCommand(sc);
            return CompletableFuturesExtra.toCompletableFuture(signedResponseFuture);
    }

    public SignedCommandResponse sendCommand(SignedCommand sc) {
        return blockingStub.processCommand(sc);
    }

}
