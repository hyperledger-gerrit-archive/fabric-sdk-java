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

import org.hyperledger.fabric.sdk.helper.Config;

/**
 * Container for methods to set SDK environment before running unit+integration tests
 * @author ttd
 *
 */
public class TestConfigHelper {

    public static final String CONFIG_FILE_LOCATION = "org.hyperledger.fabric.sdk.config.location";

    /**
     * clearConfig "resets" Config so that the Config testcases can run without interference from other test suites.
     * Depending on what order JUnit decides to run the tests, Config could have been instantiated earlier and could
     * contain values that make the tests here fail.
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     *
     */
    public void clearConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Config config = Config.getConfig();
        java.lang.reflect.Field configInstance = config.getClass().getDeclaredField("config");
        configInstance.setAccessible(true);
        configInstance.set(null, null);
    }

    /**
     * customizeConfig clears the current SDK configuration then reloads the configuration from the file pointed at by org.hyperledger.fabric.sdk.config.location
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public void customizeConfig() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        String configLocation = System.getProperty(CONFIG_FILE_LOCATION);
        this.customizeConfig(configLocation);
        }

    /**
     * customizeConfig clears the current SDK configuration then reloads the configuration from the given file
     *
     * @param configLocation
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public void customizeConfig(String configLocation) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        // first, get rid of old configuration
        this.clearConfig();
        // then point to new config file
        if (configLocation != null && !configLocation.isEmpty()) {
            System.setProperty("org.hyperledger.fabric.sdk.configuration", configLocation);
        }
    }
}
