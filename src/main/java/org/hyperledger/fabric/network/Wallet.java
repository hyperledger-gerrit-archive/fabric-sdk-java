/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.network;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.hyperledger.fabric.network.impl.FileSystemWallet;
import org.hyperledger.fabric.network.impl.InMemoryWallet;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;

public interface Wallet {
  static Wallet createFileSystemWallet(Path basePath) throws IOException {
    return new FileSystemWallet(basePath);
  }

  static Wallet createInMemoryWallet() {
    return new InMemoryWallet();
  }

  interface Identity extends User {
    static Identity createIdentity(User user) {
      return new Identity() {

        @Override
        public Set<String> getRoles() {
          return user.getRoles();
        }

        @Override
        public String getName() {
          return user.getName();
        }

        @Override
        public String getMspId() {
          return user.getMspId();
        }

        @Override
        public Enrollment getEnrollment() {
          return user.getEnrollment();
        }

        @Override
        public String getAffiliation() {
          return user.getAffiliation();
        }

        @Override
        public String getAccount() {
          return user.getAccount();
        }
      };
    }
  }

  void put(String label, Identity identity) throws Exception;

  Identity get(String label) throws Exception;

  Set<String> getAllLabels() throws Exception;

  void remove(String label) throws Exception;

  boolean exists(String label);
}
