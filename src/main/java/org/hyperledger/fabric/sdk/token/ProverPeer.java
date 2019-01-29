package org.hyperledger.fabric.sdk.token;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.sdk.ProverClient;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.identity.SigningIdentity;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.hyperledger.fabric.protos.token.ProverPeer.*;
import static org.hyperledger.fabric.protos.token.ProverPeer.CommandResponse.PayloadCase.TOKEN_TRANSACTION;
import static org.hyperledger.fabric.protos.token.ProverPeer.CommandResponse.PayloadCase.UNSPENT_TOKENS;
import static org.hyperledger.fabric.protos.token.Transaction.TokenTransaction;
import static org.hyperledger.fabric.sdk.token.TokenHelper.*;

// same API as prover.go
public class ProverPeer implements Prover {

//    private static final Log logger = LogFactory.getLog(ProverPeer.class);
    private final CommandContext ctx;
    private final ProverClient proverClient;

    public ProverPeer(SigningIdentity signingIdentity, String channelName, String url, Properties properties) {
        this.ctx = new CommandContext(signingIdentity, channelName);
        this.proverClient = ProverClient.createProverClient(url, properties);
    }

    private CompletableFuture<CommandResponse> sendCommandAsync(Command command, final CommandResponse.PayloadCase expectedCommandResponse) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return createSignedCommand(ctx, command.toByteString());
            } catch (InvalidProtocolBufferException | CryptoException | InvalidArgumentException e) {
                throw new CompletionException(e);
            }
        }).thenCompose(proverClient::sendCommandAsync)
                .thenApplyAsync(scr -> {
                    if (!verifySignedCommandResponse(ctx, scr)) {
                        throw new CompletionException(new TokenException("CommandResponse signature verification failed"));
                    }
                    try {
                        return unmarshalCommandResponse(scr, expectedCommandResponse);
                    } catch (TokenException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<TokenTransaction> requestImport(List<TokenToIssue> tokensToIssue) {
        return CompletableFuture.supplyAsync(() -> {
            ImportRequest.Builder importRequestBuilder = ImportRequest.newBuilder();
            tokensToIssue.forEach(importRequestBuilder::addTokensToIssue);
            return Command.newBuilder().setImportRequest(importRequestBuilder).build();
        }).thenCompose(cmd -> sendCommandAsync(cmd, TOKEN_TRANSACTION)
                .thenApplyAsync(CommandResponse::getTokenTransaction));
    }

    @Override
    public CompletableFuture<TokenTransaction> requestTransfer(List<ByteString> tokenIDs, List<RecipientTransferShare> shares) {
        return null;
    }

    @Override
    public CompletableFuture<TokenTransaction> requestRedeem(List<ByteString> tokenIds, long quantity) {
        return null;
    }

    @Override
    public CompletableFuture<TokenTransaction> requestSwap(List<ByteString> tokenIds, List<PlainSwapTerms> terms) {
        return null;
    }


    @Override
    public CompletableFuture<List<TokenOutput>> listTokens() {
        return CompletableFuture.supplyAsync(() -> Command.newBuilder().setListRequest(ListRequest.newBuilder()).build())
                .thenCompose(cmd -> sendCommandAsync(cmd, UNSPENT_TOKENS)
                        .thenApplyAsync(cmdResponse -> cmdResponse.getUnspentTokens().getTokensList()));
    }

    @Override
    public CompletableFuture<TokenTransaction> requestApprove(List<ByteString> tokenIds, List<AllowanceRecipientShare> shares) {
        return null;
    }

    @Override
    public CompletableFuture<TokenTransaction> requestTransferFrom(List<ByteString> tokenIds, List<RecipientTransferShare> shares) {
        return null;
    }

    @Override
    public CompletableFuture<List<TokenOutput>> requestAllowance(ByteString delegatorIdentity) {
        return null;
    }


}
