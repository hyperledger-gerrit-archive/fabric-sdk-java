package org.hyperledger.fabric.sdk.token;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.identity.SigningIdentity;

import static org.hyperledger.fabric.sdk.helper.Utils.generateNonce;

public class CommandContext {

    private final SigningIdentity signingIdentity;
    private final ByteString serializedIdentity;
    private final String channelID;

    public CommandContext(SigningIdentity signingIdentity, String channelID) {
        this.signingIdentity = signingIdentity;
        this.serializedIdentity = signingIdentity.createSerializedIdentity().toByteString();
        this.channelID = channelID;
    }

    public ByteString getSerializedIdentity() {
        return serializedIdentity;
    }

    public String getChannelID() {
        return channelID;
    }

    public ByteString getNonce() {
        return ByteString.copyFrom(generateNonce());
    }

    public ByteString sign(ByteString raw) throws CryptoException, InvalidArgumentException {
        return ByteString.copyFrom(signingIdentity.sign(raw.toByteArray()));
    }
}
