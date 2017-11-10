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
import com.ibm.zurich.amcl.BN254.*;
import com.ibm.zurich.amcl.HASH256;
import com.ibm.zurich.amcl.RAND;
import org.hyperledger.fabric.protos.idemix.Idemix;

import java.security.SecureRandom;

/**
 * The class Utils consists of all needed utility functions for Idemix.
 * The class uses the apache milagro crypto library.
 */
public class Utils {
    public static BIG Gx = new BIG(ROM.CURVE_Gx);
    public static BIG Gy = new BIG(ROM.CURVE_Gy);
    public static ECP GenG1 = new ECP(Gx, Gy);
    public static BIG Pxa = new BIG(ROM.CURVE_Pxa);
    public static BIG Pxb = new BIG(ROM.CURVE_Pxb);
    public static FP2 Px = new FP2(Pxa, Pxb);
    public static BIG Pya = new BIG(ROM.CURVE_Pya);
    public static BIG Pyb = new BIG(ROM.CURVE_Pyb);
    public static FP2 Py = new FP2(Pya, Pyb);
    public static ECP2 GenG2 = new ECP2(Px, Py);
    public static BIG GroupOrder = new BIG(ROM.CURVE_Order);
    public static int FieldBytes = BIG.MODBYTES;

    /**
     * Returns a random number generator, amcl.RAND,
     * initialized with a fresh seed.
     *
     * @return a random number generator
     */
    public static RAND getRand() {
        // construct a secure seed
        int seedLength = Utils.FieldBytes;
        SecureRandom random = new SecureRandom();
        byte[] seed = random.generateSeed(seedLength);

        // create a new amcl.RAND and initialize it with the generated seed
        RAND rng = new RAND();
        rng.clean();
        rng.seed(seedLength, seed);

        return rng;
    }

    /**
     * @return a random BIG in 0, ..., GroupOrder-1
     */
    public static BIG RandModOrder(RAND rng) {
        BIG q = new BIG(ROM.CURVE_Order);

        // Takes random element in this Zq.
        return BIG.randomnum(q, rng);
    }

    /**
     * HashModOrder hashes bytes to an amcl.BIG
     * in 0, ..., GroupOrder
     *
     * @return a BIG in 0, ..., GroupOrder-1
     */
    public static BIG HashModOrder(byte[] data) {
        HASH256 hash = new HASH256();
        for (int i = 0; i < data.length; i++) {
            hash.process(data[i]);
        }

        byte[] hasheddata = hash.hash();

        BIG ret = BIG.fromBytes(hasheddata);
        ret.mod(Utils.GroupOrder);

        return ret;
    }

    /**
     * BigToBytes turns a BIG into a byte array
     *
     * @return a byte array representation of the BIG
     */
    public static byte[] BigToBytes(BIG big) {
        byte[] ret = new byte[Utils.FieldBytes];
        big.toBytes(ret);
        return ret;
    }

    /**
     * EcpToBytes turns an ECP into a byte array
     *
     * @return a byte array representation of the ECP
     */
    public static byte[] EcpToBytes(ECP e) {
        byte[] ret = new byte[2 * FieldBytes + 1];
        e.toBytes(ret);
        return ret;
    }

    /**
     * EcpToBytes turns an ECP2 into a byte array
     *
     * @return a byte array representation of the ECP2
     */
    public static byte[] EcpToBytes(ECP2 e) {
        byte[] ret = new byte[4 * FieldBytes];
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

        byte[] valueXA = new byte[Utils.FieldBytes];
        byte[] valueXB = new byte[Utils.FieldBytes];
        byte[] valueYA = new byte[Utils.FieldBytes];
        byte[] valueYB = new byte[Utils.FieldBytes];

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
        byte[] valueX = new byte[Utils.FieldBytes];
        byte[] valueY = new byte[Utils.FieldBytes];

        w.getX().toBytes(valueX);
        w.getY().toBytes(valueY);

        Idemix.ECP proto = Idemix.ECP.newBuilder().setX(ByteString.copyFrom(valueX)).setY(ByteString.copyFrom(valueY))
                .build();

        return proto;
    }
}
