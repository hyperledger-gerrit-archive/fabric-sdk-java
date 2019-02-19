package org.hyperledger.fabric.network.spi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hyperledger.fabric.sdk.Peer;

public interface Query {
  CompletableFuture<Map<String, String>> evaluate(List<Peer> peers);
}
