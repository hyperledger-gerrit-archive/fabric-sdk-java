package org.hyperledger.fabric.sdk.identity;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.security.auth.DestroyFailedException;

import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.protos.idemix.Idemix.CredentialRevocationInformation;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.idemix.IdemixCredential;
import org.hyperledger.fabric.sdk.idemix.IdemixIssuerPublicKey;

public class IdemixEnrollment implements Enrollment, PrivateKey {


    protected IdemixIssuerPublicKey ipk;
    protected PublicKey revocationPk;
    protected String mspId;
    protected BIG sk;
    protected IdemixCredential cred;
    protected CredentialRevocationInformation cri;
    protected String ou;
    protected boolean role;
    private KeyPair key;
    private String cert;
    private boolean destroyed = false;

    public IdemixEnrollment(IdemixIssuerPublicKey ipk, PublicKey revocationPk, String mspId, BIG sk, IdemixCredential cred, CredentialRevocationInformation cri, String ou, boolean role) {
        this.ipk = ipk;
        this.revocationPk = revocationPk;
        this.mspId = mspId;
        this.sk = sk;
        this.cred = cred;
        this.cri = cri;
        this.ou = ou;
        this.role = role;
    }

    public PrivateKey getKey() {
        return this;
    }

    public String getCert() {
        return "idemix";
    }

    @Override
    public String getAlgorithm() {
        return "idemix";
    }

    @Override
    public String getFormat() {
        return "idemix";
    }

    @Override
    public byte[] getEncoded() {
        return new byte[0];
    }

    @Override
    public void destroy() throws DestroyFailedException {
        if (destroyed) {
            throw new DestroyFailedException("IdemixEnrollment was already destroyed");
        }
        destroyed = true;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
}
