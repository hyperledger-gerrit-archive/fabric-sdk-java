/*
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

package org.hyperledger.fabric_ca.sdkintegration;

import java.io.File;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdkintegration.SampleStore;
import org.hyperledger.fabric.sdkintegration.SampleUser;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.junit.Test;

import static org.junit.Assert.fail;

public class HFCAClientEnrollIT {
    public static class MemberServicesFabricCAImplTest {
        private static final String TEST_ADMIN_NAME = "admin";
        private static final String TEST_ADMIN_PW = "adminpw";
        private static final String TEST_ADMIN_ORG = "org0";
        private static final String TEST_USER1_NAME = "user1";
        private static final String TEST_USER1_ORG = "Org1";
        private static final String TEST_USER1_AFFILIATION = "org1.department1";
        private static final String CA_LOCATION = "http://localhost:7054";

        @Test
        public void testReenrollAndRevoke() {
            try {
                CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
                cryptoSuite.init();
                HFCAClient client = new HFCAClient(CA_LOCATION, null);
                client.setCryptoSuite(cryptoSuite);

                File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
                if (sampleStoreFile.exists()) { //For testing start fresh
                    sampleStoreFile.delete();
                }

                final SampleStore sampleStore = new SampleStore(sampleStoreFile);
                sampleStoreFile.deleteOnExit();

                //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface
                SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, TEST_ADMIN_ORG);
                if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric CA.
                    admin.setEnrollment(client.enroll(admin.getName(), TEST_ADMIN_PW));
                }

                // get another enrollment
                Enrollment tmpEnroll = client.reenroll(admin);

                // revoke the tmp enrollment
                client.revoke(admin, tmpEnroll, 1);
            } catch (Exception e) {
                e.printStackTrace();
                fail("user reenroll/revoke test failed with error : " + e.getMessage());
            }
        }

        @Test
        public void testUserRevoke() {
            try {
                CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
                cryptoSuite.init();
                HFCAClient client = new HFCAClient(CA_LOCATION, null);
                client.setCryptoSuite(cryptoSuite);

                File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
                if (sampleStoreFile.exists()) { //For testing start fresh
                    sampleStoreFile.delete();
                }

                final SampleStore sampleStore = new SampleStore(sampleStoreFile);
                sampleStoreFile.deleteOnExit();

                //SampleUser can be any implementation that implements org.hyperledger.fabric.sdk.User Interface
                SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, TEST_ADMIN_ORG);
                if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric CA.
                    admin.setEnrollment(client.enroll(admin.getName(), TEST_ADMIN_PW));
                }

                SampleUser user1 = sampleStore.getMember(TEST_USER1_NAME, TEST_USER1_ORG);

                if (!user1.isRegistered()) {
                    RegistrationRequest rr = new RegistrationRequest(user1.getName(), TEST_USER1_AFFILIATION);
                    user1.setEnrollmentSecret(client.register(rr, admin)); //Admin can register other users.
                }

                if (!user1.isEnrolled()) {
                    user1.setEnrollment(client.enroll(user1.getName(), user1.getEnrollmentSecret()));
                }

                client.revoke(admin, user1.getName(), 1);
            } catch (Exception e) {
                e.printStackTrace();
                fail("user enroll/revoke-all test failed with error : " + e.getMessage());
            }
        }

    }
}
