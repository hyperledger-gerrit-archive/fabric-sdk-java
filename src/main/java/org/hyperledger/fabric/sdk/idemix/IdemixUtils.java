/*
 *
 *  Copyright 2017, 2018 IBM Corp. All Rights Reserved.
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
 * The class IdemixUtils consists of all needed utility functions for Idemix.
 * The class uses the apache milagro crypto library.
 */
public final class IdemixUtils {
    private static final BIG gx = new BIG(ROM.CURVE_Gx);
    private static final BIG gy = new BIG(ROM.CURVE_Gy);
    protected static final ECP genG1 = new ECP(gx, gy);
    private static final BIG pxa = new BIG(ROM.CURVE_Pxa);
    private static final BIG pxb = new BIG(ROM.CURVE_Pxb);
    private static final FP2 px = new FP2(pxa, pxb);
    private static final BIG pya = new BIG(ROM.CURVE_Pya);
    private static final BIG pyb = new BIG(ROM.CURVE_Pyb);
    private static final FP2 py = new FP2(pya, pyb);
    protected static final ECP2 genG2 = new ECP2(px, py);
    protected static final BIG GROUP_ORDER = new BIG(ROM.CURVE_Order);
    protected static final int FIELD_BYTES = BIG.MODBYTES;
    protected static RAND rng = getRand();

    private IdemixUtils() {
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
        int seedLength = IdemixUtils.FIELD_BYTES;
        SecureRandom random = new SecureRandom();
        byte[] seed = random.generateSeed(seedLength);

        // create a new amcl.RAND and initialize it with the generated seed
        RAND rng = new RAND();
        rng.clean();
        rng.seed(seedLength, seed);

        return rng;
    }

    /**
     * @return a random BIG in 0, ..., GROUP_ORDER-1
     */
    public static BIG randModOrder() {
        BIG q = new BIG(ROM.CURVE_Order);

        // Takes random element in this Zq.
        return BIG.randomnum(q, rng);
    }

    /**
     * hashModOrder hashes bytes to an amcl.BIG
     * in 0, ..., GROUP_ORDER
     *
     * @return a BIG in 0, ..., GROUP_ORDER-1
     */
    public static BIG hashModOrder(byte[] data) {
        HASH256 hash = new HASH256();
        for (byte b : data) {
            hash.process(b);
        }

        byte[] hasheddata = hash.hash();

        BIG ret = BIG.fromBytes(hasheddata);
        ret.mod(IdemixUtils.GROUP_ORDER);

        return ret;
    }

    /**
     * bigToBytes turns a BIG into a byte array
     *
     * @return a byte array representation of the BIG
     */
    public static byte[] bigToBytes(BIG big) {
        byte[] ret = new byte[IdemixUtils.FIELD_BYTES];
        big.toBytes(ret);
        return ret;
    }

    /**
     * ecpToBytes turns an ECP into a byte array
     *
     * @return a byte array representation of the ECP
     */
    public static byte[] ecpToBytes(ECP e) {
        byte[] ret = new byte[2 * FIELD_BYTES + 1];
        e.toBytes(ret);
        return ret;
    }

    /**
     * ecpToBytes turns an ECP2 into a byte array
     *
     * @return a byte array representation of the ECP2
     */
    public static byte[] ecpToBytes(ECP2 e) {
        byte[] ret = new byte[4 * FIELD_BYTES];
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
        return new ECP(BIG.fromBytes(valuex), BIG.fromBytes(valuey));
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
        return new ECP2(valuex, valuey);
    }

    /**
     * Converts an amcl.BN256.ECP2 into an ECP2 protobuf object.
     */
    public static Idemix.ECP2 transformToProto(ECP2 w) {

        byte[] valueXA = new byte[IdemixUtils.FIELD_BYTES];
        byte[] valueXB = new byte[IdemixUtils.FIELD_BYTES];
        byte[] valueYA = new byte[IdemixUtils.FIELD_BYTES];
        byte[] valueYB = new byte[IdemixUtils.FIELD_BYTES];

        w.getX().getA().toBytes(valueXA);
        w.getX().getB().toBytes(valueXB);
        w.getY().getA().toBytes(valueYA);
        w.getY().getB().toBytes(valueYB);

        return Idemix.ECP2.newBuilder()
                .setXA(ByteString.copyFrom(valueXA))
                .setXB(ByteString.copyFrom(valueXB))
                .setYA(ByteString.copyFrom(valueYA))
                .setYB(ByteString.copyFrom(valueYB))
                .build();
    }

    /**
     * Converts an amcl.BN256.ECP into an ECP protobuf object.
     */
    public static Idemix.ECP transformToProto(ECP w) {
        byte[] valueX = new byte[IdemixUtils.FIELD_BYTES];
        byte[] valueY = new byte[IdemixUtils.FIELD_BYTES];

        w.getX().toBytes(valueX);
        w.getY().toBytes(valueY);

        return Idemix.ECP.newBuilder().setX(ByteString.copyFrom(valueX)).setY(ByteString.copyFrom(valueY)).build();
    }
}
