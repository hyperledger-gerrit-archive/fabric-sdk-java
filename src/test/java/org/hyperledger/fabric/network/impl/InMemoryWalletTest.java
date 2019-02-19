package org.hyperledger.fabric.network.impl;

import org.hyperledger.fabric.network.Wallet;
import org.junit.Before;

public class InMemoryWalletTest extends WalletTest {

  @Before
  public void setup() {
    super.setup();
    wallet = Wallet.createInMemoryWallet();
  }

}
