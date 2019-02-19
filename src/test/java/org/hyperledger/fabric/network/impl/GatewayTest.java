package org.hyperledger.fabric.network.impl;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.hyperledger.fabric.network.Gateway;
import org.hyperledger.fabric.network.Network;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.TestHFClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GatewayTest {
  Gateway.Builder builder = null;
  Channel channel = null;
  static NetworkConfig networkConfig = null;

  @BeforeClass
  public static void initialize() {
    Path networkConfigPath = Paths.get("src", "test", "java", "org", "hyperledger", "fabric", "network", "connection.json");
    try {
      networkConfig = NetworkConfig.fromJsonFile(networkConfigPath.toFile());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Before
  public void setup() {
    try {
      builder = Gateway.createBuilder();
//      User user = TestUtils.getMockUser("user1", "org1msp");
      HFClient client = TestHFClient.newInstance();
      channel = client.newChannel("ch1");
      builder.client(client);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGetNetwork() {
    try (Gateway gateway = builder.connect()) {
      Network network = gateway.getNetwork("ch1");
      Assert.assertEquals(network.getChannel(), channel);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGetCachedNetwork() {
    try (Gateway gateway = builder.connect()) {
      Network network = gateway.getNetwork("ch1");
      Network network2 = gateway.getNetwork("ch1");
      Assert.assertEquals(network, network2);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGetNetworkFromConfig() {
    try (GatewayImpl gateway = (GatewayImpl) builder.connect()) {
      gateway.setNetworkConfig(networkConfig);
      Network network = gateway.getNetwork("mychannel");
      Assert.assertEquals(network.getChannel().getName(), "mychannel");
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGetNonExistentNetwork() {
    try (Gateway gateway = builder.connect()) {
      Network network = gateway.getNetwork("none");
      Assert.assertNull(network);
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

}
