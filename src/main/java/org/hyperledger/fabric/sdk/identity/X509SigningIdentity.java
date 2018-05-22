package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

public class X509SigningIdentity extends X509Identity implements SigningIdentity {

    private CryptoSuite cryptoSuite;

    public X509SigningIdentity(CryptoSuite cryptoSuite, User user) {
        super(user);

        this.cryptoSuite = cryptoSuite;
    }

    @Override
    public byte[] sign(byte[] msg) throws CryptoException {
        return cryptoSuite.sign(super.user.getEnrollment().getKey(), msg);
    }

    @Override
    public boolean verifySignature(byte[] msg, byte[] sig) throws CryptoException {
        throw new CryptoException("Not Implemented yet!!!");
    }

}
