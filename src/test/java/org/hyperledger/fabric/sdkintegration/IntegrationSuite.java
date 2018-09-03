/*
 Copyright IBM Corp. All Rights Reserved.

 SPDX-License-Identifier: Apache-2.0
*/
package org.hyperledger.fabric.sdkintegration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith (Suite.class)

@Suite.SuiteClasses (
        {
                End2endMTIT.class
        })
public class IntegrationSuite {

}
