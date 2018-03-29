package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.protos.msp.Identities;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.transaction.ProtoUtils;

public class X509Identity implements Identity {

    protected User user;

    public X509Identity(User user) {
        this.user = user;
    }

    @Override
    public Identities.SerializedIdentity createSerializedIdentity() {
        return ProtoUtils.createSerializedIdentity(user);
    }
}
