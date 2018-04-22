/*
 *  Copyright 2018 Mediaocean - All Rights Reserved.
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

package org.hyperledger.fabric.sdkintegration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.testutils.TestConfig;
import org.hyperledger.fabric.sdkintegration.helper.ChaincodeProcessor;
import org.hyperledger.fabric.sdkintegration.helper.FabricProcessor;
import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;

public class AddingAnOrgIT {
    private static final TestConfig testConfig = TestConfig.getConfig();
    private static final String TESTUSER_1_NAME = "user1";

    private SampleStore sampleStore;

    private Collection<SampleOrg> testSampleOrgs;
    private final Map<String, SampleOrg> orgs1to3 = new HashMap<>();
    private final Map<String, HFClient> clients1to3 = new HashMap<>();

    public static final String V1PATH = "src/test/fixture/sdkintegration/e2e-2Orgs/v1.1";
    public static final String CHANNEL = "foo";

    private static final String CHAIN_CODE_NAME = "tripart_example";
    private static final String CHAIN_CODE_PATH = "github.com/example_cc";
    private static final String CHAIN_CODE_VERSION = "1.0";

    SampleOrg ordererOrg;
    SampleOrg org3;
    SampleOrg org2;
    SampleOrg org1;

    int sleep = 3;

    @Before
    public void setUp() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();

        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        if (!sampleStoreFile.exists()) { // End2EndIT must run before this testing start fresh
            throw new RuntimeException("You must run End2EndIT first");
        }
        sampleStore = new SampleStore(sampleStoreFile);

        for (SampleOrg org : testSampleOrgs) {
            switch (org.getMSPID()) {
                case "Org1MSP":
                    org1 = org;
                    org1.addUser(sampleStore.getMember(TESTUSER_1_NAME, org.getName())); // Remember user belongs to this Org
                    setPeerAdmin(org1);
                    orgs1to3.put(org1.getMSPID(), org1);
                    break;

                case "Org2MSP":
                    org2 = org;
                    org2.addUser(sampleStore.getMember(TESTUSER_1_NAME, org.getName())); // Remember user belongs to this Org
                    setPeerAdmin(org2);
                    orgs1to3.put(org2.getMSPID(), org2);
                    break;

                case "Org3MSP":
                    org3 = org;
                    org3.addUser(sampleStore.getMember(TESTUSER_1_NAME, org.getName())); // Remember user belongs to this Org
                    setPeerAdmin(org3);
                    orgs1to3.put(org3.getMSPID(), org3);
                    break;

                case "OrdererMSP":
                    ordererOrg = org;
                    ordererOrg.setPeerAdmin(createOrdererPeerAdmin(ordererOrg));
                    break;

                default:
                    break;
            }
        }
        if (org1 == null || org2 == null || org3 == null || ordererOrg == null) {
            throw new RuntimeException("There is a problem with the SampleOrg org definitions");
        }

    }

    @Test
    public void testAddingMemberToConsortium() throws Exception {
        FabricProcessor fp = new FabricProcessor(this);
        ChaincodeProcessor cp = new ChaincodeProcessor(fp);

        // We will use this client throughout.
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(ordererOrg.getPeerAdmin());

        // Joining peer to channel is one time remember
        System.out.println("***Joining orderer peer to system channel");
        fp.joinOrdererChannel(client, ordererOrg);
        sleepAWhile();
        System.out.println("***Adding org3 to the consortium");
        fp.addMemberToConsortium(client, ordererOrg, org3);
        sleepAWhile();

        // Set up an HFClient pointing to the peerAdmin for each one of these
        for (Entry<String, SampleOrg> entry : orgs1to3.entrySet()) {
            HFClient hfc = HFClient.createNewInstance();
            hfc.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            hfc.setUserContext(entry.getValue().getPeerAdmin());
            clients1to3.put(entry.getKey(), hfc);
        }

        System.out.println("***Adding org3 to the foo channel");
        fp.addMemberToFooChannel(ordererOrg, org1, org3, org2);
        sleepAWhile();

        // End2EndIt doesn't add the peers for Org2 to the channel.
        // We also need to do that for Org3 since we just added it. Let's do that now so it all will work as it should.
        fp.constructFooChannel(org2);
        sleepAWhile();
        fp.constructFooChannel(org3);
        sleepAWhile();

        // So if you get to this point then you have org3 added to the foo channel and the Org3 peers have been joined to it too.

        // Now lets install and instantiate some chaincode on the peers for all 3 orgs and see if we can do something useful
        // We will install the same chaincode as used in End2EndIt but give it a different name and a different endorsement policy
        // A lot of the code is hardcoded in ChaincodeProcessor as this example was taken from something more generic.
        // It should be easy to extend back for a different application.

        ChaincodeID ccid = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME).setVersion(CHAIN_CODE_VERSION).setPath(CHAIN_CODE_PATH).build();
        System.out.println("***Installing new chaincode " + CHAIN_CODE_NAME + ":" + CHAIN_CODE_VERSION + " onto the peers on the foo channel for the three orgs");
        cp.installChaincode(ccid, org1, org2, org3);
        sleepAWhile();

        // So now there's chaincode on all the peers. Let's try a transaction.
        // We run a transaction with a regular user, not a peer admin
        HFClient o1c = clients1to3.get(org1.getMSPID());
        client.setUserContext(org1.getUser(TESTUSER_1_NAME));

        ArrayList<String> parameters = new ArrayList<>();
        parameters.add("a");
        parameters.add("b");
        parameters.add("100");

        System.out.println("***Trying a transaction on the channel using the new chaincode");
        Channel c = fp.reconstructFooChannel(org1, org2, org3);
        sleepAWhile();

        cp.invoke(o1c, c, ccid, "move", parameters);
        sleepAWhile();

        System.out.println("***Trying to read using Org2 on the channel using the new chaincode");
        HFClient o2c = clients1to3.get(org2.getMSPID());
        client.setUserContext(org2.getUser(TESTUSER_1_NAME));

        parameters = new ArrayList<>();
        parameters.add("b");

        String s = cp.query(o2c, fp.reconstructFooChannel(org2), ccid, "query", parameters);
        System.out.println(s);

        System.out.println("***Job done");
    }

    private SampleUser createOrdererPeerAdmin(SampleOrg org) throws Exception {
        SampleUser su = new SampleUser("OrdererPeerAdmin", org.getName(), sampleStore);
        su.setMspId(org.getMSPID());
        su.setEnrollment(buildEnrollment("example.com"));
        return su;
    }

    /**
     * Build something that matches the Enrollment for an admin user (a peer admin) from the orderer crypto
     *
     * @param domain
     * @return
     * @throws Exception
     */
    private Enrollment buildEnrollment(String domain) throws Exception {
        System.out.println("Building enrollment for " + domain);

        String admin = "Admin@example.com"; // Admin user in orderer users
        String pem = admin + "-cert.pem";

        Path keyPath = Paths.get(V1PATH, "crypto-config", "ordererOrganizations", domain, "users", admin, "msp", "keystore");
        PrivateKey privateKey = getPrivateKeyFromBytes(keyPath);

        Path certPath = Paths.get(V1PATH, "crypto-config", "ordererOrganizations", domain, "users", admin, "msp", "signcerts", pem);
        String certificate = getCertificate(certPath);
        return new PeerEnrollment(privateKey, certificate);
    }

    private PrivateKey getPrivateKeyFromBytes(Path keyPath) throws Exception {
        File skf = findFileSk(keyPath.toFile());
        System.out.println(format("Determined that the _sk file for %s is %s", keyPath.toString(), skf.getName()));

        final Reader pemReader = new StringReader(new String(IOUtils.toByteArray(new FileInputStream(skf))));

        final PrivateKeyInfo pemPair;
        try (PEMParser pemParser = new PEMParser(pemReader)) {
            pemPair = (PrivateKeyInfo) pemParser.readObject();
        }

        return new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);
    }

    private String getCertificate(Path certPath) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        File cf = certPath.toFile();
        String certificate = new String(IOUtils.toByteArray(new FileInputStream(cf)), "UTF-8");
        return certificate;
    }

    /**
     * Find the _sk file in a directory
     *
     * @param directory
     * @return
     * @throws FabricException
     */
    private File findFileSk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(format("Matches returned null. Does {} directory exist?", directory.getAbsoluteFile().getPath()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(format("Expected only 1 sk file in {} but found {}", directory.getAbsoluteFile().getPath(), matches.length));
        }
        return matches[0];
    }

    @SuppressWarnings("serial")
    private static class PeerEnrollment implements Enrollment, Serializable {
        PrivateKey key;
        String cert;

        PeerEnrollment(PrivateKey key, String cert) {
            super();
            this.key = key;
            this.cert = cert;
        }

        @Override
        public PrivateKey getKey() {
            return key;
        }

        @Override
        public String getCert() {
            return cert;
        }

    }

    private void setPeerAdmin(SampleOrg org) throws Exception {
        String sampleOrgName = org.getName();
        String sampleOrgDomainName = org.getDomainName();

        SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, org.getMSPID(),
                        Util.findFileSk(Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName))
                                        .toFile()),
                        Paths.get(testConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                                        format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());

        org.setPeerAdmin(peerOrgAdmin); // A special user that can create channels, join peers and install chaincode
    }

    public TestConfig getTestconfig() {
        return testConfig;
    }

    public SampleStore getSampleStore() {
        return sampleStore;
    }

    public Collection<SampleOrg> getTestSampleOrgs() {
        return testSampleOrgs;
    }

    public Map<String, SampleOrg> getOrgs1to3() {
        return orgs1to3;
    }

    public Map<String, HFClient> getClients1to3() {
        return clients1to3;
    }

    private void sleepAWhile() throws InterruptedException {
        if (sleep > 0) {
            System.out.println("Nap time");
            TimeUnit.SECONDS.sleep(sleep);
        }
    }

}
