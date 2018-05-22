package org.hyperledger.fabric.sdk.user;

import java.security.PublicKey;
import java.util.Set;

import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.idemix.IdemixCredential;
import org.hyperledger.fabric.sdk.idemix.IdemixIssuerPublicKey;
import org.hyperledger.fabric.sdk.identity.IdemixSigningIdentity;
import org.hyperledger.fabric.sdk.identity.SigningIdentity;

public class IdemixUser implements User {

    protected String mspId;
    protected IdemixIssuerPublicKey ipk;

    protected IdemixCredential cred;
    protected Idemix.CredentialRevocationInformation cri;
    protected BIG sk;
    protected PublicKey revocationPk;

    public IdemixUser(String mspId, IdemixIssuerPublicKey ipk, PublicKey revocationPk, IdemixCredential cred, BIG sk, Idemix.CredentialRevocationInformation cri) {
        this.mspId = mspId;
        this.ipk = ipk;
        this.cri = cri;
        this.cred = cred;
        this.sk = sk;
        this.revocationPk = revocationPk;
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
        try {
            return new IdemixSigningIdentity(ipk, revocationPk, mspId, sk, cred, cri);
        } catch (InvalidArgumentException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }
}
