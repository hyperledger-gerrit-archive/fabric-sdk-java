/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk.transaction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;
import com.google.protobuf.ByteString;
import io.netty.util.internal.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeDeploymentSpec;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeID;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeInput;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec;
import org.hyperledger.fabric.protos.peer.Chaincode.ChaincodeSpec.Type;
import org.hyperledger.fabric.protos.peer.FabricProposal;
import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.helper.SDKUtil;


public class InstallProposalBuilder extends ProposalBuilder {


    private Log logger = LogFactory.getLog(InstallProposalBuilder.class);
    private String chaincodePath;


    private String chaincodeSource;
    private String chaincodeName;
    private String chaincodeVersion;
    private List<String> argList;
    private TransactionRequest.Type chaincodeLanguage;


    private InstallProposalBuilder() {
        super();
    }

    public static InstallProposalBuilder newBuilder() {
        return new InstallProposalBuilder();


    }


    public InstallProposalBuilder chaincodePath(String chaincodePath) {

        this.chaincodePath = chaincodePath;

        return this;

    }

    public InstallProposalBuilder chaincodeName(String chaincodeName) {

        this.chaincodeName = chaincodeName;

        return this;

    }

    public InstallProposalBuilder argss(List<String> argList) {
        this.argList = argList;
        return this;
    }

    public InstallProposalBuilder setChaincodeSource(String chaincodeSource) {
        this.chaincodeSource = chaincodeSource;

        return this;
    }

    @Override
    public FabricProposal.Proposal build() throws Exception {

        constructInstallProposal();
        return super.build();
    }

    private static String LCCC_CHAIN_NAME = "lccc";


    private void constructInstallProposal() throws ProposalException {


        try {

            if (context.isDevMode()) {
                createDevModeTransaction();
            } else {
                createNetModeTransaction();
            }

        } catch (Exception exp) {
            logger.error(exp);
            throw new ProposalException("IO Error while creating install proposal", exp);
        }
    }

    private void createNetModeTransaction() throws Exception {
        logger.debug("newNetModeTransaction");

        // Verify that chaincodePath is being passed
        if (StringUtil.isNullOrEmpty(chaincodePath)) {
            throw new IllegalArgumentException("[NetMode] Missing chaincodePath in DeployRequest");
        }

        String rootDir = "";
        String chaincodeDir = "";


        Type ccType = Type.GOLANG;

        String projDir = null;
        String pathPrefix = null;

        if (chaincodeLanguage == TransactionRequest.Type.GO_LANG) {
            // Determine the user's $GOPATH
            if (chaincodeSource == null) {
                chaincodeSource = System.getenv("GOPATH");
            }
            String goPath = System.getenv("GOPATH");
            logger.info(String.format("Using GOPATH :%s", goPath));
            if (StringUtil.isNullOrEmpty(chaincodeSource)) {
                throw new IllegalArgumentException("[NetMode] chaincodeSource or set GOPATH");
            }

            logger.debug("chaincodeSource " + chaincodeSource);

            // Compose the path to the chaincode project directory
            // rootDir = SDKUtil.combinePaths(chaincodeSource, "src");
            //    chaincodeDir = chaincodePath;
            projDir = SDKUtil.combinePaths(chaincodeSource, "src", chaincodePath);
            pathPrefix = SDKUtil.combinePaths("src", chaincodePath);
            //   filterpath = FilenameUtils.separatorsToUnix(SDKUtil.combinePaths("src", chaincodePath)) + "/";


        } else {
            ccType = Type.JAVA;

            if (StringUtil.isNullOrEmpty(chaincodeSource)) {
                throw new IllegalArgumentException("[NetMode] chaincodeSource ");
            }

            // Compose the path to the chaincode project directory
            File ccFile = new File(chaincodeSource);
            rootDir = ccFile.getParent();
            chaincodeDir = ccFile.getName();

            projDir = SDKUtil.combinePaths(rootDir, chaincodeDir);
        }


        logger.debug("projDir: " + projDir);

        String dockerFilePath = null;
        //    String targzFilePath = null;
        ChaincodeDeploymentSpec depspec = null;
        String dockerFileContents = getDockerFileContents(chaincodeLanguage);
        try {
            if (dockerFileContents != null) {


                dockerFileContents = String.format(dockerFileContents, chaincodeName);

                // Create a Docker file with dockerFileContents
                dockerFilePath = SDKUtil.combinePaths(projDir, "Dockerfile");
                Files.write(dockerFileContents.getBytes(), new File(dockerFilePath));

                logger.debug(String.format("Created Dockerfile at [%s]", dockerFilePath));
            }


            byte[] data = SDKUtil.generateTarGz(projDir, pathPrefix);


            depspec = createDeploymentSpec(ccType,
                    chaincodeName, chaincodePath, chaincodeVersion, data);
        } finally {
            // Clean up temporary files
//            if (targzFilePath != null)
//                SDKUtil.deleteFileOrDirectory(new File(targzFilePath));
            if (dockerFilePath != null)
                SDKUtil.deleteFileOrDirectory(new File(dockerFilePath));
        }


        List<ByteString> argList = new ArrayList<>();
//        argList.add(ByteString.copyFrom("deploy", StandardCharsets.UTF_8));
//        argList.add(ByteString.copyFrom("default", StandardCharsets.UTF_8));
        argList.add(ByteString.copyFrom("install", StandardCharsets.UTF_8));
        argList.add(depspec.toByteString());

        ChaincodeID lcccID = ChaincodeID.newBuilder().setName(LCCC_CHAIN_NAME).build();

        args(argList);
        chaincodeID(lcccID);
        ccType(ccType);

    }


    private void createDevModeTransaction() {
        logger.debug("newDevModeTransaction");


        ChaincodeDeploymentSpec depspec = createDeploymentSpec(Type.GOLANG,
                chaincodeName, null, null, null);

        ChaincodeID lcccID = ChaincodeID.newBuilder().setName(LCCC_CHAIN_NAME).build();

        List<ByteString> argList = new ArrayList<>();
//        argList.add(ByteString.copyFrom("deploy", StandardCharsets.UTF_8));
//        argList.add(ByteString.copyFrom("default", StandardCharsets.UTF_8));
        argList.add(ByteString.copyFrom("install", StandardCharsets.UTF_8));
        argList.add(depspec.toByteString());


        super.args(argList);
        super.chaincodeID(lcccID);


    }


    private ChaincodeDeploymentSpec createDeploymentSpec(Type ccType, String name, String chaincodePath, String chaincodeVersion,  byte[] codePackage) {
        logger.trace("Creating deployment Specification.");

        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(name);
        if (chaincodePath != null) {
            chaincodeIDBuilder = chaincodeIDBuilder.setPath(chaincodePath);
        }

        chaincodeIDBuilder.setVersion(chaincodeVersion);

        ChaincodeID chaincodeID = chaincodeIDBuilder.build();

        // build chaincodeInput
        List<ByteString> argList = new ArrayList<>(1);
        argList.add(ByteString.copyFrom("init", StandardCharsets.UTF_8));
//        for (String arg : args) {
//            argList.add(ByteString.copyFrom(arg.getBytes()));
//        }
        ChaincodeInput chaincodeInput = ChaincodeInput.newBuilder().addAllArgs(argList).build();

        // Construct the ChaincodeSpec
        ChaincodeSpec chaincodeSpec = ChaincodeSpec.newBuilder().setType(ccType).setChaincodeId(chaincodeID)
                .setInput(chaincodeInput)
                .build();


        ChaincodeDeploymentSpec.Builder chaincodeDeploymentSpecBuilder = ChaincodeDeploymentSpec
                .newBuilder().setChaincodeSpec(chaincodeSpec).setEffectiveDate(context.getFabricTimestamp())
                .setExecEnv(ChaincodeDeploymentSpec.ExecutionEnvironment.DOCKER);
        chaincodeDeploymentSpecBuilder.setCodePackage(ByteString.copyFrom(codePackage));

        return chaincodeDeploymentSpecBuilder.build();

    }


    private String getDockerFileContents(TransactionRequest.Type lang) throws IOException {
        if (chaincodeLanguage == TransactionRequest.Type.GO_LANG) {
            return null; //No dockerfile for GO
        } else if (chaincodeLanguage == TransactionRequest.Type.JAVA) {
            return new String(SDKUtil.readFileFromClasspath("Java.Docker"));
        }

        throw new UnsupportedOperationException(String.format("Unknown chaincode language: %s", lang));
    }


    public void setChaincodeLanguage(TransactionRequest.Type chaincodeLanguage) {
        this.chaincodeLanguage = chaincodeLanguage;
    }


    public void chaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;
    }
}