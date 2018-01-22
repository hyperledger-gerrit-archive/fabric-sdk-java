package org.hyperledger.fabric.sdkintegration;

import java.io.File;
import java.nio.file.Paths;

import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;

public class End2endNodeIT extends End2endIT {

    void setupChainCode() {
        this.CHAIN_CODE_FILEPATH = "/sdkintegration/nodecc/sample1";
        this.CHAIN_CODE_PATH = "node_cc/example_cc";
        this.CHAIN_CODE_NAME = "example_cc_node";
        this.CHAIN_CODE_VERSION = "1";
        this.CHAIN_CODE_LANG = Type.NODE;
    }

}
