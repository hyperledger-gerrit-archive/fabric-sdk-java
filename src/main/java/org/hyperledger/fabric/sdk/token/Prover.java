package org.hyperledger.fabric.sdk.token;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hyperledger.fabric.protos.token.ProverPeer.*;
import static org.hyperledger.fabric.protos.token.Transaction.TokenTransaction;

public interface Prover {

    /**
     * RequestImport allows the client to submit an issue request to a prover peer service;
     * the function takes as parameters tokensToIssue and the signing identity of the client;
     * it returns a response in bytes and an error message in the case the request fails.
     * The response corresponds to a serialized TokenTransaction protobuf message.
     *
     * @param tokensToIssue
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<TokenTransaction> requestImport(List<TokenToIssue> tokensToIssue);

    /**
     * RequestTransfer allows the client to submit a transfer request to a prover peer service;
     * the function takes as parameters a fabtoken application credential, the identifiers of the tokens
     * to be transfererd and the shares describing how they are going to be distributed
     * among recipients; it returns a response in bytes and an error message in the case the
     * request fails.
     *
     * @param tokenIDs
     * @param shares
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<TokenTransaction> requestTransfer(List<ByteString> tokenIDs, List<RecipientTransferShare> shares);

    /**
     * RequestRedeem allows the redemption of the tokens in the input tokenIDs
     * It queries the ledger to read detail for each token id.
     * It creates a token transaction with an output for redeemed tokens and
     * possibly another output to transfer the remaining tokens, if any, to the same user.
     *
     * @param tokenIds
     * @param quantity
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<TokenTransaction> requestRedeem(List<ByteString> tokenIds, long quantity);

    /**
     * RequestSwap allows the client to submit an atomic swap request to a prover peer service.
     *
     * @param tokenIds
     * @param terms
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<TokenTransaction> requestSwap(List<ByteString> tokenIds, List<PlainSwapTerms> terms);

    /**
     * ListTokens allows the client to submit a list request to a prover peer service;
     * it returns a list of TokenOutput and an error message in the case the request fails.
     *
     * @return
     * @throws TokenException
     */
    CompletableFuture<List<TokenOutput>> listTokens();

    /**
     * RequestApprove allows the client to submit an approve request to a prover peer service;
     * the function takes as parameters a fabtoken application credential, the identifiers of the tokens
     * to be delegated to third parties and the allowance shares describing how they are going to be distributed
     * among delegatee; it returns a response in bytes and an error message in the case the
     * request fails.
     *
     * @param tokenIds
     * @param shares
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<TokenTransaction> requestApprove(List<ByteString> tokenIds, List<AllowanceRecipientShare> shares);

    /**
     * RequestTransferFrom allows the client to submit a transferFrom request to a prover peer service;
     * the function takes as parameters a fabtoken application credential, the identifiers of the tokens
     * to be delegated to third parties and the shares describing how they are going to be distributed
     * among recipients; it returns a response in bytes and an error message in the case the
     * request fails.
     *
     * @param tokenIds
     * @param shares
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<TokenTransaction> requestTransferFrom(List<ByteString> tokenIds, List<RecipientTransferShare> shares);

    /**
     * RequestAllowance allows the client to submit an allowance request to a prover peer service;
     * it returns a response in bytes and an error message in the case the request fails.
     *
     * @param delegatorIdentity
     * @return
     * @throws TokenException
     * @throws InvalidArgumentException
     */
    CompletableFuture<List<TokenOutput>> requestAllowance(ByteString delegatorIdentity);

}
