package org.hyperledger.fabric_ca.sdk;

import java.util.Collection;

/**
 * The response from a certificate API request, contains the status code of the
 * request and certificates that were retrieved
 */
public class HFCACertificateResponse {
    private final int statusCode;
    private final Collection<HFCACredential> certs;

    HFCACertificateResponse(int statusCode, Collection<HFCACredential> certs) {
        this.statusCode = statusCode;
        this.certs = certs;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Collection<HFCACredential> getCerts() {
        return certs;
    }
}
