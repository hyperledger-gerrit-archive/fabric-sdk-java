/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk;

import java.util.Collection;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.helper.Config;
import org.hyperledger.fabric.sdk.helper.Utils;

/**
 * LifecycleApproveChaincodeDefinitionForMyOrgRequest parameters for approving chaincode with lifecycle.
 * Send to peers with {@link Channel#sendLifecycleApproveChaincodeDefinitionForMyOrgProposal(LifecycleApproveChaincodeDefinitionForMyOrgRequest, Collection)}
 */
public class LifecycleApproveChaincodeDefinitionForMyOrgRequest extends LifecycleRequest {
    private static final Config config = Config.getConfig();
    protected String chaincodeName;
    // The version of the chaincode
    protected String chaincodeVersion;

    private String packageId;
    private boolean sourceUnavailable = false; // there is no packageId source
    private long sequence;
    private ChaincodeCollectionConfiguration chaincodeCollectionConfiguration;
    private String chaincodeCodeEndorsementPlugin = null;
    private Boolean initRequired = null;
    private String chaincodeCodeValidationPlugin = null;
    private ByteString validationParameter = null;

    LifecycleApproveChaincodeDefinitionForMyOrgRequest(User userContext) {
        super(userContext);
    }

    public boolean isSourceUnavailable() {
        return sourceUnavailable;
    }

    /**
     * There is no specific packageId for this approval.
     *
     * @param sourceUnavailable
     * @throws InvalidArgumentException
     */
    public void setSourceUnavailable(boolean sourceUnavailable) throws InvalidArgumentException {
        if (packageId != null) {
            throw new InvalidArgumentException("Source none can not be set to true if packageId has been provided already");
        }
        this.sourceUnavailable = sourceUnavailable;
    }

    /**
     * The chaincode validation parameter. Only this or chaincode endorsement policy {@link #setChaincodeEndorsementPolicy(LifecycleChaincodeEndorsementPolicy)} may be set at any one time.
     *
     * @param validationParameter
     * @throws InvalidArgumentException
     */
    public void setValidationParameter(byte[] validationParameter) throws InvalidArgumentException {
        if (null == validationParameter) {
            throw new InvalidArgumentException("The valdiationParameter parameter can not be null.");
        }
        this.validationParameter = ByteString.copyFrom(validationParameter);

    }

    /**
     * The chaincode endorsement policy. Only this or setValdationParamter {@link #setValidationParameter(byte[])} maybe set at any one time.
     *
     * @param lifecycleChaincodeEndorsementPolicy
     * @throws InvalidArgumentException
     */

    public void setChaincodeEndorsementPolicy(LifecycleChaincodeEndorsementPolicy lifecycleChaincodeEndorsementPolicy) throws InvalidArgumentException {
        if (null == lifecycleChaincodeEndorsementPolicy) {
            throw new InvalidArgumentException("The lifecycleChaincodeEndorsementPolicy parameter can not be null.");
        }
        this.validationParameter = lifecycleChaincodeEndorsementPolicy.getByteString();
    }

    public Boolean isInitRequired() {
        return initRequired;
    }

    /**
     * If set the chaincode will need to have an explicit initializer. See {@link TransactionProposalRequest#setInit(boolean)} must be true, for first invoke.
     *
     * @param initRequired set to true in chaincode will need initialization.
     */
    public void setInitRequired(boolean initRequired) {
        this.initRequired = initRequired;
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    /**
     * The name of the chaincode to approve.
     *
     * @param chaincodeName
     */
    public void setChaincodeName(String chaincodeName) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeName)) {
            throw new InvalidArgumentException("The chaincodeName parameter can not be null or empty.");
        }
        this.chaincodeName = chaincodeName;
    }

    public String getChaincodeVersion() {

        return chaincodeVersion;
    }

    /**
     * The version of the chaincode to approve.
     *
     * @param chaincodeVersion the version.
     */

    public void setChaincodeVersion(String chaincodeVersion) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeVersion)) {
            throw new InvalidArgumentException("The chaincodeVersion parameter can not be null or empty.");
        }
        this.chaincodeVersion = chaincodeVersion;

    }

    public long getSequence() {
        return sequence;
    }

    /**
     * The sequence of this change. Latest sequence can be determined from {@link QueryLifecycleQueryChaincodeDefinitionRequest}
     *
     * @param sequence
     */
    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public String getPackageId() {

        return packageId;
    }

    /**
     * The packageId being approved. This is the package id gotten from {@link LifecycleInstallProposalResponse#getPackageId()}
     * or from {@link LifecycleQueryInstalledChaincodesProposalResponse}, {@link LifecycleQueryInstalledChaincodeProposalResponse}
     * <p>
     * Only packageID or the sourceUnavailable to true may be set any time.
     *
     * @param packageId the package ID
     * @throws InvalidArgumentException
     */

    public void setPackageId(String packageId) throws InvalidArgumentException {
        if (sourceUnavailable) {
            throw new InvalidArgumentException("The source none has be set to true already. Can not have packageId set when source none set to true.");
        }
        if (Utils.isNullOrEmpty(packageId)) {
            throw new InvalidArgumentException("The packageId parameter can not be null or empty.");
        }

        this.packageId = packageId;

    }

    public ChaincodeCollectionConfiguration getChaincodeCollectionConfiguration() {
        return this.chaincodeCollectionConfiguration;
    }

    /**
     * The collections configuration for this chaincode;
     *
     * @param chaincodeCollectionConfiguration the collection configurtation {@link ChaincodeCollectionConfiguration}
     * @throws InvalidArgumentException
     */
    public void setChaincodeCollectionConfiguration(ChaincodeCollectionConfiguration chaincodeCollectionConfiguration) throws InvalidArgumentException {
        if (null == chaincodeCollectionConfiguration) {
            throw new InvalidArgumentException("The chaincodeCollectionConfiguration may not be null");
        }
        this.chaincodeCollectionConfiguration = chaincodeCollectionConfiguration;
    }

    public String chaincodeCodeEndorsementPlugin() {
        return chaincodeCodeEndorsementPlugin;
    }

    public String chaincodeCodeValidationPlugin() {

        return chaincodeCodeValidationPlugin;
    }

    /**
     * This is the chaincode endorsement plugin. Should default, not needing set. ONLY set if there is a specific endorsement is set for your organization
     *
     * @param chaincodeCodeEndorsementPlugin
     * @throws InvalidArgumentException
     */
    public void setChaincodeCodeEndorsementPlugin(String chaincodeCodeEndorsementPlugin) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeCodeEndorsementPlugin)) {
            throw new InvalidArgumentException("The chaincodeCodeEndorsementPlugin parameter can not be null or empty.");
        }
        this.chaincodeCodeEndorsementPlugin = chaincodeCodeEndorsementPlugin;
    }

    /**
     * This is the chaincode validation plugin. Should default, not needing set. ONLY set if there is a specific validation is set for your organization
     *
     * @param chaincodeCodeValidationPlugin
     * @throws InvalidArgumentException
     */
    public void setChaincodeCodeValidationPlugin(String chaincodeCodeValidationPlugin) throws InvalidArgumentException {
        if (Utils.isNullOrEmpty(chaincodeCodeValidationPlugin)) {
            throw new InvalidArgumentException("The chaincodeCodeValidationPlugin parameter can not be null or empty.");
        }
        this.chaincodeCodeValidationPlugin = chaincodeCodeValidationPlugin;
    }

    ByteString getValidationParamter() {
        return validationParameter;
    }
}
