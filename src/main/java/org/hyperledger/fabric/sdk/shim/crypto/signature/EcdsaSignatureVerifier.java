package org.hyperledger.fabric.sdk.shim.crypto.signature;

import org.hyperledger.fabric.sdk.shim.crypto.CryptoPrimitives;

public class EcdsaSignatureVerifier implements SignatureVerifier {
    private static final String HASH_ALGO = "SHA3";
    private static final int KEYLENGTH = 256;
    private CryptoPrimitives crypto;

    public EcdsaSignatureVerifier() {

        crypto = new CryptoPrimitives(KEYLENGTH, HASH_ALGO);

    }

    @Override
    public boolean verify(byte[] publicKey, byte[] signature, byte[] payload) {
        try {
            return crypto.ecdsaVerify(publicKey, signature, payload);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
}
