package org.hyperledger.fabric.network.spi;

import java.util.concurrent.CompletableFuture;

public interface QueryHandler {
  CompletableFuture<String> evaluate(Query query);
}
