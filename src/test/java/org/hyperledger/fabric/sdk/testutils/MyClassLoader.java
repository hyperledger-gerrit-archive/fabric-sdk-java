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

package org.hyperledger.fabric.sdk.testutils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class MyClassLoader {

    private URLClassLoader urlClassLoader;

    public MyClassLoader() throws MalformedURLException {

        URL nurl = new File("target/classes/").toURI().toURL();
        URL[] urlArray = {nurl};
        //   urlClassLoader = new URLClassLoader(urlArray, ClassLoader.getSystemClassLoader());
        urlClassLoader = new URLClassLoader(urlArray, null);

    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {

        return (Class<?>) urlClassLoader.loadClass(name);

    }

    @Override
    protected void finalize() throws Throwable {
        System.out.println("MyClassLoader - End.");
    }

}
