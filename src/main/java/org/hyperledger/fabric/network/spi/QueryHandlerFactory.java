package org.hyperledger.fabric.network.spi;

import org.hyperledger.fabric.network.Network;

public interface QueryHandlerFactory {
  QueryHandler create(Network network);
}
