package org.hyperledger.fabric.network.impl;

import java.security.PrivateKey;
import java.util.Set;

import org.hyperledger.fabric.network.Wallet;
import org.hyperledger.fabric.sdk.Enrollment;

public class WalletIdentity implements Wallet.Identity {
  private String mspId;
  private Enrollment enrollment;
  private String name;

  WalletIdentity(String name, String mspId, String certificate, PrivateKey privateKey) {
    this.name = name;
    this.mspId = mspId;
    this.enrollment = new Enrollment() {

      @Override
      public PrivateKey getKey() {
        // TODO Auto-generated method stub
        return privateKey;
      }

      @Override
      public String getCert() {
        // TODO Auto-generated method stub
        return certificate;
      }
    };
  }

  @Override
  public String getMspId() {
    return this.mspId;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Set<String> getRoles() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getAccount() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getAffiliation() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Enrollment getEnrollment() {
    return enrollment;
  }


}
