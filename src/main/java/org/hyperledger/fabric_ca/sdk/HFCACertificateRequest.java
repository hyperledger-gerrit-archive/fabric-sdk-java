package org.hyperledger.fabric_ca.sdk;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.sdk.helper.Utils;

/**
 * Request to the Fabric CA server to get certificates
 * based on filter parameters
 */
public class HFCACertificateRequest {

    private Map<String, String> queryParms = new HashMap<String, String>();

    HFCACertificateRequest() {
    }

    public void setEnrollmentID(String enrollmentID) {
        queryParms.put("id", enrollmentID);
    }

    public void setSerial(String serial) {
        queryParms.put("serial", serial);
    }

    public void setAki(String aki) {
        queryParms.put("aki", aki);
    }

    public void setRevokedStart(Date revokedStart) {
        queryParms.put("revoked_start", Utils.dateToString(revokedStart));
    }

    public void setRevokedEnd(Date revokedEnd) {
        queryParms.put("revoked_end", Utils.dateToString(revokedEnd));
    }

    public void setExpiredStart(Date expiredStart) {
        queryParms.put("expired_start", Utils.dateToString(expiredStart));
    }

    public void setExpiredEnd(Date expiredEnd) {
        queryParms.put("expired_end", Utils.dateToString(expiredEnd));
    }

    public void setNotexpired(boolean notexpired) {
        queryParms.put("notexpired", String.valueOf(notexpired));
    }

    public void setNotrevoked(boolean notrevoked) {
        queryParms.put("notrevoked", String.valueOf(notrevoked));
    }

    public Map<String, String> getQueryParms() {
        return this.queryParms;
    }

}
