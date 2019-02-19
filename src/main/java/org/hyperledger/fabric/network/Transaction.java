/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network;

import java.util.Map;

import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public interface Transaction {
  String getName();

  String getTransactionId();

  void setTransient(Map<String, byte[]> transientData);

  String submit(String... args) throws Exception;

  String evaluate(String... args) throws ProposalException, InvalidArgumentException;
}
