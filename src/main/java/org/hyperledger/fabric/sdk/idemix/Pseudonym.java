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

import com.ibm.zurich.amcl.BN254.BIG;
import com.ibm.zurich.amcl.BN254.ECP;
import com.ibm.zurich.amcl.RAND;

/**
 * The class represents a pseudonym of a user,
 * unlinkable to other pseudonyms of the user.
 */
public class Pseudonym {

    private ECP Nym;
    private BIG RandNym;

    /**
     * Constructor
     *
     * @param sk  the secret key of the user
     * @param ipk the public key of the issuer
     * @param rng random number generator
     */
    public Pseudonym(BIG sk, IssuerPublicKey ipk, RAND rng) {
        this.RandNym = Utils.RandModOrder(rng);
        this.Nym = ipk.Hsk.mul2(sk, ipk.HRand, RandNym);
    }

    /**
     * @return the value of the pseudonym as an ECP
     */
    public ECP getNym() {
        return this.Nym;
    }

    /**
     * @return the secret randomness used to construct this pseudonym
     */
    public BIG getRandNym() {
        return this.RandNym;
    }
}
