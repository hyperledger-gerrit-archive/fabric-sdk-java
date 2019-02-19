package org.hyperledger.fabric.network;

import org.hyperledger.fabric.network.impl.GatewayImpl;
import org.hyperledger.fabric.network.spi.CommitHandlerFactory;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.User;

public interface Gateway extends AutoCloseable {
  Network getNetwork(String networkName) throws Exception;

  Wallet.Identity getCurrentIdentity();

  HFClient getClient();

  NetworkConfig getNetworkConfig();

  static Builder createBuilder() {
    return new GatewayImpl.Builder();
  }

  public interface Builder {
    Builder client(HFClient client);
    Builder networkConfig(NetworkConfig config);
    Builder identity(Wallet wallet, String id) throws Exception;
    Builder identity(User user);
    Builder commitHandler(CommitHandlerFactory commitHandler);

    Gateway connect() throws Exception;
  }
}
