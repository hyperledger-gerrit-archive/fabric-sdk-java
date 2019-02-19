package org.hyperledger.fabric.network.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hyperledger.fabric.network.Wallet;

public class InMemoryWallet implements Wallet {
  Map<String, Identity> store = new HashMap<String, Identity>();

  @Override
  public void put(String label, Identity identity) throws Exception {
    store.put(label, identity);
  }

  @Override
  public Identity get(String label) {
    return store.get(label);
  }

  @Override
  public Set<String> getAllLabels() {
    return store.keySet();
  }

  @Override
  public void remove(String label) {
    store.remove(label);
  }

  @Override
  public boolean exists(String label) {
    return store.containsKey(label);
  }

}
