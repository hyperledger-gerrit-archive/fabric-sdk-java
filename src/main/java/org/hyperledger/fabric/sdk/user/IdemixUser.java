package org.hyperledger.fabric.sdk.user;

import java.util.Set;

import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.idemix.Credential;
import org.hyperledger.fabric.sdk.idemix.IssuerPublicKey;
import org.hyperledger.fabric.sdk.identity.IdemixSigningIdentity;
import org.hyperledger.fabric.sdk.identity.SigningIdentity;

public class IdemixUser implements User {

    protected String mspId;
    protected IssuerPublicKey ipk;

    protected Credential cred;
    protected BIG sk;

    public IdemixUser(String mspId, IssuerPublicKey ipk, Credential cred, BIG sk) {
        this.mspId = mspId;
        this.ipk = ipk;

        this.cred = cred;
        this.sk = sk;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<String> getRoles() {
        return null;
    }

    @Override
    public String getAccount() {
        return null;
    }

    @Override
    public String getAffiliation() {
        return null;
    }

    @Override
    public Enrollment getEnrollment() {
        return null;
    }

    @Override
    public String getMspId() {
        return mspId;
    }

    @Override
    public SigningIdentity getSigningIdentity() throws CryptoException {
        return new IdemixSigningIdentity(ipk, mspId, sk, cred);
    }
}
