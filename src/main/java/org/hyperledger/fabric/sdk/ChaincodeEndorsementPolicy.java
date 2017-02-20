/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * A wrapper for the Hyperledger Fabric Policy object
 *
 */
public class ChaincodeEndorsementPolicy {
    private byte[] policyBytes = null ;

    public ChaincodeEndorsementPolicy() {
    }

    public ChaincodeEndorsementPolicy(File policyFile) throws IOException {
        InputStream is = new FileInputStream(policyFile) ;
        this.policyBytes = IOUtils.toByteArray(is);
    }

    public ChaincodeEndorsementPolicy(byte[] policyAsBytes) {
        this.policyBytes = policyAsBytes;
    }

    public void setChaincodePolicy(byte[] policyAsBytes) {
        this.policyBytes = policyAsBytes;
    }

    public byte[] getChaincodePolicyAsBytes() {
        return this.policyBytes;
    }
}
