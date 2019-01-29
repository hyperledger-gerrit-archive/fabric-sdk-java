package org.hyperledger.fabric.sdk.token;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.common.Common;
import org.hyperledger.fabric.protos.token.Transaction;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.transaction.ProtoUtils;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

public class ChannelHelper {
    public static Common.Header createHeader(TransactionContext transactionContext) {
        ByteString chdrBytes = Common.ChannelHeader.newBuilder()
                .setType(Common.HeaderType.TOKEN_TRANSACTION.getNumber())
                .setChannelId(transactionContext.getChannelID())
                .setTxId(transactionContext.getTxID())
                .setEpoch(transactionContext.getEpoch())
                .setTimestamp(ProtoUtils.getCurrentFabricTimestamp())
                //                .setTlsCertHash()
                .build()
                .toByteString();

        ByteString shdr = Common.SignatureHeader.newBuilder()
                .setCreator(transactionContext.getIdentity().toByteString())
                .setNonce(transactionContext.getNonce())
                .build()
                .toByteString();

        return Common.Header.newBuilder()
                .setChannelHeader(chdrBytes)
                .setSignatureHeader(shdr)
                .build();
    }

    public static Common.Payload createPayload(Transaction.TokenTransaction tokenTransaction, TransactionContext transactionContext) {
        return Common.Payload.newBuilder()
                .setData(tokenTransaction.toByteString())
                .setHeader(createHeader(transactionContext))
                .build();
    }

    public static Common.Envelope createTransactionEnvelope(Common.Payload transactionPayload, TransactionContext transactionContext) throws CryptoException, InvalidArgumentException {
        return Common.Envelope.newBuilder()
                .setPayload(transactionPayload.toByteString())
                .setSignature(ByteString.copyFrom(transactionContext.sign(transactionPayload.toByteArray())))
                .build();
    }

    public static Common.Envelope createTokenTransactionEnvelope(Transaction.TokenTransaction tokenTransaction, TransactionContext transactionContext) throws CryptoException, InvalidArgumentException {
        return createTransactionEnvelope(createPayload(tokenTransaction, transactionContext), transactionContext);
    }
}
