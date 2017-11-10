/*
 *
 *  Copyright 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hyperledger.fabric.sdk.idemix;

import com.google.protobuf.ByteString;
import com.ibm.zurich.amcl.BN254.BIG;
import com.ibm.zurich.amcl.BN254.ECP;
import com.ibm.zurich.amcl.BN254.FP12;
import com.ibm.zurich.amcl.BN254.PAIR;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

import java.util.Arrays;

import static org.hyperledger.fabric.sdk.idemix.Utils.FieldBytes;

/**
 * Signature represents an idemix signature, which is a zero knowledge proof
 * of knowledge of a BBS+ signature. The ZK proof is taken from http:/ia.cr/2016/663.pdf.
 */
public class Signature {

	private ECP aPrime;
	private ECP aBar;
	private ECP bPrime;
	private BIG proofC;
	private BIG proofSSk;
	private BIG proofSE;
	private BIG proofSR2;
	private BIG proofSR3;
	private BIG proofSSPrime;
	private BIG[] proofSAttrs;
	private BIG nonce;
	private ECP nym;
	private BIG proofSRNym;

	public static final String SIGN_LABEL = "sign";
	
	/**
	 * Some attributes may be hidden, some disclosed. The indices of the hidden attributes will be passed.
	 * @param Disclosure	an array of length of attributes with 0 and 1 for hide and disclose
	 * @return an integer array of the hidden indices
	 */
	public int[] hiddenIndices(byte[] Disclosure){
		
		int counter = 0;
		for (int i = 0; i < Disclosure.length; i++){
			if (Disclosure[i] == 0){
				counter++;
			}
		}
		
		int[] HiddenIndices = new int[counter];
				
		for (int i = 0; i < Disclosure.length; i++){
			if (Disclosure[i] == 0){
				HiddenIndices[i] = i;
			}
		}
		return HiddenIndices;
	}

	/**
	 * Creates a new Signature by proving knowledge of a credential
	 */
	public Signature(Credential c, BIG sk, ECP Nym, BIG RNym, IssuerPublicKey ipk, byte[] Disclosure, byte[] msg, RAND rng){
		
		int[] HiddenIndices = hiddenIndices(Disclosure);
		
		// Start signature
		BIG r1 = Utils.RandModOrder(rng);
		BIG r2 = Utils.RandModOrder(rng);
		BIG r3 = new BIG(r1);
		r3.invmodp(Utils.GroupOrder);

		this.nonce = Utils.RandModOrder(rng);
		
		this.aPrime = PAIR.G1mul(c.getA(), r1);
		this.aBar = PAIR.G1mul(c.getB(), r1);
		this.aBar.sub(PAIR.G1mul(aPrime, c.getE()));

		this.bPrime = PAIR.G1mul(c.getB(), r1);
		this.bPrime.sub(PAIR.G1mul(ipk.HRand, r2));
		BIG SPrime = new BIG(c.getS());
		SPrime.add(BIG.modneg(BIG.modmul(r2, r3, Utils.GroupOrder), Utils.GroupOrder));
		SPrime.mod(Utils.GroupOrder);
				
		//Construct Zero Knowledge Proof
		BIG rsk = Utils.RandModOrder(rng);
		BIG re = Utils.RandModOrder(rng);
		BIG rR2 = Utils.RandModOrder(rng);
		BIG rR3 = Utils.RandModOrder(rng);
		BIG rSPrime = Utils.RandModOrder(rng);
		BIG rRNym = Utils.RandModOrder(rng);
		BIG[] rAttrs = new BIG[HiddenIndices.length];
		for (int i = 0; i < HiddenIndices.length; i++){
			rAttrs[i] = Utils.RandModOrder(rng);
		}
		
		ECP t1 = this.aPrime.mul2(re, ipk.HRand, rR2);
		ECP t2 = PAIR.G1mul(ipk.HRand, rSPrime);
		t2.add(bPrime.mul2(rR3, ipk.Hsk, rsk));
		
		for (int i = 0; i < HiddenIndices.length/2; i++){
			t2.add(ipk.HAttrs[HiddenIndices[2*i]].mul2(rAttrs[2*i], ipk.HAttrs[HiddenIndices[2*i+1]], rAttrs[2*i+1]));
		}
		if (HiddenIndices.length%2 != 0){
			t2.add(PAIR.G1mul(ipk.HAttrs[HiddenIndices[HiddenIndices.length-1]], rAttrs[HiddenIndices.length-1]));
		}
				
		ECP t3 = ipk.Hsk.mul2(rsk, ipk.HRand, rRNym);

		// create proofData such that it can contain the sign label, 7 elements in G1 (each of size 2*FieldBytes+1),
        // the ipk hash, the disclosure array, and the message
		byte[] proofData = new byte[SIGN_LABEL.getBytes().length + 7 * (2 * Utils.FieldBytes + 1) + ipk.Hash.length + Disclosure.length + msg.length];
		int index = 0;
        index = Utils.append(proofData, index, SIGN_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.EcpToBytes(t1));
        index = Utils.append(proofData, index, Utils.EcpToBytes(t2));
        index = Utils.append(proofData, index, Utils.EcpToBytes(t3));
        index = Utils.append(proofData, index, Utils.EcpToBytes(aPrime));
        index = Utils.append(proofData, index, Utils.EcpToBytes(aBar));
        index = Utils.append(proofData, index, Utils.EcpToBytes(bPrime));
        index = Utils.append(proofData, index, Utils.EcpToBytes(Nym));
        index = Utils.append(proofData, index, ipk.Hash);
        index = Utils.append(proofData, index, Disclosure);
        index = Utils.append(proofData, index, msg);

		BIG cvalue = Utils.HashModOrder(proofData);

        byte[] finalProofData = new byte[2 * Utils.FieldBytes];
        index = 0;
        index = Utils.append(finalProofData, index, Utils.BigToBytes(cvalue));
        index = Utils.append(finalProofData, index, Utils.BigToBytes(this.nonce));

		this.proofC = Utils.HashModOrder(finalProofData);

		this.proofSSk = new BIG(rsk);
		this.proofSSk.add(BIG.modmul(this.proofC, sk, Utils.GroupOrder));
		this.proofSSk.mod(Utils.GroupOrder);

		this.proofSE = new BIG(re);
		this.proofSE.add(BIG.modneg(BIG.modmul(this.proofC, c.getE(), Utils.GroupOrder), Utils.GroupOrder));
		this.proofSE.mod(Utils.GroupOrder);

		this.proofSR2 = new BIG(rR2);
		this.proofSR2.add(BIG.modmul(this.proofC, r2, Utils.GroupOrder));
		this.proofSR2.mod(Utils.GroupOrder);

		this.proofSR3 = new BIG(rR3);
		this.proofSR3.add(BIG.modneg(BIG.modmul(this.proofC, r3, Utils.GroupOrder), Utils.GroupOrder));
		this.proofSR3.mod(Utils.GroupOrder);

		this.proofSSPrime = new BIG(rSPrime);
		this.proofSSPrime.add(BIG.modmul(this.proofC, SPrime, Utils.GroupOrder));
		this.proofSSPrime.mod(Utils.GroupOrder);

		this.proofSRNym = new BIG(rRNym);
		this.proofSRNym.add(BIG.modmul(this.proofC, RNym, Utils.GroupOrder));
		this.proofSRNym.mod(Utils.GroupOrder);

		this.nym = new ECP();
		this.nym.copy(Nym);
		
		this.proofSAttrs = new BIG[HiddenIndices.length];
		byte[] b = new byte[FieldBytes];
		for (int i = 0; i < HiddenIndices.length; i++){
			this.proofSAttrs[i] = new BIG(rAttrs[i]);
			this.proofSAttrs[i].add(BIG.modmul(this.proofC, BIG.fromBytes(c.getAttrs()[HiddenIndices[i]]), Utils.GroupOrder));
		}
		
	}

	/**
	 * Construct a new signature from a serialized Signature
	 */
	public Signature(Idemix.Signature proto) {
		this.aBar = Utils.transformFromProto(proto.getABar());
		this.aPrime = Utils.transformFromProto(proto.getAPrime());
		this.bPrime = Utils.transformFromProto(proto.getBPrime());
		this.nym = Utils.transformFromProto(proto.getNym());
		this.proofC = BIG.fromBytes(proto.getProofC().toByteArray());
		this.proofSSk = BIG.fromBytes(proto.getProofSSk().toByteArray());
		this.proofSE = BIG.fromBytes(proto.getProofSE().toByteArray());
		this.proofSR2 = BIG.fromBytes(proto.getProofSR2().toByteArray());
		this.proofSR3 = BIG.fromBytes(proto.getProofSR3().toByteArray());
		this.proofSSPrime = BIG.fromBytes(proto.getProofSSPrime().toByteArray());
		this.proofSRNym = BIG.fromBytes(proto.getProofSRNym().toByteArray());
		this.nonce = BIG.fromBytes(proto.getNonce().toByteArray());
		this.proofSAttrs = new BIG[proto.getProofSAttrsCount()];
		for (int i = 0; i < proto.getProofSAttrsCount(); i++) {
			this.proofSAttrs[i] = BIG.fromBytes(proto.getProofSAttrs(i).toByteArray());
		}
	}

	/**
	 * Verify this signature
	 *
	 * @param Disclosure	an array indicating which attributes it expects to be disclosed
	 * @param ipk			the issuer public key
	 * @param msg			the message that should be signed in this signature
	 * @param attributeValues	BIG array with attributeValues[i] contains the desired attribute value for the i-th undisclosed attribute in Disclosure
	 * @return true iff valid
	 */
	public boolean Ver(byte[] Disclosure, IssuerPublicKey ipk, byte[] msg, BIG[] attributeValues){
		
		int[] HiddenIndices = hiddenIndices(Disclosure);
		
		if (this.proofSAttrs.length != HiddenIndices.length){
			return false;
		}
		
		if (this.aPrime.is_infinity()){
			return false;
		}
		
		FP12 temp1 = PAIR.ate(ipk.W, this.aPrime);
		FP12 temp2 = PAIR.ate(Utils.GenG2, this.aBar);
		temp2.inverse();
		temp1.mul(temp2);
		if (!PAIR.fexp(temp1).isunity()){
			return false;
		}
		
		ECP t1 = this.aPrime.mul2(this.proofSE, ipk.HRand, this.proofSR2);
		ECP temp = new ECP();
		temp.copy(this.aBar);
		temp.sub(this.bPrime);
		t1.sub(PAIR.G1mul(temp, this.proofC));

		ECP t2 = PAIR.G1mul(ipk.HRand, this.proofSSPrime);
		t2.add(this.bPrime.mul2(this.proofSR3, ipk.Hsk, this.proofSSk));
		
		for (int i = 0; i < HiddenIndices.length/2; i++){
			t2.add(ipk.HAttrs[HiddenIndices[2*i]].mul2(this.proofSAttrs[2*i], ipk.HAttrs[HiddenIndices[2*i+1]], this.proofSAttrs[2*i+1]));
		}
		if (HiddenIndices.length%2 != 0){
			t2.add(PAIR.G1mul(ipk.HAttrs[HiddenIndices[HiddenIndices.length-1]], this.proofSAttrs[HiddenIndices.length-1]));
		}
		
		temp = new ECP();
		temp.copy(Utils.GenG1);

		for(int i = 0; i < Disclosure.length; i++){
			if (Disclosure[i] != 0){
				temp.add(PAIR.G1mul(ipk.HAttrs[i], attributeValues[i]));
			}
		}
		t2.add(PAIR.G1mul(temp, this.proofC));
		
		ECP t3 = ipk.Hsk.mul2(this.proofSSk, ipk.HRand, this.proofSRNym);
		t3.sub(this.nym.mul(this.proofC));

        // create proofData such that it can contain the sign label, 7 elements in G1 (each of size 2*FieldBytes+1),
        // the ipk hash, the disclosure array, and the message
        byte[] proofData = new byte[SIGN_LABEL.getBytes().length + 7 * (2 * Utils.FieldBytes + 1) + ipk.Hash.length + Disclosure.length + msg.length];
        int index = 0;
        index = Utils.append(proofData, index, SIGN_LABEL.getBytes());
        index = Utils.append(proofData, index, Utils.EcpToBytes(t1));
        index = Utils.append(proofData, index, Utils.EcpToBytes(t2));
        index = Utils.append(proofData, index, Utils.EcpToBytes(t3));
        index = Utils.append(proofData, index, Utils.EcpToBytes(aPrime));
        index = Utils.append(proofData, index, Utils.EcpToBytes(aBar));
        index = Utils.append(proofData, index, Utils.EcpToBytes(bPrime));
        index = Utils.append(proofData, index, Utils.EcpToBytes(nym));
        index = Utils.append(proofData, index, ipk.Hash);
        index = Utils.append(proofData, index, Disclosure);
        index = Utils.append(proofData, index, msg);

        BIG cvalue = Utils.HashModOrder(proofData);

        byte[] finalProofData = new byte[2 * Utils.FieldBytes];
        index = 0;
        index = Utils.append(finalProofData, index, Utils.BigToBytes(cvalue));
        index = Utils.append(finalProofData, index, Utils.BigToBytes(this.nonce));

		byte[] hashedProofData = Utils.BigToBytes(Utils.HashModOrder(finalProofData));
		if (!Arrays.equals(Utils.BigToBytes(this.proofC), hashedProofData)) {
			return false;
		}
		return true;
	}

    /**
     * Convert this signature to a proto
     */
	public Idemix.Signature toProto() {
		Idemix.Signature.Builder builder = Idemix.Signature.newBuilder()
				.setAPrime(Utils.transformToProto(this.aPrime))
				.setABar(Utils.transformToProto(this.aBar))
				.setBPrime(Utils.transformToProto(this.bPrime))
				.setNym(Utils.transformToProto(this.nym))
				.setProofC(ByteString.copyFrom(Utils.BigToBytes(this.proofC)))
				.setProofSSk(ByteString.copyFrom(Utils.BigToBytes(this.proofSSk)))
				.setProofSE(ByteString.copyFrom(Utils.BigToBytes(this.proofSE)))
				.setProofSR2(ByteString.copyFrom(Utils.BigToBytes(this.proofSR2)))
				.setProofSR3(ByteString.copyFrom(Utils.BigToBytes(this.proofSR3)))
				.setProofSRNym(ByteString.copyFrom(Utils.BigToBytes(this.proofSRNym)))
				.setProofSSPrime(ByteString.copyFrom(Utils.BigToBytes(this.proofSSPrime)))
				.setNonce(ByteString.copyFrom(Utils.BigToBytes(this.nonce)));

		for (BIG attr : this.proofSAttrs) {
			builder.addProofSAttrs(ByteString.copyFrom(Utils.BigToBytes(attr)));
		}

		return builder.build();
	}
}
