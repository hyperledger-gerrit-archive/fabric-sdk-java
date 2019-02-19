package org.hyperledger.fabric.network.impl;

import java.util.Map;

import org.hyperledger.fabric.network.Contract;
import org.hyperledger.fabric.network.Gateway;
import org.hyperledger.fabric.network.Network;
import org.hyperledger.fabric.network.Transaction;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.TestHFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ContractTest {
  Channel channel = null;
  Gateway gateway = null;
  Network network = null;
  Contract contract = null;
  Contract nscontract = null;

  @Before
  public void setup() {
    try {
      Gateway.Builder builder = Gateway.createBuilder();
      HFClient client = TestHFClient.newInstance();
      channel = client.newChannel("ch1");
      builder.client(client);
      gateway = builder.connect();
      network = gateway.getNetwork("ch1");
      contract = network.getContract("contract1");
      nscontract = network.getContract("contract2", "name1");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testCreateTransaction() {
    Transaction txn = contract.createTransaction("txn1");
    Assert.assertEquals(txn.getName(), "txn1");
  }

  @Test
  public void testCreateTransactionWithNamespace() {
    Transaction txn = nscontract.createTransaction("txn2");
    Assert.assertEquals(txn.getName(), "name1:txn2");
  }

  @Test
  public void testSubmitTransaction() {
    try {
      Contract mockContract = new MockContract(network, gateway, "contract1", "");
      String result = mockContract.submitTransaction("txn1", "arg1");
      Assert.assertEquals(result, "success");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testEvaluateTransaction() {
    try {
      Contract mockContract = new MockContract(network, gateway, "contract1", "");
      String result = mockContract.evaluateTransaction("txn1", "arg1");
      Assert.assertEquals(result, "results");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  class MockContract extends ContractImpl {

    MockContract(Network network, Gateway gateway, String chaincodeId, String name) {
      super(network, gateway, chaincodeId, name);
    }

    @Override
    public Transaction createTransaction(String name) {
      return new Transaction() {

        @Override
        public String submit(String... args) throws ProposalException, InvalidArgumentException {
          return "success";
        }

        @Override
        public void setTransient(Map<String, byte[]> transientData) {
        }

        @Override
        public String getTransactionId() {
          return null;
        }

        @Override
        public String getName() {
          return name;
        }

        @Override
        public String evaluate(String... args) throws ProposalException, InvalidArgumentException {
          return "results";
        }
      };
    }

  }
}
