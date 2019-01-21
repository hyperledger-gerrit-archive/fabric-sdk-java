/*
 *
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 * /
 */

package org.hyperledger.fabric.sdk.transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import org.hyperledger.fabric.protos.peer.lifecycle.Lifecycle;

public class LifecycleQueryInstalledChaincodesBuilder extends LifecycleProposalBuilder {
//    private static final Log logger = LogFactory.getLog(QueryInstalledChaincodesBuilder.class);

    private LifecycleQueryInstalledChaincodesBuilder() {
        List<ByteString> argList = new ArrayList<>();
        argList.add(ByteString.copyFrom("QueryInstalledChaincodes", StandardCharsets.UTF_8));
        argList.add(Lifecycle.QueryInstalledChaincodesArgs.getDefaultInstance().toByteString());
        args(argList);
    }

    @Override
    public LifecycleQueryInstalledChaincodesBuilder context(TransactionContext context) {
        super.context(context);
        return this;
    }

    public static LifecycleQueryInstalledChaincodesBuilder newBuilder() {
        return new LifecycleQueryInstalledChaincodesBuilder();
    }

}
