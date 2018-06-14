package org.hyperledger.fabric_ca.sdk;

import java.util.Collection;

public class HFCACertificateResponse {
    private final int statusCode;
    private final Collection<HFCACertificate> certs;

    HFCACertificateResponse(int statusCode, Collection<HFCACertificate> certs) {
        this.statusCode = statusCode;
        this.certs = certs;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Collection<HFCACertificate> getCerts() {
        return certs;
    }
}
