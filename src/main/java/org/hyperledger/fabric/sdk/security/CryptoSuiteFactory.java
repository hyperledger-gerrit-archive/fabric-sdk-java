/*
 *
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

package org.hyperledger.fabric.sdk.security;

import java.util.Properties;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;

public interface CryptoSuiteFactory {
    Config config = Config.getConfig();

    CryptoSuite getCryptoSuite(Properties properties) throws CryptoException, InvalidArgumentException;

    CryptoSuite getCryptoSuite() throws CryptoException, InvalidArgumentException;

    static CryptoSuiteFactory getDefault() throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        CryptoSuiteFactory theFactory;

        String cf = config.getDefaultCryptoSuiteFactory();
        if (null == cf) {

            theFactory = HLSDKJCryptoSuiteFactory.instance();

        } else {

            Class<?> aClass = Class.forName(cf);
            theFactory = (CryptoSuiteFactory) aClass.newInstance();

        }

        return theFactory;

    }
}