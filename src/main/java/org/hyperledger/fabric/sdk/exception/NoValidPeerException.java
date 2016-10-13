package org.hyperledger.fabric.sdk.exception;

public class NoValidPeerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public NoValidPeerException(String message) {
		super(message);
	}

}
