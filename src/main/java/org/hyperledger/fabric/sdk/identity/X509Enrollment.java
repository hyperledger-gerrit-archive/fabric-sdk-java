package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.sdk.Enrollment;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;

public class X509Enrollment implements Enrollment, Serializable {

    private PrivateKey key;
    private String cert;

    public X509Enrollment(KeyPair signingKeyPair, String signedPem) {
        key = signingKeyPair.getPrivate();
        this.cert = signedPem;
    }

    public X509Enrollment(PrivateKey key, String signedPem) {
        this.key = key;
        this.cert = signedPem;
    }

    public PrivateKey getKey() {
        return key;
    }

    public String getCert() {
        return cert;
    }

}
