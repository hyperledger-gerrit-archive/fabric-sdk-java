/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;


import java.io.File;
import java.security.PrivateKey;

import org.hyperledger.fabric.sdk.sample_store.SampleStore;
import org.hyperledger.fabric.sdk.sample_store.SampleUser;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.FCAClient;

public class TestHFClient {

    final File tempFile;
    final HFClient hfClient;

    public TestHFClient(File tempFile, HFClient hfClient) {
        this.tempFile = tempFile;
        this.hfClient = hfClient;
    }

    public static HFClient newInstance() throws Exception {


        File tempFile = File.createTempFile("teststore", "properties");
        tempFile.deleteOnExit();

        File sampleStoreFile = new File(System.getProperty("user.home") + "/test.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        final SampleStore sampleStore = new SampleStore(sampleStoreFile);

        SampleUser someTestUSER = sampleStore.getMember("someTestUSER");



        HFClient hfclient = HFClient.createNewInstance();

        someTestUSER.setEnrollment(new Enrollment() {
            @Override
            public PrivateKey getKey() {
                return null;
            }

            @Override
            public String getCert() {
                return null;
            }
        });
        hfclient.setUserContext(someTestUSER);
        tempFile = File.createTempFile("teststore", "properties");
        hfclient.setKeyValStore(new FileKeyValStore(tempFile));
        hfclient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        hfclient.setMemberServices(new FCAClient("http://Nowhere.com", null));

        new TestHFClient(tempFile, hfclient);

        return hfclient;

    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (tempFile != null) {
            try {
                tempFile.delete();
            } catch (Exception e) {
               // // now harm done.
            }
        }
    }
}
