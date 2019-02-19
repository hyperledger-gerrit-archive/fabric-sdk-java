/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network;

import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public interface Contract {
  Transaction createTransaction(String name);

  String submitTransaction(String name, String... args) throws Exception;

  String evaluateTransaction(String name, String... args) throws ProposalException, InvalidArgumentException;
}
