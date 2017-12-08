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

import javax.json.JsonObject;

import org.junit.Assert;
import org.junit.Test;

public class IdentityRequestTest {
    private static final String attrName = "some name";
    private static final String attrValue = "some value";
    private static final String affiliation = "corporation";
    private static final String caname = "CA";
    private static final String id = "userid";
    private static final String secret = "secret";
    private static final String type = "user";

    private static final int maxEnrollments = 5;

    @Test
    public void testNewInstance() {

        try {
            IdentityRequest testIdentityReq = new IdentityRequest(id, affiliation);
            Assert.assertEquals(testIdentityReq.getEnrollmentID(), id);
            Assert.assertEquals(testIdentityReq.getType(), type);
            Assert.assertEquals(testIdentityReq.getMaxEnrollments(), null);
            Assert.assertEquals(testIdentityReq.getAffiliation(), affiliation);
            Assert.assertTrue(testIdentityReq.getAttributes().isEmpty());

            JsonObject jo = testIdentityReq.toJsonObject();
            JsonObject info = jo.getJsonObject("info");
            Assert.assertEquals(affiliation, info.getString("affiliation"));
            Assert.assertFalse(info.containsKey("max_enrollments"));
            Assert.assertEquals(id, info.getString("id"));

        } catch (Exception e) {
            Assert.fail("Unexpected Exception " + e.getMessage());
        }
    }

    @Test
    public void testNewInstanceNoAffiliation() {

        try {
            IdentityRequest testIdentityReq = new IdentityRequest(id);
            testIdentityReq.setMaxEnrollments(3);

            Assert.assertEquals(id, testIdentityReq.getEnrollmentID());
            Assert.assertEquals(type, testIdentityReq.getType());
            Assert.assertEquals(new Integer(3), testIdentityReq.getMaxEnrollments());
            Assert.assertEquals(null, testIdentityReq.getAffiliation());
            Assert.assertTrue(testIdentityReq.getAttributes().isEmpty());

            JsonObject jo = testIdentityReq.toJsonObject();
            JsonObject info = jo.getJsonObject("info");
            Assert.assertFalse(info.containsKey("affiliation"));
            Assert.assertEquals(3, info.getInt("max_enrollments"));
            Assert.assertEquals(id, info.getString("id"));

        } catch (Exception e) {
            Assert.fail("Unexpected Exception " + e.getMessage());
        }
    }

    @Test
    public void testNewInstanceSetNullID() {

        try {
            new IdentityRequest(null, affiliation);
            Assert.fail("Expected exception when null is specified for id");

        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), "id may not be null");
        }
    }

    @Test
    public void testNewInstanceSetNullAffiliation() {

        try {
            new IdentityRequest(id, null);
            Assert.fail("Expected exception when null is specified for affiliation");

        } catch (Exception e) {
            Assert.assertEquals(e.getMessage(), "affiliation may not be null");
        }
    }

    @Test
    public void testIdentityReqSetGet() {

        try {
            IdentityRequest testIdentityReq = new IdentityRequest(id, affiliation);
            testIdentityReq.setEnrollmentID(id + "update");
            testIdentityReq.setSecret(secret);
            testIdentityReq.setMaxEnrollments(maxEnrollments);
            testIdentityReq.setType(type);
            testIdentityReq.setAffiliation(affiliation + "update");
            testIdentityReq.setCAName(caname);
            testIdentityReq.addAttribute(new Attribute(attrName, attrValue));
            Assert.assertEquals(testIdentityReq.getEnrollmentID(), id + "update");
            Assert.assertEquals(testIdentityReq.getSecret(), secret);
            Assert.assertEquals(testIdentityReq.getType(), type);
            Assert.assertEquals(testIdentityReq.getAffiliation(), affiliation + "update");
            Assert.assertTrue(!testIdentityReq.getAttributes().isEmpty());

        } catch (Exception e) {
            Assert.fail("Unexpected Exception " + e.getMessage());
        }
    }

    @Test
    public void testIdentityReqToJson() {

        try {
            IdentityRequest testIdentityReq = new IdentityRequest(id, affiliation);
            testIdentityReq.setEnrollmentID(id + "update");
            testIdentityReq.setSecret(secret);
            testIdentityReq.setMaxEnrollments(maxEnrollments);
            testIdentityReq.setType(type);
            testIdentityReq.setAffiliation(affiliation + "update");
            testIdentityReq.setCAName(caname);
            testIdentityReq.addAttribute(new Attribute(attrName, attrValue));
            Assert.assertTrue(testIdentityReq.toJson().contains(affiliation + "update"));

        } catch (Exception e) {
            Assert.fail("Unexpected Exception " + e.getMessage());
        }
    }
}