package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.util.HashMap;
import java.util.Map;

public class IdentityFactory {
    private IdentityFactory() {
    }


    private static Map<String, SigningIdentity> signingIdentityMap = new HashMap<>();

    public synchronized static void addSigningIdentity(String id, SigningIdentity signingIdentity) {
        signingIdentityMap.put(id, signingIdentity);
    }

    public synchronized static SigningIdentity getSigningIdentity(String id) {
        return signingIdentityMap.remove(id);
    }

    public static SigningIdentity getSigningIdentity(CryptoSuite cryptoSuite, User user) {
        Enrollment enrollment = user.getEnrollment();

        try {
            if (enrollment instanceof X509Enrollment) {
                return new X509SigningIdentity(cryptoSuite, user);
            } else if (enrollment instanceof IdemixEnrollment) {
                return new IdemixSigningIdentity((IdemixEnrollment) enrollment);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        throw new IllegalStateException("Invalid enrollment. Expected either X509Enrollment or IdemixEnrollment." + enrollment);
    }

}
