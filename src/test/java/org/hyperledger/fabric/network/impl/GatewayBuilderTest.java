package org.hyperledger.fabric.network.impl;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyperledger.fabric.network.Gateway;
import org.hyperledger.fabric.network.Wallet;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.testutils.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GatewayBuilderTest {
  Gateway.Builder builder = null;
  User user = null;
  Path networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "network", "connection.json");

  @Before
  public void setup() {
    builder = Gateway.createBuilder();
    user = TestUtils.getMockUser("user1", "org1msp");
  }

  @Test
  public void testBuilderNoOptions() {
    try {
      builder.connect();
      Assert.fail("connect should have thrown");
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "UserContext is null");
    }
  }

  @Test
  public void testBuilderWithIdentity() {
    builder.identity(user);
    try (Gateway gateway = builder.connect()) {
        Assert.assertEquals(gateway.getCurrentIdentity().getEnrollment(), user.getEnrollment());
        HFClient client = gateway.getClient();
        Assert.assertEquals(client.getUserContext().getEnrollment(), user.getEnrollment());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testBuilderWithWalletIdentity() {
    Wallet wallet = Wallet.createInMemoryWallet();
    try {
      wallet.put("admin", Wallet.Identity.createIdentity(user));
      builder.identity(wallet, "admin");
    } catch (Exception e1) {
      e1.printStackTrace();
      Assert.fail("unexpected exception");
    }
    try (Gateway gateway = builder.connect()) {
        Assert.assertEquals(gateway.getCurrentIdentity().getEnrollment(), user.getEnrollment());
        HFClient client = gateway.getClient();
        Assert.assertEquals(client.getUserContext().getEnrollment(), user.getEnrollment());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testBuilderWithNetworkConfig() {
    try {
      NetworkConfig networkConfig = NetworkConfig.fromJsonFile(networkConfigPath.toFile());
      builder.networkConfig(networkConfig);
      builder.identity(user);
      builder.commitHandler(null);
      Gateway gateway = builder.connect();
      Assert.assertEquals(gateway.getNetworkConfig(), networkConfig);
    } catch (Exception e1) {
      e1.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }
}
