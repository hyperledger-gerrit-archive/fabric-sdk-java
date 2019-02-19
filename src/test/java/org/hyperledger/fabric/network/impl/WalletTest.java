package org.hyperledger.fabric.network.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.util.Set;

import org.hyperledger.fabric.network.Wallet;
import org.hyperledger.fabric.network.Wallet.Identity;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public abstract class WalletTest {
  Wallet wallet = null;
  Identity identity1 = null;
  Identity identity2 = null;
  Path certPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "network");

  private Identity createIdentity(String name, String mspid, Path certFile, Path keyFile) throws Exception {
    // read private key
    PrivateKey key = FileSystemWallet.readPrivateKey(keyFile);
    // read certificate
    String cert = readFile(certFile);

    return new WalletIdentity(name, mspid, cert, key);
  }

  private String readFile(Path file) throws IOException {
    if (Files.exists(file)) {
      try (BufferedReader reader = Files.newBufferedReader(file)) {
        StringBuilder sb = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {
          sb.append(line);
          sb.append('\n');
          line = reader.readLine();
        }
        return sb.toString();
      }
    }
    return null;
  }

  @Before
  public void setup() {
    try {
      identity1 = createIdentity("user1", "org1msp", certPath.resolve("User1-cert.pem"), certPath.resolve("User1.priv"));
      identity2 = createIdentity("user2", "org2msp", certPath.resolve("User1-cert.pem"), certPath.resolve("User1.priv"));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testPut() {
    try {
      wallet.put("label1", identity1);
      Assert.assertTrue(wallet.exists("label1"));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGet() {
    try {
      wallet.put("label1", identity1);
      Identity id1 = wallet.get("label1");
      Assert.assertEquals(identity1.getName(), id1.getName());
      Assert.assertEquals(identity1.getMspId(), id1.getMspId());
      Assert.assertEquals(identity1.getEnrollment().getCert(), id1.getEnrollment().getCert());
      Identity id2 = wallet.get("label2");
      Assert.assertNull(id2);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testPutOverwrite() {
    try {
      wallet.put("label1", identity1);
      Assert.assertTrue(wallet.exists("label1"));
      wallet.put("label1", identity2);
      Identity id1 = wallet.get("label1");
      Assert.assertEquals(identity2.getName(), id1.getName());
      Assert.assertEquals(identity2.getMspId(), id1.getMspId());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGetAllLabels() {
    try {
      wallet.put("label1", identity1);
      Set<String> ids = wallet.getAllLabels();
      Assert.assertEquals(ids.size(), 1);
      Assert.assertTrue(ids.contains("label1"));
      wallet.put("label2", identity2);
      ids = wallet.getAllLabels();
      Assert.assertEquals(ids.size(), 2);
      Assert.assertTrue(ids.contains("label1"));
      Assert.assertTrue(ids.contains("label2"));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testRemove() {
    try {
      wallet.put("label1", identity1);
      wallet.put("label2", identity2);
      Set<String> ids = wallet.getAllLabels();
      Assert.assertEquals(ids.size(), 2);
      Assert.assertTrue(ids.contains("label1"));
      Assert.assertTrue(ids.contains("label2"));
      wallet.remove("label2");
      ids = wallet.getAllLabels();
      Assert.assertEquals(ids.size(), 1);
      Assert.assertTrue(ids.contains("label1"));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testRemoveTwice() {
    try {
      wallet.put("label1", identity1);
      wallet.put("label2", identity2);
      wallet.remove("label2");
      Set<String> ids = wallet.getAllLabels();
      Assert.assertEquals(ids.size(), 1);
      Assert.assertTrue(ids.contains("label1"));
      // remove again - should silently ignore
      wallet.remove("label2");
      ids = wallet.getAllLabels();
      Assert.assertEquals(ids.size(), 1);
      Assert.assertTrue(ids.contains("label1"));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }
}
