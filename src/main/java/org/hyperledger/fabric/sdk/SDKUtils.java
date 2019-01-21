/*
 *
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hyperledger.fabric.sdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.hyperledger.fabric.protos.peer.Chaincode;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SDKUtils {
    private static final Log logger = LogFactory.getLog(SDKUtils.class);
    private static final boolean IS_DEBUG_LEVEL = logger.isDebugEnabled();

    private SDKUtils() {

    }

    public static CryptoSuite suite = null;

    /**
     * used asn1 and get hash
     *
     * @param blockNumber
     * @param previousHash
     * @param dataHash
     * @return byte[]
     * @throws IOException
     * @throws InvalidArgumentException
     */
    public static byte[] calculateBlockHash(HFClient client, long blockNumber, byte[] previousHash, byte[] dataHash) throws IOException, InvalidArgumentException {

        if (previousHash == null) {
            throw new InvalidArgumentException("previousHash parameter is null.");
        }
        if (dataHash == null) {
            throw new InvalidArgumentException("dataHash parameter is null.");
        }
        if (null == client) {
            throw new InvalidArgumentException("client parameter is null.");
        }

        CryptoSuite cryptoSuite = client.getCryptoSuite();
        if (null == cryptoSuite) {
            throw new InvalidArgumentException("Client crypto suite has not  been set.");
        }

        ByteArrayOutputStream s = new ByteArrayOutputStream();
        DERSequenceGenerator seq = new DERSequenceGenerator(s);
        seq.addObject(new ASN1Integer(blockNumber));
        seq.addObject(new DEROctetString(previousHash));
        seq.addObject(new DEROctetString(dataHash));
        seq.close();
        return cryptoSuite.hash(s.toByteArray());

    }

    /**
     * Check that the proposals all have consistent read write sets
     *
     * @param proposalResponses
     * @return A Collection of sets where each set has consistent proposals.
     * @throws InvalidArgumentException
     */

    public static Collection<Set<ProposalResponse>> getProposalConsistencySets(Collection<ProposalResponse> proposalResponses
    ) throws InvalidArgumentException {

        return getProposalConsistencySets(proposalResponses, new HashSet<ProposalResponse>());

    }

    /**
     * Check that the proposals all have consistent read write sets
     *
     * @param proposalResponses
     * @param invalid           proposals that were found to be invalid.
     * @return A Collection of sets where each set has consistent proposals.
     * @throws InvalidArgumentException
     */

    public static Collection<Set<ProposalResponse>> getProposalConsistencySets(Collection<ProposalResponse> proposalResponses,
                                                                               Set<ProposalResponse> invalid) throws InvalidArgumentException {

        if (proposalResponses == null) {
            throw new InvalidArgumentException("proposalResponses collection is null");
        }

        if (proposalResponses.isEmpty()) {
            throw new InvalidArgumentException("proposalResponses collection is empty");
        }

        if (null == invalid) {
            throw new InvalidArgumentException("invalid set is null.");
        }

        HashMap<ByteString, Set<ProposalResponse>> ret = new HashMap<>();

        for (ProposalResponse proposalResponse : proposalResponses) {

            if (proposalResponse.isInvalid()) {
                invalid.add(proposalResponse);
            } else {
                // payload bytes is what's being signed over so it must be consistent.
                final ByteString payloadBytes = proposalResponse.getPayloadBytes();

                if (payloadBytes == null) {
                    throw new InvalidArgumentException(format("proposalResponse.getPayloadBytes() was null from peer: %s.",
                            proposalResponse.getPeer()));
                } else if (payloadBytes.isEmpty()) {
                    throw new InvalidArgumentException(format("proposalResponse.getPayloadBytes() was empty from peer: %s.",
                            proposalResponse.getPeer()));
                }
                Set<ProposalResponse> set = ret.computeIfAbsent(payloadBytes, k -> new HashSet<>());
                set.add(proposalResponse);
            }
        }

        if (IS_DEBUG_LEVEL && ret.size() > 1) {

            StringBuilder sb = new StringBuilder(1000);

            int i = 0;
            String sep = "";

            for (Map.Entry<ByteString, Set<ProposalResponse>> entry : ret.entrySet()) {
                ByteString bytes = entry.getKey();
                Set<ProposalResponse> presp = entry.getValue();

                sb.append(sep)
                        .append("Consistency set: ").append(i++).append(" bytes size: ").append(bytes.size())
                        .append(" bytes: ")
                        .append(Utils.toHexString(bytes.toByteArray())).append(" [");

                String psep = "";

                for (ProposalResponse proposalResponse : presp) {
                    sb.append(psep).append(proposalResponse.getPeer());
                    psep = ", ";
                }
                sb.append("]");
                sep = ", ";
            }

            logger.debug(sb.toString());

        }

        return ret.values();

    }

    //{"Path":"github.com/hyperledger/fabric/integration/chaincode/simple/cmd","Type":"golang"}[

    public static byte[] generatePlusLifeCycleChaincodeDataPackageBytes(File chaincodeSource, TransactionRequest.Type chaincodeType, File chaincodeMetaInfLocation, String chaincodePath) throws IOException {

        byte[] mataDataBytes = generatePackageMataDataBytes(chaincodePath, chaincodeType);
        byte[] dataBytes = generatePackageDataBytes(chaincodeSource, chaincodeMetaInfLocation, chaincodeType, chaincodePath);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(500000);

        // String sourcePath = sourceDirectory.getAbsolutePath();

        TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(new GzipCompressorOutputStream(bos));
        archiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

        TarArchiveEntry archiveEntry = new TarArchiveEntry("Chaincode-Package-Metadata.json");
        archiveEntry.setMode(0100644);
        archiveEntry.setSize(mataDataBytes.length);
        archiveOutputStream.putArchiveEntry(archiveEntry);
        archiveOutputStream.write(mataDataBytes);
        archiveOutputStream.closeArchiveEntry();

        archiveEntry = new TarArchiveEntry(chaincodeType.toPackageName().toUpperCase() + "-Code-Package.tar.gz");
        archiveEntry.setMode(0100644);
        archiveEntry.setSize(dataBytes.length);
        archiveOutputStream.putArchiveEntry(archiveEntry);
        archiveOutputStream.write(dataBytes);
        archiveOutputStream.closeArchiveEntry();
        archiveOutputStream.close();

        return bos.toByteArray();
    }

    public static void generatePlusLifeCycleChaincodeDataPackageFile(File outputFile, File chaincodeSource, TransactionRequest.Type chaincodeType, File chaincodeMetaInfLocation, String chaincodePath) throws IOException {

        byte[] bytes = generatePlusLifeCycleChaincodeDataPackageBytes(chaincodeSource, chaincodeType, chaincodeMetaInfLocation, chaincodePath);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        fileOutputStream.write(bytes);
        fileOutputStream.close();

    }

    private static byte[] generatePackageMataDataBytes(String path, TransactionRequest.Type type) {

        return ("{\"Path\":" + new Gson().toJson(path) + ",\"Type\":\"" + type.toPackageName() + "\"}").getBytes(UTF_8); // simple string now.

    }

    private static byte[] generatePackageDataBytes(File chaincodeSource, File chaincodeMetaInfLocation, TransactionRequest.Type chaincodeLanguage, String chaincodePath) throws IOException {
        logger.debug("createNetModeTransaction");

        if (null == chaincodeSource) {
            throw new IllegalArgumentException("Missing chaincodeSource or chaincodeInputStream in InstallRequest");
        }

        final Chaincode.ChaincodeSpec.Type ccType;
        File projectSourceDir = null;
        String targetPathPrefix = null;
        String dplang;

        File metainf = null;
        if (null != chaincodeMetaInfLocation) {
            if (!chaincodeMetaInfLocation.exists()) {
                throw new IllegalArgumentException(format("Directory to find chaincode META-INF %s does not exist", chaincodeMetaInfLocation.getAbsolutePath()));
            }

            if (!chaincodeMetaInfLocation.isDirectory()) {
                throw new IllegalArgumentException(format("Directory to find chaincode META-INF %s is not a directory", chaincodeMetaInfLocation.getAbsolutePath()));
            }
            metainf = new File(chaincodeMetaInfLocation, "META-INF");
            logger.trace("META-INF directory is " + metainf.getAbsolutePath());
            if (!metainf.exists()) {

                throw new IllegalArgumentException(format("The META-INF directory does not exist in %s", chaincodeMetaInfLocation.getAbsolutePath()));
            }

            if (!metainf.isDirectory()) {
                throw new IllegalArgumentException(format("The META-INF in %s is not a directory.", chaincodeMetaInfLocation.getAbsolutePath()));
            }
            File[] files = metainf.listFiles();

            if (files == null) {
                throw new IllegalArgumentException("null for listFiles on: " + chaincodeMetaInfLocation.getAbsolutePath());
            }

            if (files.length < 1) {

                throw new IllegalArgumentException(format("The META-INF directory %s is empty.", metainf.getAbsolutePath()));
            }

            logger.trace(format("chaincode META-INF found %s", metainf.getAbsolutePath()));

        }

        switch (chaincodeLanguage) {
            case GO_LANG:

                // chaincodePath is mandatory
                // chaincodeSource may be a File or InputStream

                //   Verify that chaincodePath is being passed
                if (Utils.isNullOrEmpty(chaincodePath)) {
                    throw new IllegalArgumentException("Missing chaincodePath in InstallRequest");
                }

                dplang = "Go";
                ccType = Chaincode.ChaincodeSpec.Type.GOLANG;
                if (null != chaincodeSource) {

                    projectSourceDir = Paths.get(chaincodeSource.toString(), "src", chaincodePath).toFile();
                    targetPathPrefix = Paths.get("src", chaincodePath).toString();
                }
                break;

            case JAVA:

                // chaincodePath is not applicable and must be null
                // chaincodeSource may be a File or InputStream

                //   Verify that chaincodePath is null
                if (!Utils.isNullOrEmpty(chaincodePath)) {
                    throw new IllegalArgumentException("chaincodePath must be null for Java chaincode");
                }

                dplang = "Java";
                ccType = Chaincode.ChaincodeSpec.Type.JAVA;
                if (null != chaincodeSource) {
                    targetPathPrefix = "src";
                    projectSourceDir = Paths.get(chaincodeSource.toString()).toFile();
                }
                break;

            case NODE:

                // chaincodePath is not applicable and must be null
                // chaincodeSource may be a File or InputStream

                //   Verify that chaincodePath is null
                if (!Utils.isNullOrEmpty(chaincodePath)) {
                    throw new IllegalArgumentException("chaincodePath must be null for Node chaincode");
                }

                dplang = "Node";
                ccType = Chaincode.ChaincodeSpec.Type.NODE;
                if (null != chaincodeSource) {

                    projectSourceDir = Paths.get(chaincodeSource.toString()).toFile();
                    targetPathPrefix = "src"; //Paths.get("src", chaincodePath).toString();
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected chaincode language: " + chaincodeLanguage);
        }

        byte[] data = null;
        //    String chaincodeID = chaincodeName + "::" + chaincodePath + "::" + chaincodeVersion;

        if (chaincodeSource != null) {
            if (!projectSourceDir.exists()) {
                final String message = "The project source directory does not exist: " + projectSourceDir.getAbsolutePath();
                logger.error(message);
                throw new IllegalArgumentException(message);
            }
            if (!projectSourceDir.isDirectory()) {
                final String message = "The project source directory is not a directory: " + projectSourceDir.getAbsolutePath();
                logger.error(message);
                throw new IllegalArgumentException(message);
            }

//            logger.info(format("Installing '%s' language %s chaincode from directory: '%s' with source location: '%s'. chaincodePath:'%s'",
//                    chaincodeID, dplang, projectSourceDir.getAbsolutePath(), targetPathPrefix, chaincodePath));

            // generate chaincode source tar
            data = Utils.generateTarGz(projectSourceDir, targetPathPrefix, metainf);

//            if (null != diagnosticFileDumper) {
//
//                logger.trace(format("Installing '%s' language %s chaincode from directory: '%s' with source location: '%s'. chaincodePath:'%s' tar file dump %s",
//                        chaincodeID, dplang, projectSourceDir.getAbsolutePath(), targetPathPrefix,
//                        chaincodePath, diagnosticFileDumper.createDiagnosticTarFile(data)));
//            }

        }

        return data;
    }
}
