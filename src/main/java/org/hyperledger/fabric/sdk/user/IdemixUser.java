package org.hyperledger.fabric.sdk.user;

import java.security.PrivateKey;
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

public class IdemixUser implements User, Enrollment {

    private String name;
    private String mspId;
    private IdemixIssuerPublicKey ipk;
    private IdemixCredential cred;
    private BIG sk;
    private PublicKey revocationPk;
    private Idemix.CredentialRevocationInformation cri;
    private String ou;
    private boolean role;
    private String affiliation;
    private String roles;

    public IdemixUser(String name, String mspId, IdemixIssuerPublicKey ipk, IdemixCredential cred, BIG sk, PublicKey revocationPk, Idemix.CredentialRevocationInformation cri, String ou, boolean role) {
        this.name = name;
        this.mspId = mspId;
        this.ipk = ipk;
        this.cred = cred;
        this.sk = sk;
        this.revocationPk = revocationPk;
        this.ou = ou;
        this.role = role;
        this.cri = cri;
    }

    @Override
    public String getName() {
        return name;
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
        return this.affiliation;
    }

    @Override
    public Enrollment getEnrollment() { return this; }

    @Override
    public String getMspId() {
        return mspId;
    }

    @Override
    public SigningIdentity getSigningIdentity() throws CryptoException {
        try {
            return new IdemixSigningIdentity(ipk, revocationPk, mspId, sk, cred, cri, ou, role);
        } catch (InvalidArgumentException e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    @Override
    public PrivateKey getKey() { return null; }

    @Override
    public String getCert() { return null; }
}
