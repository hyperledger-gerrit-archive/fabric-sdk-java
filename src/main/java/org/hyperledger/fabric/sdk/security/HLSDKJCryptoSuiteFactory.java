/*
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
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

package org.hyperledger.fabric.sdk.security;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;

/**
 * SDK's Default implementation of CryptoSuiteFactory.
 */
class HLSDKJCryptoSuiteFactory implements CryptoSuiteFactory {
    static final Config config = Config.getConfig();

    private HLSDKJCryptoSuiteFactory() {

    }

    private static final Map<Properties, CryptoSuite> cache = new ConcurrentHashMap<>();

    @Override
    public CryptoSuite getCryptoSuite(Properties properties) throws CryptoException, InvalidArgumentException {

        CryptoSuite ret = cache.get(properties);
        if (ret == null) {
            ret = new CryptoPrimitives();
            ret.setProperties(properties);
            ret.init();
            cache.put(properties, ret);

        }

        return ret;

    }

    @Override
    public CryptoSuite getCryptoSuite() throws CryptoException, InvalidArgumentException {

        Properties properties = new Properties();
        properties.put(Config.SECURITY_LEVEL, config.getSecurityLevel());
        properties.put(Config.HASH_ALGORITHM, config.getHashAlgorithm());

        return getCryptoSuite(properties);
    }

    private static final HLSDKJCryptoSuiteFactory INSTANCE = new HLSDKJCryptoSuiteFactory();

    static final synchronized HLSDKJCryptoSuiteFactory instance() {

        return INSTANCE;
    }

    private static CryptoSuiteFactory theFACTORY = null; // one and only factory.

    static final synchronized CryptoSuiteFactory getDefault() throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        if (null == theFACTORY) {

            String cf = config.getDefaultCryptoSuiteFactory();
            if (null == cf || cf.isEmpty() || cf.equals(HLSDKJCryptoSuiteFactory.class.getName())) { // Use this class.

                theFACTORY = HLSDKJCryptoSuiteFactory.instance();

            } else {

                Class<?> aClass = Class.forName(cf);

                Method method = aClass.getMethod("instance");
                theFACTORY = (CryptoSuiteFactory) method.invoke(null, new Object[] {});

            }
        }

        return theFACTORY;

    }

}