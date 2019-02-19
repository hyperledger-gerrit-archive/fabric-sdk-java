package org.hyperledger.fabric.network.impl;

import java.io.IOException;
import java.nio.file.Path;

import org.hyperledger.fabric.network.Wallet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemWalletTest extends WalletTest {

  @Rule
  public TemporaryFolder basePath = new TemporaryFolder();

  @Override
  @Before
  public void setup() {
    super.setup();
    try {
      wallet = Wallet.createFileSystemWallet(basePath.getRoot().toPath());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testCreateFolder() {
    // create a wallet instance for non-existing folder
    try {
      Path tempDir = basePath.getRoot().toPath().resolve("temp");
      Wallet existing = Wallet.createFileSystemWallet(tempDir);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

}
