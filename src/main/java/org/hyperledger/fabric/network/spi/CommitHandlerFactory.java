package org.hyperledger.fabric.network.spi;

import org.hyperledger.fabric.network.Network;

public interface CommitHandlerFactory {
  CommitHandler create(String transactionId, Network network);
}
