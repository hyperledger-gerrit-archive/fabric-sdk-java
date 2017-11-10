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
import com.ibm.zurich.amcl.BN254.ECP2;
import com.ibm.zurich.amcl.BN254.PAIR;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;
import org.hyperledger.fabric.sdk.exception.CryptoException;

/**
 * Credential represents a user's idemix credential,
 * which is a BBS+ signature (see "Constant-Size Dynamic k-TAA" by Man Ho Au, Willy Susilo, Yi Mu)
 * on the user's secret key and attribute values.
 */
public class Credential {

    private ECP A;
    private ECP B;
    private BIG E;
    private BIG S;
    private byte[][] Attrs;

    /**
     * Constructor a new credential
     *
     * @param key   the issuer key pair
     * @param m     a credential request
     * @param attrs an array of attribute values as BIG
     * @param rng   a random number generator
     */
    public Credential(IssuerKey key, CredRequest m, BIG[] attrs, RAND rng) throws CryptoException {
        if (attrs.length != key.Ipk.AttributeNames.length) {
            throw new CryptoException("Amount of attribute values does not match amount of attributes in issuer public key");
        }

        // Place a BBS+ signature on the user key and the attribute values
        // (For BBS+, see "Constant-Size Dynamic k-TAA" by Man Ho Au, Willy Susilo, Yi Mu)
        this.E = Utils.randModOrder(rng);
        this.S = Utils.randModOrder(rng);

        this.B = new ECP();
        this.B.copy(Utils.genG1);
        this.B.add(m.getNym());
        this.B.add(key.Ipk.HRand.mul(S));

        for (int i = 0; i < attrs.length / 2; i++) {
            this.B.add(key.Ipk.HAttrs[2 * i].mul2(attrs[2 * i], key.Ipk.HAttrs[2 * i + 1], attrs[2 * i + 1]));
        }
        if (attrs.length % 2 != 0) {
            this.B.add(key.Ipk.HAttrs[attrs.length - 1].mul(attrs[attrs.length - 1]));
        }

        BIG exp = new BIG(key.Isk).plus(E);
        exp.mod(Utils.groupOrder);
        exp.invmodp(Utils.groupOrder);
        this.A = this.B.mul(exp);

        this.Attrs = new byte[attrs.length][Utils.fieldBytes];
        byte[] b = new byte[Utils.fieldBytes];
        for (int i = 0; i < attrs.length; i++) {
            attrs[i].toBytes(b);
            for (int j = 0; j < Utils.fieldBytes; j++) {
                this.Attrs[i][j] = b[j];
            }
        }
    }

    /**
     * Construct a CredRequest from a serialized credrequest
     */
    public Credential(Idemix.Credential proto) {
        this.A = Utils.transformFromProto(proto.getA());
        this.B = Utils.transformFromProto(proto.getB());
        this.E = BIG.fromBytes(proto.getE().toByteArray());
        this.S = BIG.fromBytes(proto.getS().toByteArray());
        this.Attrs = new byte[proto.getAttrsCount()][];
        for (int i = 0; i < proto.getAttrsCount(); i++) {
            this.Attrs[i] = proto.getAttrs(i).toByteArray();
        }
    }

    /**
     * complete the credential with the randomness used when constructing the CredRequest.
     */
    public void complete(BIG credS1) {
        this.S.add(credS1);
        this.S.mod(Utils.groupOrder);
    }

    public ECP getA() {
        return A;
    }

    public ECP getB() {
        return B;
    }

    public BIG getE() {
        return E;
    }

    public BIG getS() {
        return S;
    }

    public byte[][] getAttrs() {
        return Attrs;
    }

    /**
     * ver cryptographically verifies the credential
     *
     * @param sk  the secret key of the user
     * @param ipk the public key of the issuer
     * @return true iff valid
     */
    public boolean ver(BIG sk, IssuerPublicKey ipk) {
        for (int i = 0; i < this.Attrs.length; i++) {
            if (this.Attrs[i] == null) {
                return false;
            }
        }

        ECP bPrime = new ECP();
        bPrime.copy(Utils.genG1);
        bPrime.add(ipk.Hsk.mul2(sk, ipk.HRand, this.S));
        for (int i = 0; i < this.Attrs.length / 2; i++) {
            bPrime.add(ipk.HAttrs[2 * i].mul2(BIG.fromBytes(this.Attrs[2 * i]), ipk.HAttrs[2 * i + 1], BIG.fromBytes(this.Attrs[2 * i + 1])));
        }
        if (this.Attrs.length % 2 != 0) {
            bPrime.add(ipk.HAttrs[this.Attrs.length - 1].mul(BIG.fromBytes(this.Attrs[this.Attrs.length - 1])));
        }
        if (!B.equals(bPrime)) {
            return false;
        }

        ECP2 a = Utils.genG2.mul(this.E);
        a.add(ipk.W);
        a.affine();
        if (!PAIR.fexp(PAIR.ate(a, A)).equals(PAIR.fexp(PAIR.ate(Utils.genG2, B)))) {
            return false;
        }
        return true;
    }

    /**
     * Convert this credential to a proto
     */
    public Idemix.Credential toProto() {
        Idemix.Credential.Builder builder = Idemix.Credential.newBuilder()
                .setA(Utils.transformToProto(this.A))
                .setB(Utils.transformToProto(this.B))
                .setE(ByteString.copyFrom(Utils.bigToBytes(this.E)))
                .setS(ByteString.copyFrom(Utils.bigToBytes(this.S)));

        for (byte[] attr : this.Attrs) {
            builder.addAttrs(ByteString.copyFrom(attr));
        }

        return builder.build();
    }
}
