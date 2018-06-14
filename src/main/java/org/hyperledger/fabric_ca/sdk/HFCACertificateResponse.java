package org.hyperledger.fabric_ca.sdk;

import java.util.Collection;

public class HFCACertificateResponse {
    private final int statusCode;
    private final Collection<String> certs;

    HFCACertificateResponse(int statusCode, Collection<String> certs) {
        this.statusCode = statusCode;
        this.certs = certs;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Collection<String> getCerts() {
        return certs;
    }
}
