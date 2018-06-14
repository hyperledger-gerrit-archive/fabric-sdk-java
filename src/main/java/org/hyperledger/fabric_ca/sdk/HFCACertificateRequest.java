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
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric_ca.sdk.exception.CertificateException;
import org.hyperledger.fabric_ca.sdk.exception.HTTPException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;

import static java.lang.String.format;

public class HFCACertificateRequest {

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
    private static final Log logger = LogFactory.getLog(HFCACertificateRequest.class);

    HFCACertificateRequest(HFCAClient client) throws InvalidArgumentException {
        if (client == null) {
            throw new InvalidArgumentException("Client not set.");
        }

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

        try {
            logger.debug(format("certificate url: %s, registrar: %s", HFCA_CERTIFICATE, registrar.getName()));

            Map<String, String> queryParms = getQueryParm();
            JsonObject result = client.httpGet(HFCA_CERTIFICATE, registrar, queryParms);

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
                logger.debug(format("certificate url: %s, registrar: %s done.", HFCA_CERTIFICATE, registrar));
            }
            return new HFCACertificateResponse(statusCode, certs);
        }  catch (HTTPException e) {
            String msg = format("[Code: %d] - Error while getting certificates from url '%s': %s", e.getStatusCode(), HFCA_CERTIFICATE, e.getMessage());
            CertificateException certificateException = new CertificateException(msg, e);
            logger.error(msg);
            throw certificateException;
        } catch (Exception e) {
            String msg = format("Error while getting certificates from url '%s': %s", HFCA_CERTIFICATE, e.getMessage());
            CertificateException certificateException = new CertificateException(msg, e);
            logger.error(msg);
            throw certificateException;
        }
    }

    private Map<String, String> getQueryParm() {
        Map<String, String> queryParm = new HashMap<String, String>();
        if (!Utils.isNullOrEmpty(enrollmentID)) {
           queryParm.put("id", enrollmentID);
        }
        if (!Utils.isNullOrEmpty(serial)) {
            queryParm.put("serial", serial);
        }
        if (!Utils.isNullOrEmpty(aki)) {
            queryParm.put("aki", this.aki);
        }
        if (!Utils.isNullOrEmpty(revokedStart)) {
            queryParm.put("revoked_start", revokedStart);
        }
        if (!Utils.isNullOrEmpty(revokedEnd)) {
            queryParm.put("revoked_end", revokedEnd);
        }
        if (!Utils.isNullOrEmpty(expiredStart)) {
            queryParm.put("expired_start", expiredStart);
        }
        if (!Utils.isNullOrEmpty(expiredEnd)) {
            queryParm.put("expired_end", expiredEnd);
        }
        queryParm.put("notexpired", String.valueOf(notexpired));
        queryParm.put("notrevoked", String.valueOf(notrevoked));
        return queryParm;
    }



}
