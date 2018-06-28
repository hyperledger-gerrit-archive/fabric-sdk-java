package org.hyperledger.fabric.sdk.identity;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.security.auth.DestroyFailedException;

import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.protos.idemix.Idemix.CredentialRevocationInformation;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.idemix.IdemixCredential;
import org.hyperledger.fabric.sdk.idemix.IdemixIssuerPublicKey;

public class IdemixEnrollment implements Enrollment {

    private static final String algo = "idemix";

    protected IdemixIssuerPublicKey ipk;
    protected PublicKey revocationPk;
    protected String mspId;
    protected BIG sk;
    protected IdemixCredential cred;
    protected CredentialRevocationInformation cri;
    protected String ou;
    protected boolean role;
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
        return null;
    }

    public String getCert() {
        return null;
    }
}
