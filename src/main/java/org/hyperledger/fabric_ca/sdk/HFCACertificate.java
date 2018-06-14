package org.hyperledger.fabric_ca.sdk;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.exception.CertificateException;
import org.hyperledger.fabric_ca.sdk.exception.HTTPException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import static java.lang.String.format;

public class HFCACertificate {

    private String enrollmentID;
    private String serial;
    private String aki;
    private String revokedStart;
    private String revokedEnd;
    private String expiredStart;
    private String expiredEnd;
    private boolean notexpired;
    private boolean notrevoked;

    private HFCAClient client;

    static final String HFCA_CERTIFICATE = HFCAClient.HFCA_CONTEXT_ROOT + "certificates";
    private static final Log logger = LogFactory.getLog(HFCAIdentity.class);

    HFCACertificate(HFCAClient client) throws InvalidArgumentException {
        if (client.getCryptoSuite() == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        this.client = client;
    }

    public void setEnrollmentID(String enrollmentID) {
        this.enrollmentID = enrollmentID;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public void setAki(String aki) {
        this.aki = aki;
    }

    public void setRevokedStart(String revokedStart) {
        this.revokedStart = revokedStart;
    }

    public void setRevokedEnd(String revokedEnd) {
        this.revokedEnd = revokedEnd;
    }

    public void setExpiredStart(String expiredStart) {
        this.expiredStart = expiredStart;
    }

    public void setExpiredEnd(String expiredEnd) {
        this.expiredEnd = expiredEnd;
    }

    public void setNotexpired(boolean notexpired) {
        this.notexpired = notexpired;
    }

    public void setNotrevoked(boolean notrevoked) {
        this.notrevoked = notrevoked;
    }

    /**
     * read retrieves certificates from the fabric ca server based on the filters provided
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return statusCode The HTTP status code in the response
     * @throws CertificateException    if retrieving an certificate fails.
     * @throws InvalidArgumentException Invalid (null) argument specified
     */

    public HFCACertificateResponse read(User registrar) throws CertificateException, InvalidArgumentException {
        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        String readCertURL = HFCA_CERTIFICATE;
        try {
            logger.debug(format("certificate url: %s, registrar: %s", readCertURL, registrar.getName()));

            Map<String, String> queryParms = getQueryParm();
            JsonObject result = client.httpGet(readCertURL, registrar, queryParms);

            int statusCode = result.getInt("statusCode");
            Collection<String> certs = new ArrayList<String>();
            if (statusCode < 400) {
                JsonArray certificates = result.getJsonArray("certs");
                if (certificates != null && !certificates.isEmpty()) {
                    for (int i = 0; i < certificates.size(); i++) {
                        JsonObject cert = certificates.getJsonObject(i);
                        String certPEM = cert.getString("PEM");
                        certs.add(certPEM);
                    }
                }
                logger.debug(format("certificate url: %s, registrar: %s done.", readCertURL, registrar));
            }
            HFCACertificateResponse resp = new HFCACertificateResponse(statusCode, certs);
            return resp;
        }  catch (HTTPException e) {
            String msg = format("[Code: %d] - Error while getting certificates from url '%s': %s", e.getStatusCode(), readCertURL, e.getMessage());
            CertificateException certificateException = new CertificateException(msg, e);
            logger.error(msg);
            throw certificateException;
        } catch (Exception e) {
            String msg = format("Error while getting certificates from url '%s': %s", readCertURL, e.getMessage());
            CertificateException certificateException = new CertificateException(msg, e);
            logger.error(msg);
            throw certificateException;
        }
    }

    private Map<String, String> getQueryParm() {
        Map<String, String> queryParm = new HashMap<String, String>();
        if (this.enrollmentID != null) {
           queryParm.put("id", this.enrollmentID);
        }
        if (this.serial != null) {
            queryParm.put("serial", this.serial);
        }
        if (this.aki != null) {
            queryParm.put("aki", this.aki);
        }
        if (this.revokedStart != null) {
            queryParm.put("revoked_start", this.revokedStart);
        }
        if (this.revokedEnd != null) {
            queryParm.put("revoked_end", this.revokedEnd);
        }
        if (this.expiredStart != null) {
            queryParm.put("expired_start", this.expiredStart);
        }
        if (this.expiredEnd != null) {
            queryParm.put("expired_end", this.expiredEnd);
        }
        queryParm.put("notexpired", String.valueOf(this.notexpired));
        queryParm.put("notrevoked", String.valueOf(this.notrevoked));
        return queryParm;
    }

    public class HFCACertificateResponse {
        private int statusCode;
        private Collection<String> certs;

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

}
