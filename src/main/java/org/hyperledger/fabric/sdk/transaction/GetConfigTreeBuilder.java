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

package org.hyperledger.fabric.sdk.transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.exception.ProposalException;

public class GetConfigTreeBuilder extends CSCCProposalBuilder {
    private static final Log logger = LogFactory.getLog(GetConfigTreeBuilder.class);

    public GetConfigTreeBuilder channelID(String channelID) throws ProposalException {

        if (channelID == null || channelID.isEmpty()) {
            ProposalException exp = new ProposalException("No channelID ");
            GetConfigTreeBuilder.logger.error(exp.getMessage(), exp);
            throw exp;
        }

        List<ByteString> argList = new ArrayList<>();
        argList.add(ByteString.copyFrom("GetConfigTree", StandardCharsets.UTF_8));
        argList.add(ByteString.copyFrom(channelID, StandardCharsets.UTF_8));
        args(argList);
        return this;
    }

    private GetConfigTreeBuilder() {

    }

    @Override
    public GetConfigTreeBuilder context(TransactionContext context) {
        super.context(context);
        return this;
    }

    public static GetConfigTreeBuilder newBuilder() {
        return new GetConfigTreeBuilder();
    }

}

