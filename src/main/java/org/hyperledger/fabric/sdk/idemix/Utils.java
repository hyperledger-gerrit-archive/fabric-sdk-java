/*
 *
 *  Copyright IBM Corp. All Rights Reserved.
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

import java.security.SecureRandom;

import com.google.protobuf.ByteString;
import org.apache.milagro.amcl.FP256BN.BIG;
import org.apache.milagro.amcl.FP256BN.ECP;
import org.apache.milagro.amcl.FP256BN.ECP2;
import org.apache.milagro.amcl.FP256BN.FP2;
import org.apache.milagro.amcl.FP256BN.ROM;
import org.apache.milagro.amcl.HASH256;
import org.apache.milagro.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

/**
 * The class Utils consists of all needed utility functions for Idemix.
 * The class uses the apache milagro crypto library.
 */
public final class Utils {
    public static BIG gx = new BIG(ROM.CURVE_Gx);
    public static BIG gy = new BIG(ROM.CURVE_Gy);
    public static ECP genG1 = new ECP(gx, gy);
    public static BIG pxa = new BIG(ROM.CURVE_Pxa);
    public static BIG pxb = new BIG(ROM.CURVE_Pxb);
    public static FP2 px = new FP2(pxa, pxb);
    public static BIG pya = new BIG(ROM.CURVE_Pya);
    public static BIG pyb = new BIG(ROM.CURVE_Pyb);
    public static FP2 py = new FP2(pya, pyb);
    public static ECP2 genG2 = new ECP2(px, py);
    public static BIG groupOrder = new BIG(ROM.CURVE_Order);
    public static int fieldBytes = BIG.MODBYTES;


    private Utils() {
        // private constructor as there shouldn't be instances of this utility class
    }

    /**
     * Returns a random number generator, amcl.RAND,
     * initialized with a fresh seed.
     *
     * @return a random number generator
     */
    public static RAND getRand() {
        // construct a secure seed
        int seedLength = Utils.fieldBytes;
        SecureRandom random = new SecureRandom();
        byte[] seed = random.generateSeed(seedLength);

        // create a new amcl.RAND and initialize it with the generated seed
        RAND rng = new RAND();
        rng.clean();
        rng.seed(seedLength, seed);

        return rng;
    }

    /**
     * @return a random BIG in 0, ..., groupOrder-1
     */
    public static BIG randModOrder(RAND rng) {
        BIG q = new BIG(ROM.CURVE_Order);

        // Takes random element in this Zq.
        return BIG.randomnum(q, rng);
    }

    /**
     * hashModOrder hashes bytes to an amcl.BIG
     * in 0, ..., groupOrder
     *
     * @return a BIG in 0, ..., groupOrder-1
     */
    public static BIG hashModOrder(byte[] data) {
        HASH256 hash = new HASH256();
        for (int i = 0; i < data.length; i++) {
            hash.process(data[i]);
        }

        byte[] hasheddata = hash.hash();

        BIG ret = BIG.fromBytes(hasheddata);
        ret.mod(Utils.groupOrder);

        return ret;
    }

    /**
     * bigToBytes turns a BIG into a byte array
     *
     * @return a byte array representation of the BIG
     */
    public static byte[] bigToBytes(BIG big) {
        byte[] ret = new byte[Utils.fieldBytes];
        big.toBytes(ret);
        return ret;
    }

    /**
     * ecpToBytes turns an ECP into a byte array
     *
     * @return a byte array representation of the ECP
     */
    public static byte[] ecpToBytes(ECP e) {
        byte[] ret = new byte[2 * fieldBytes + 1];
        e.toBytes(ret);
        return ret;
    }

    /**
     * ecpToBytes turns an ECP2 into a byte array
     *
     * @return a byte array representation of the ECP2
     */
    public static byte[] ecpToBytes(ECP2 e) {
        byte[] ret = new byte[4 * fieldBytes];
        e.toBytes(ret);
        return ret;
    }

    /**
     * append appends a byte array to an existing byte array that is filled up to
     *
     * @param data     the data to which we want to append
     * @param index    the index up to which the data has been filled, i.e., the index at which we can start writing
     * @param toAppend the data to be appended
     * @return the new index up to which the data has been filled
     */
    public static int append(byte[] data, int index, byte[] toAppend) {
        System.arraycopy(toAppend, 0, data, index, toAppend.length);
        return index + toAppend.length;
    }

    /**
     * Returns an amcl.BN256.ECP on input of an ECP protobuf object.
     */
    public static ECP transformFromProto(Idemix.ECP w) {
        byte[] valuex = w.getX().toByteArray();
        byte[] valuey = w.getY().toByteArray();
        ECP ecp = new ECP(BIG.fromBytes(valuex), BIG.fromBytes(valuey));
        return ecp;
    }

    /**
     * Returns an amcl.BN256.ECP2 on input of an ECP2 protobuf object.
     */
    public static ECP2 transformFromProto(Idemix.ECP2 w) {
        byte[] valuexa = w.getXA().toByteArray();
        byte[] valuexb = w.getXB().toByteArray();
        byte[] valueya = w.getYA().toByteArray();
        byte[] valueyb = w.getYB().toByteArray();
        FP2 valuex = new FP2(BIG.fromBytes(valuexa), BIG.fromBytes(valuexb));
        FP2 valuey = new FP2(BIG.fromBytes(valueya), BIG.fromBytes(valueyb));
        ECP2 ecp2 = new ECP2(valuex, valuey);
        return ecp2;
    }

    /**
     * Converts an amcl.BN256.ECP2 into an ECP2 protobuf object.
     */
    public static Idemix.ECP2 transformToProto(ECP2 w) {

        byte[] valueXA = new byte[Utils.fieldBytes];
        byte[] valueXB = new byte[Utils.fieldBytes];
        byte[] valueYA = new byte[Utils.fieldBytes];
        byte[] valueYB = new byte[Utils.fieldBytes];

        w.getX().getA().toBytes(valueXA);
        w.getX().getB().toBytes(valueXB);
        w.getY().getA().toBytes(valueYA);
        w.getY().getB().toBytes(valueYB);

        Idemix.ECP2 proto = Idemix.ECP2.newBuilder()
                .setXA(ByteString.copyFrom(valueXA))
                .setXB(ByteString.copyFrom(valueXB))
                .setYA(ByteString.copyFrom(valueYA))
                .setYB(ByteString.copyFrom(valueYB))
                .build();

        return proto;
    }

    /**
     * Converts an amcl.BN256.ECP into an ECP protobuf object.
     */
    public static Idemix.ECP transformToProto(ECP w) {
        byte[] valueX = new byte[Utils.fieldBytes];
        byte[] valueY = new byte[Utils.fieldBytes];

        w.getX().toBytes(valueX);
        w.getY().toBytes(valueY);

        Idemix.ECP proto = Idemix.ECP.newBuilder().setX(ByteString.copyFrom(valueX)).setY(ByteString.copyFrom(valueY))
                .build();

        return proto;
    }
}
