/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network.impl;

import java.util.HashMap;
import java.util.Map;

import org.hyperledger.fabric.network.Gateway;
import org.hyperledger.fabric.network.Network;
import org.hyperledger.fabric.network.Wallet;
import org.hyperledger.fabric.network.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdk.security.CryptoSuiteFactory;

public class GatewayImpl implements Gateway {
  private HFClient client;
  private NetworkConfig networkConfig;
  private Wallet.Identity identity;
  private Map<String, Network> networks = new HashMap<>();

  private GatewayImpl() {
  }

  public static class Builder implements Gateway.Builder {
    private CommitHandlerFactory commitHandler = null;
    private NetworkConfig networkConfig = null;
    private Wallet.Identity identity = null;
    private HFClient client = null;

    public Builder() { }

    @Override
    public Builder networkConfig(NetworkConfig config) {
      this.networkConfig = config;
      return this;
    }

    @Override
    public Builder client(HFClient client) {
      this.client = client;
      return this;
    }

    @Override
    public Builder identity(Wallet wallet, String id) throws Exception {
      this.identity = wallet.get(id);
      return this;
    }

    @Override
    public Builder identity(User user) {
      this.identity = Wallet.Identity.createIdentity(user);
      return null;
    }

    @Override
    public Builder commitHandler(CommitHandlerFactory commitHandler) {
      this.commitHandler = commitHandler;
      return this;
    }

    @Override
    public Gateway connect() throws Exception {
      GatewayImpl gw = new GatewayImpl();
      if (this.client == null) {
        gw.client = HFClient.createNewInstance();
        CryptoSuite cryptoSuite;
        cryptoSuite = CryptoSuiteFactory.getDefault().getCryptoSuite();
        gw.client.setCryptoSuite(cryptoSuite);
        gw.networkConfig = this.networkConfig;
        gw.identity = this.identity;
        gw.client.setUserContext(this.identity);
      } else {
        gw.client = this.client;
      }
      return gw;
    }
  }

  @Override
  public void close() {
  }

  /**
   * Get a network.
   * @param networkName The name of the network (channel).
   * @return network
   * @throws NetworkConfigurationException
   * @throws InvalidArgumentException
   */
  @Override
  public Network getNetwork(String networkName) throws Exception {
    Network network = networks.get(networkName);
    if (network == null) {
      Channel channel = null;
      channel = client.getChannel(networkName);
      if (channel == null && networkConfig != null) {
        channel = client.loadChannelFromConfig(networkName, networkConfig);
      }
      if (channel == null) {
        return null;
      }
      channel.initialize();
      network = new NetworkImpl(channel, this);
      networks.put(networkName, network);
    }
    return network;
  }

  @Override
  public Wallet.Identity getCurrentIdentity() {
    return identity;
  }

  @Override
  public HFClient getClient() {
    return client;
  }

  @Override
  public NetworkConfig getNetworkConfig() {
    return networkConfig;
  }

  // for use by unit tests
  void setNetworkConfig(NetworkConfig config) {
    this.networkConfig = config;
  }

}