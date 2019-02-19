/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network;

import org.hyperledger.fabric.network.impl.GatewayImpl;
import org.hyperledger.fabric.sdk.Channel;

public interface Network {
  Contract getContract(String chaincodeId);

  Contract getContract(String chaincodeId, String name);

  Channel getChannel();

  GatewayImpl getGateway();
}