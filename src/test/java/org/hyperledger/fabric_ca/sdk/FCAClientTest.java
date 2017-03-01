package org.hyperledger.fabric_ca.sdk;

import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rineholt on 3/10/17.
 */
public class FCAClientTest {
    public static class MemberServicesFabricCAImplTest {


        @Test
        public void testCOPCreation() {

            try {
                FCAClient memberServices = new FCAClient("http://localhost:99", null);
                Assert.assertNotNull(memberServices);
                Assert.assertSame(FCAClient.class, memberServices.getClass());


            } catch (Exception e) {
                Assert.fail("Unexpected Exception " + e.getMessage());
            }
        }
        @Test
        public void testNullURL() {

            try {
                 new FCAClient(null, null);
                Assert.fail("Expected exception");

            } catch (Exception e) {
                Assert.assertSame(e.getClass(), MalformedURLException.class);

            }
        }
        @Test
        public void emptyURL() {

            try {
                new FCAClient("", null);
                Assert.fail("Expected exception");

            } catch (Exception e) {
                Assert.assertSame(e.getClass(), MalformedURLException.class);

            }
        }

        @Test
        public void testBadProto() {

            try {
                new FCAClient("file://localhost", null);
                Assert.fail("Expected exception");

            } catch (Exception e) {
                Assert.assertSame(e.getClass(), IllegalArgumentException.class);

            }
        }

        @Test
        public void testBadURLPath() {

            try {
                new FCAClient("http://localhost/bad", null);
                Assert.fail("Expected exception");

            } catch (Exception e) {
                Assert.assertSame(e.getClass(), IllegalArgumentException.class);

            }
        }

        @Test
        public void testBadURLQuery() {

            try {
                new FCAClient("http://localhost?bad", null);
                Assert.fail("Expected exception");

            } catch (Exception e) {
                Assert.assertSame(e.getClass(), IllegalArgumentException.class);

            }
        }


    //    @Test
    //    public void testBadEnrollUser() {
    //
    //        try {
    //            FCAClient memberServices = new FCAClient("http://localhost:99", null);
    //            memberServices.enroll(null);
    //            Assert.fail("Expected exception");
    //
    //        } catch (Exception e) {
    //            Assert.assertSame(e.getClass(), RuntimeException.class);
    //
    //        }
    //    }
    //
    //    @Test
    //    public void testBadEnrollBadUser() {
    //
    //        try {
    //            FCAClient memberServices = new FCAClient("http://localhost:99", null);
    //            EnrollmentRequest req = new EnrollmentRequest();
    //            req.setEnrollmentID("");
    //            req.setEnrollmentSecret("adminpw");
    //            memberServices.enroll(null);
    //            Assert.fail("Expected exception");
    //
    //        } catch (Exception e) {
    //            Assert.assertSame(e.getClass(), RuntimeException.class);
    //
    //        }
    //    }
    //
    //    @Test
    //    public void testBadEnrollBadSecret() {
    //
    //        try {
    //            FCAClient memberServices = new FCAClient("http://localhost:99", null);
    //            EnrollmentRequest req = new EnrollmentRequest();
    //            req.setEnrollmentID("user");
    //            req.setEnrollmentSecret("");
    //            memberServices.enroll(null);
    //            Assert.fail("Expected exception");
    //
    //        } catch (Exception e) {
    //            Assert.assertSame(e.getClass(), RuntimeException.class);
    //
    //        }
    //    }
    }
}
