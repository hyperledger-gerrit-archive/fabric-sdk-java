/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk.transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeDeploymentSpec;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.sdk.ChaincodeCollectionConfiguration;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.ChaincodeCollectionConfigurationException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import static org.hyperledger.fabric.sdk.transaction.ProtoUtils.createDeploymentSpec;

public class InstantiateProposalBuilder extends LSCCProposalBuilder {

    private static final Log logger = LogFactory.getLog(InstantiateProposalBuilder.class);

    private String chaincodePath;

    private String chaincodeName;
    private List<String> argList = Collections.emptyList();
    private String chaincodeVersion;

    private byte[] chaincodePolicy = null;
    private byte[] chaincodeCollectionConfiguration = null;
    protected String action = "deploy";

    protected InstantiateProposalBuilder() {
        super();
    }

    public static InstantiateProposalBuilder newBuilder() {
        return new InstantiateProposalBuilder();

    }

    public InstantiateProposalBuilder chaincodePath(String chaincodePath) {

        this.chaincodePath = chaincodePath;

        return this;

    }

    public InstantiateProposalBuilder chaincodeName(String chaincodeName) {

        this.chaincodeName = chaincodeName;

        return this;

    }

    public void chaincodEndorsementPolicy(ChaincodeEndorsementPolicy policy) {
        if (policy != null) {
            this.chaincodePolicy = policy.getChaincodeEndorsementPolicyAsBytes();
        }
    }

    public void chaincodeCollectionConfiguration(ChaincodeCollectionConfiguration chaincodeCollectionConfiguration) throws ChaincodeCollectionConfigurationException {
        if (chaincodeCollectionConfiguration != null) {
            this.chaincodeCollectionConfiguration = chaincodeCollectionConfiguration.getAsBytes();
        }
    }

    public InstantiateProposalBuilder argss(List<String> argList) {
        this.argList = argList;
        return this;
    }

    @Override
    public FabricProposal.Proposal build() throws ProposalException {

        constructInstantiateProposal();
        return super.build();
    }

    private void constructInstantiateProposal() throws ProposalException {

        try {

            createNetModeTransaction();

        } catch (RuntimeException exp) {
            logger.error(exp);
            throw exp;
        } catch (Exception exp) {
            logger.error(exp);
            throw new ProposalException("IO Error while creating install transaction", exp);
        }
    }

    private void createNetModeTransaction() {
        if (request == null) {
            throw new IllegalStateException("Request not set");
        }

        List<String> modlist = new ArrayList<>();
        final String functionName = request.getFcn();
        if (functionName != null && !functionName.isEmpty()) {
            modlist.add(functionName);
            modlist.addAll(argList);
        }

        ChaincodeDeploymentSpec depspec = createDeploymentSpec(ccType,
                chaincodeName, chaincodePath, chaincodeVersion, modlist, null);

        List<ByteString> argList = new ArrayList<>();

        argList.add(ByteString.copyFromUtf8(action)); // command
        argList.add(ByteString.copyFromUtf8(context.getChannelID())); //channel name.
        argList.add(depspec.toByteString());
        if (chaincodePolicy != null) {
            argList.add(ByteString.copyFrom(chaincodePolicy));
        } else if (null != chaincodeCollectionConfiguration) {
            argList.add(ByteString.EMPTY); //place holder for chaincodePolicy
        }

        if (null != chaincodeCollectionConfiguration) {
            argList.add(ByteString.EMPTY); //escc name place holder
            argList.add(ByteString.EMPTY); //vscc name place holder
            argList.add(ByteString.copyFrom(chaincodeCollectionConfiguration));
        }

        args(argList);
    }

    public void chaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;
    }
}