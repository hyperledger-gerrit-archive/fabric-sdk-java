/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric_ca.sdk;

import java.util.Collection;

import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class CSRInfoTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String cn = "testCN";

    @Test
    public void testCSREmpytCN() throws Exception {
        thrown.expect(InvalidArgumentException.class);
        thrown.expectMessage("Common Name (CN) is required for CSR");

        CSRInfo testCSRInfo = new CSRInfo("");
    }

    @Test
    public void testCSRNullCN() throws Exception {
        thrown.expect(InvalidArgumentException.class);
        thrown.expectMessage("Common Name (CN) is required for CSR");

        CSRInfo testCSRInfo = new CSRInfo(null);
    }

    @Test
    public void testCSRInfo() throws Exception {
        CSRInfo testCSRInfo = new CSRInfo(cn);
        testCSRInfo.addName("US", "IBM Dr", "RTP", "IBM");
        testCSRInfo.addName("CA", "IBM Campus", "Durham", "ibm");

        assertEquals(testCSRInfo.getCn(), cn);

        Collection<CSRInfo.Name> names = testCSRInfo.getNames();

        CSRInfo.Name[] namesArray = names.toArray(new CSRInfo.Name[names.size()]);

        assertEquals(namesArray[0].getC(), "US");
        assertEquals(namesArray[0].getSt(), "IBM Dr");
        assertEquals(namesArray[0].getL(), "RTP");
        assertEquals(namesArray[0].getO(), "IBM");

        assertEquals(namesArray[1].getC(), "CA");
        assertEquals(namesArray[1].getSt(), "IBM Campus");
        assertEquals(namesArray[1].getL(), "Durham");
        assertEquals(namesArray[1].getO(), "ibm");
    }
}