package org.hyperledger.fabric.sdk.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.protos.msp.MspConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.idemix.Credential;
import org.hyperledger.fabric.sdk.idemix.IssuerPublicKey;

public class IdemixUserStore {

    private static final String USER_PATH = "/user/";
    private static final String VERIFIER_PATH = "/msp/";
    private static final String IPK_CONFIG = "IssuerPublicKey";


    protected String storePath;
    protected String mspId;
    protected IssuerPublicKey ipk;

    public IdemixUserStore(String storePath, String mspId) throws CryptoException {
        this.storePath = storePath;
        this.mspId = mspId;

        Idemix.IssuerPublicKey ipkProto =  readIdemixIssuerPublicKey(mspId  + "/" +  VERIFIER_PATH + IPK_CONFIG);
        this.ipk = new IssuerPublicKey(ipkProto);
        if (!ipk.check()) {
            throw new CryptoException("Failed verifying issuer public key.");
        }

    }

    public User getUser(String id) throws IOException {
        MspConfig.IdemixMSPSignerConfig signerConfig = null;
        signerConfig = readIdemixMSPConfig(mspId + "/" + USER_PATH + id);

        BIG sk = BIG.fromBytes(signerConfig.getSk().toByteArray());
        Credential cred = new Credential(Idemix.Credential.parseFrom(signerConfig.getCred()));

        return new IdemixUser(mspId, ipk, cred, sk);
    }

    /**
     * Helper function: parse Idemix MSP Signer config (is part of the MSPConfig proto) from path
     *
     * @param id
     * @return IdemixMSPSignerConfig proto
     */
    protected MspConfig.IdemixMSPSignerConfig readIdemixMSPConfig(String id) throws IOException {
        Path path = Paths.get(storePath + id);
        byte[] data = Files.readAllBytes(path);

        return MspConfig.IdemixMSPSignerConfig.parseFrom(data);
    }

    /**
     * Parse Idemix issuer public key from the config file
     *
     * @param id
     * @return Idemix IssuerPublicKey proto
     */
    protected Idemix.IssuerPublicKey readIdemixIssuerPublicKey(String id) {
        Path path = Paths.get(storePath + id);
        byte[] data = null;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Idemix.IssuerPublicKey ipk = null;

        try {
            ipk = Idemix.IssuerPublicKey.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        return ipk;
    }



}
