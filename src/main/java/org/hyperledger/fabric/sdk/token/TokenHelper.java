package org.hyperledger.fabric.sdk.token;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.transaction.ProtoUtils;

import static org.hyperledger.fabric.protos.token.ProverPeer.*;
import static org.hyperledger.fabric.protos.token.Transaction.TokenTransaction;

public class TokenHelper {

    public static boolean verifySignedCommandResponse(final CommandContext ctx, SignedCommandResponse signedCommandResponse) {
        // TODO implement
        return true;
    }

    public static CommandResponse unmarshalCommandResponse(SignedCommandResponse signedCommandResponse, CommandResponse.PayloadCase expectedCommandResponse) throws TokenException {
        CommandResponse commandResponse;
        try {
            commandResponse = CommandResponse.parseFrom(signedCommandResponse.getResponse());
        } catch (InvalidProtocolBufferException e) {
            throw new TokenException(e);
        }

        if (commandResponse.getPayloadCase() == CommandResponse.PayloadCase.ERR) {
            throw new TokenException(commandResponse.getErr().getMessage());
        }

        if (commandResponse.getPayloadCase() != expectedCommandResponse) {
            throw new TokenException("CommandResponse does not expected response type");
        }

        return commandResponse;
    }

    public static SignedCommand createSignedCommand(final CommandContext ctx, ByteString payload) throws InvalidProtocolBufferException, CryptoException, InvalidArgumentException {

        // unpack command
        Command.Builder commandBuilder = Command.parseFrom(payload).toBuilder();
//        getClientTLSCertificateDigest()

        Header header = createCommandHeader(ctx);

        ByteString raw = commandBuilder
                .setHeader(header)
                .build().toByteString();

        ByteString signature = ctx.sign(raw);

        return SignedCommand.newBuilder()
                .setCommand(raw)
                .setSignature(signature)
                .build();
    }

    public static Header createCommandHeader(final CommandContext ctx) {
        return Header.newBuilder()
                .setTimestamp(ProtoUtils.getCurrentFabricTimestamp())
                .setChannelId(ctx.getChannelID())
                .setNonce(ctx.getNonce())
                .setCreator(ctx.getSerializedIdentity())
                //                .setTlsCertHash()
                .build();
    }

    public static boolean isValid(TokenTransaction tokenTransaction) {
// TODO impl
//        for (ProposalResponse sdkProposalResponse : proposalResponses) {
//            ed.add(sdkProposalResponse.getProposalResponse().getEndorsement());
//            if (proposal == null) {
//                proposal = sdkProposalResponse.getProposal();
//                proposalTransactionID = sdkProposalResponse.getTransactionID();
//                if (proposalTransactionID == null) {
//                    throw new InvalidArgumentException("Proposals with missing transaction ID");
//                }
//                proposalResponsePayload = sdkProposalResponse.getProposalResponse().getPayload();
//                if (proposalResponsePayload == null) {
//                    throw new InvalidArgumentException("Proposals with missing payload.");
//                }
//                transactionContext = sdkProposalResponse.getTransactionContext();
//                if (transactionContext == null) {
//                    throw new InvalidArgumentException("Proposals with missing transaction context.");
//                }
//            } else {
//                final String transactionID = sdkProposalResponse.getTransactionID();
//                if (transactionID == null) {
//                    throw new InvalidArgumentException("Proposals with missing transaction id.");
//                }
//                if (!proposalTransactionID.equals(transactionID)) {
//                    throw new InvalidArgumentException(format("Proposals with different transaction IDs %s,  and %s", proposalTransactionID, transactionID));
//                }
//            }
//        }
        return true;
    }

}
