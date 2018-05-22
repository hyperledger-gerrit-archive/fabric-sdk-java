package org.hyperledger.fabric.sdk.identity;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.user.IdemixUser;

public class IdentityFactory {
    private IdentityFactory() {
        // private constructor for utility class
    }

    public static SigningIdentity getSigningIdentity(CryptoSuite cryptoSuite, User user) {
        Enrollment enrollment = user.getEnrollment();

        try {
            if (enrollment instanceof X509Enrollment) {
                return new X509SigningIdentity(cryptoSuite, user);
            } else if (user instanceof IdemixUser) {
                return ((IdemixUser)user).getSigningIdentity();
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception exc) {
            throw new RuntimeException("Failed getting signing identity", exc);
        }


        throw new IllegalStateException("Invalid enrollment. Expected X509Enrollment. " + enrollment);
    }

}
