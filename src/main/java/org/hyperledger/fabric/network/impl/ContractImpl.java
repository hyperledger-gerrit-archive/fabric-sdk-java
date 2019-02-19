package org.hyperledger.fabric.network.impl;

import org.hyperledger.fabric.network.Contract;
import org.hyperledger.fabric.network.Gateway;
import org.hyperledger.fabric.network.Network;
import org.hyperledger.fabric.network.Transaction;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public class ContractImpl implements Contract {
  private final Network network;
  private final Gateway gateway;
  private final String chaincodeId;
  private final String name;

  ContractImpl(Network network, Gateway gateway, String chaincodeId, String name) {
    this.network = network;
    this.gateway = gateway;
    this.chaincodeId = chaincodeId;
    this.name = name;
  }

  @Override
  public Transaction createTransaction(String name) {
    String qualifiedName = getQualifiedName(name);
    TransactionProposalRequest proposalRequest = gateway.getClient().newTransactionProposalRequest();
    proposalRequest.setChaincodeID(ChaincodeID.newBuilder().setName(chaincodeId).build());
    proposalRequest.setFcn(qualifiedName);
    Transaction txn = new TransactionImpl(proposalRequest, network.getChannel());
    return txn;
  }

  @Override
  public String submitTransaction(String name, String... args) throws Exception {
    return createTransaction(name).submit(args);
  }

  @Override
  public String evaluateTransaction(String name, String... args) throws ProposalException, InvalidArgumentException {
    return createTransaction(name).evaluate(args);
  }

  private String getQualifiedName(String tname) {
    return this.name.isEmpty() ? tname : this.name + ':' + tname;
  }
}
