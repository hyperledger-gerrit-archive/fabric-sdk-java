package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;

public class X509SigningIdentity extends X509Identity implements SigningIdentity {

    private CryptoPrimitives cryptoPrimitives;

    public X509SigningIdentity(CryptoPrimitives cryptoPrimitives, User user) {
        super(user);

        this.cryptoPrimitives = cryptoPrimitives;
    }

    @Override
    public byte[] sign(byte[] msg) throws CryptoException {
        return cryptoPrimitives.sign(super.user.getEnrollment().getKey(), msg);
    }

    @Override
    public boolean verifySignature(byte[] msg, byte[] sig) throws CryptoException {
        return false;
    }

}
