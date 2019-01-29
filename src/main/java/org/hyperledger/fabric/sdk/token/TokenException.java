package org.hyperledger.fabric.sdk.token;

public class TokenException extends Exception {
    public TokenException(String message) {
        super(message);
    }

    public TokenException(Throwable cause) {
        super(cause);
    }
}
