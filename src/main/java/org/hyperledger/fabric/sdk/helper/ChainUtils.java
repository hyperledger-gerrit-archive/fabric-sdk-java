package org.hyperledger.fabric.sdk.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;

public class ChainUtils {
	 /**
     * used asn1 and get hash
     * @param blockNumber
     * @param previousHash
     * @param dataHash
     * @return byte[]
     * @throws IOException
     */
    public static byte[] getAns1Bytes(long blockNumber, byte[] previousHash, byte[] dataHash) throws IOException{

    	ByteArrayOutputStream s = new ByteArrayOutputStream();
    	DERSequenceGenerator seq = new DERSequenceGenerator(s);
    	//order by blockNumber,PreviousHash,DataHash
        seq.addObject(new ASN1Integer(blockNumber));
        seq.addObject(new DEROctetString(previousHash));
        seq.addObject(new DEROctetString(dataHash));
        seq.close();
        byte[] ret = s.toByteArray();
        return ret;
    }
}
