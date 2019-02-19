package org.hyperledger.fabric.network.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.network.Transaction;
import org.hyperledger.fabric.sdk.ChaincodeResponse;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public class TransactionImpl implements Transaction {
  private TransactionProposalRequest request;
  private Channel channel;
  private String transactionId = null;

  TransactionImpl(TransactionProposalRequest request, Channel channel) {
    this.request = request;
    this.channel = channel;
  }

  @Override
  public String getName() {
    return request.getFcn();
  }

  @Override
  public String getTransactionId() {
    return transactionId;
  }

  @Override
  public void setTransient(Map<String, byte[]> transientData) {
    // TODO Auto-generated method stub

  }

  @Override
  public String submit(String... args)  throws Exception {
    request.setArgs(args);
    Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request);

    // Validate the proposal responses.
    if (proposalResponses.size() < 1) {
        System.err.println("No proposal responses received");
        return null;
    }
    ProposalResponse proposalResponse = proposalResponses.iterator().next();
    transactionId = proposalResponse.getTransactionID();
    if (!proposalResponse.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
        System.err.println("The proposal response was not successful");
        System.err.println("Message: " + proposalResponse.getMessage());
        return null;
    }
    String result = new String(proposalResponse.getChaincodeActionResponsePayload(), StandardCharsets.UTF_8);
    channel.sendTransaction(proposalResponses).get(60, TimeUnit.SECONDS);

    return result;
  }

  @Override
  public String evaluate(String... args) throws ProposalException, InvalidArgumentException {
    request.setArgs(args);
    Collection<ProposalResponse> proposalResponses = channel.sendTransactionProposal(request);

    // Validate the proposal responses.
    if (proposalResponses.size() < 1) {
        System.err.println("No proposal responses received");
        return null;
    }
    ProposalResponse proposalResponse = proposalResponses.iterator().next();
    transactionId = proposalResponse.getTransactionID();
    if (!proposalResponse.getStatus().equals(ChaincodeResponse.Status.SUCCESS)) {
        System.err.println("The proposal response was not successful");
        System.err.println("Message: " + proposalResponse.getMessage());
        return null;
    }
    String result = new String(proposalResponse.getChaincodeActionResponsePayload(), StandardCharsets.UTF_8);
    return result;
  }

}
