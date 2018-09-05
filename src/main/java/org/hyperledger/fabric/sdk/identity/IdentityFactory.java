package org.hyperledger.fabric.sdk.identity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.transaction.TransactionContext;

public class IdentityFactory {
    private IdentityFactory() {
        // private constructor for utility class
    }
    private static final Log logger = LogFactory.getLog(TransactionContext.class);
    public static SigningIdentity getSigningIdentity(CryptoSuite cryptoSuite, User user) {
        Enrollment enrollment = user.getEnrollment();

        try {
            if (enrollment instanceof X509Enrollment) {
                logger.trace("x509 signing");
                return new X509SigningIdentity(cryptoSuite, user);
            }

            if (enrollment instanceof IdemixEnrollment) {
                logger.trace("idemix signing");
                return new IdemixSigningIdentity((IdemixEnrollment) enrollment);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        throw new IllegalStateException("Invalid enrollment. Expected either X509Enrollment or IdemixEnrollment." + enrollment);
    }

}
