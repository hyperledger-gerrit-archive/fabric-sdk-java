/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network.impl;

import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.network.Contract;
import org.hyperledger.fabric.network.Network;
import org.hyperledger.fabric.sdk.Channel;

public class NetworkImpl implements Network {
  private Channel channel;
  private GatewayImpl gateway;
  private Map<String, Contract> contracts = new HashMap<>();

  NetworkImpl(Channel channel, GatewayImpl gateway) {
    this.channel = channel;
    this.gateway = gateway;
  }

  @Override
  public Contract getContract(String chaincodeId, String name) {
    String key = chaincodeId + ':' + name;
    Contract contract = contracts.get(key);
    if (contract == null) {
      contract = new ContractImpl(this, gateway, chaincodeId, name);
      contracts.put(key, contract);
    }
    return contract;
  }

  @Override
  public Contract getContract(String chaincodeId) {
    return getContract(chaincodeId, "");
  }

  public Channel getChannel() {
    return channel;
  }

  @Override
  public GatewayImpl getGateway() {
    // TODO Auto-generated method stub
    return gateway;
  }

}
