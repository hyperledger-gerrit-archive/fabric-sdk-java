package org.hyperledger.fabric.network.impl;

import org.hyperledger.fabric.network.Contract;
import org.hyperledger.fabric.network.Gateway;
import org.hyperledger.fabric.network.Network;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.TestHFClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NetworkTest {
  Channel channel = null;
  Gateway gateway = null;
  Network network = null;

  @Before
  public void setup() {
    try {
      Gateway.Builder builder = Gateway.createBuilder();
      HFClient client = TestHFClient.newInstance();
      channel = client.newChannel("ch1");
      builder.client(client);
      gateway = builder.connect();
      network = gateway.getNetwork("ch1");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testGetChannel() {
    Channel ch1 = network.getChannel();
    Assert.assertEquals(ch1, channel);
  }

  @Test
  public void testGetGateway() {
    Gateway gw = network.getGateway();
    Assert.assertEquals(gw, gateway);
  }

  @Test
  public void testGetContract() {
    Contract contract = network.getContract("contract1");
    Assert.assertTrue(contract instanceof ContractImpl);
  }

  @Test
  public void testGetCachedContract() {
    Contract contract = network.getContract("contract1");
    Contract contract2 = network.getContract("contract1");
    Assert.assertEquals(contract, contract2);
  }


}
