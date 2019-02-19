package org.hyperledger.fabric.network.impl;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hyperledger.fabric.network.Transaction;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionTest {
  TransactionProposalRequest request = null;
  Channel channel = null;
  Transaction transaction = null;

  @Before
  public void setup() throws Exception {
    request = mock(TransactionProposalRequest.class);
    channel = mock(Channel.class);
    transaction = new TransactionImpl(request, channel);
  }

  @Test
  public void testGetName() {
    when(request.getFcn()).thenReturn("txn");
    String name = transaction.getName();
    Assert.assertEquals(name, "txn");
  }

  @Test
  public void testGetTransactionId() {
    String txid = transaction.getTransactionId();
    Assert.assertNull(txid);
  }

  @Test
  public void testSetTransient() {
    transaction.setTransient(null);
    // TODO
  }

  @Test
  public void testEvaluateNoResponses() {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      String result = transaction.evaluate("arg1");
      Assert.assertNull(result);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testEvaluateUnsuccessfulResponse() {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      ProposalResponse response = mock(ProposalResponse.class);
      when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
      responses.add(response);
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      String result = transaction.evaluate("arg1");
      Assert.assertNull(result);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testEvaluateSuccess() {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      ProposalResponse response = mock(ProposalResponse.class);
      when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
      when(response.getChaincodeActionResponsePayload()).thenReturn("successful result".getBytes());
      responses.add(response);
      when(channel.sendTransactionProposal(request)).thenReturn(responses);
      when(channel.sendTransaction(responses)).thenReturn(CompletableFuture.completedFuture(null));

      String result = transaction.evaluate("arg1");
      Assert.assertEquals(result, "successful result");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testSubmitNoResponses() {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      String result = transaction.submit("arg1");
      Assert.assertNull(result);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testSubmitUnsuccessfulResponse() {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      ProposalResponse response = mock(ProposalResponse.class);
      when(response.getStatus()).thenReturn(ChaincodeResponse.Status.FAILURE);
      responses.add(response);
      when(channel.sendTransactionProposal(request)).thenReturn(responses);

      String result = transaction.submit("arg1");
      Assert.assertNull(result);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

  @Test
  public void testSubmitSuccess() {
    try {
      List<ProposalResponse> responses = new ArrayList<>();
      ProposalResponse response = mock(ProposalResponse.class);
      when(response.getStatus()).thenReturn(ChaincodeResponse.Status.SUCCESS);
      when(response.getChaincodeActionResponsePayload()).thenReturn("successful result".getBytes());
      responses.add(response);
      when(channel.sendTransactionProposal(request)).thenReturn(responses);
      when(channel.sendTransaction(responses)).thenReturn(CompletableFuture.completedFuture(null));

      String result = transaction.submit("arg1");
      Assert.assertEquals(result, "successful result");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assert.fail("unexpected exception");
    }
  }

}
