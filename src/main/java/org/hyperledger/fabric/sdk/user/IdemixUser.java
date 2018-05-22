package org.hyperledger.fabric.sdk.user;

import java.util.Set;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.identity.IdemixEnrollment;

public class IdemixUser implements User {

    protected String name;
    protected String mspId;

    protected IdemixEnrollment enrollment;

    public IdemixUser(String name, String mspId, IdemixEnrollment enrollment) {
        this.name = name;
        this.mspId = mspId;
        this.enrollment = enrollment;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<String> getRoles() {
        return null;
    }

    @Override
    public String getAccount() {
        return null;
    }

    @Override
    public String getAffiliation() {
        return null;
    }

    @Override
    public Enrollment getEnrollment() {
        return enrollment;
    }

    @Override
    public String getMspId() {
        return mspId;
    }

}
