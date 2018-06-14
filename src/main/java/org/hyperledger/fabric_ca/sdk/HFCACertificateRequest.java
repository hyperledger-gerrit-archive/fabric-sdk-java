package org.hyperledger.fabric_ca.sdk;

import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.sdk.helper.Utils;

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

    public void setRevokedStart(String revokedStart) {
        queryParms.put("revoked_start", revokedStart);
    }

    public void setRevokedEnd(String revokedEnd) {
        queryParms.put("revoked_end", revokedEnd);
    }

    public void setExpiredStart(String expiredStart) {
        queryParms.put("expired_start", expiredStart);
    }

    public void setExpiredEnd(String expiredEnd) {
        queryParms.put("expired_end", expiredEnd);
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
