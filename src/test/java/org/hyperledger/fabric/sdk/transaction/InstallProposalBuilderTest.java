/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk.transaction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.hyperledger.fabric.sdk.TransactionRequest;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class InstallProposalBuilderTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // Create a temp folder to hold temp files for various file I/O operations
    // These are automatically deleted when each test completes
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testBuildNoChaincode() throws Exception {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing chaincodeSource or chaincodeInputStream");

        InstallProposalBuilder builder = createTestBuilder();

        builder.build();

    }

    // Tests that both chaincodeSource and chaincodeInputStream are not specified together
    @Test
    public void testBuildBothChaincodeSources() throws Exception {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Both chaincodeSource and chaincodeInputStream");

        InstallProposalBuilder builder = createTestBuilder();

        builder.setChaincodeSource(new File("some/dir"));
        builder.setChaincodeInputStream(new ByteArrayInputStream("test string".getBytes()));

        builder.build();
    }

    // Tests that a chaincode path has been specified
    @Test
    public void testBuildChaincodePath() throws Exception {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing chaincodePath");

        InstallProposalBuilder builder = createTestBuilder();

        builder.setChaincodeSource(new File("some/dir"));
        builder.chaincodePath(null);

        builder.build();
    }

    // Tests for non existent chaincode source path
    @Test
    public void testBuildSourceNotExistGolang() throws Exception {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The project source directory does not exist");

        InstallProposalBuilder builder = createTestBuilder();

        builder.setChaincodeLanguage(TransactionRequest.Type.JAVA);
        builder.chaincodePath("/some/path");
        builder.setChaincodeSource(new File("some/dir"));

        builder.build();
    }

    // Tests for a chaincode source path which is a file and not a directory
    @Test
    public void testBuildSourceNotDirectory() throws Exception {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("The project source directory is not a directory");

        InstallProposalBuilder builder = createTestBuilder();

        // create an empty temp file
        tempFolder.newFile("src");

        builder.chaincodePath("");
        builder.setChaincodeSource(tempFolder.getRoot().getAbsoluteFile());

        builder.build();
    }

    // Tests for an empty directory
    @Test
    public void testBuildInvalidSource() throws Exception {

        // A mock InputStream that throws an IOException
        class MockInputStream extends InputStream {
            @Override
            public int read() throws IOException {
                throw new IOException("Cannot read!");
            }
        }

        thrown.expect(ProposalException.class);
        thrown.expectMessage("IO Error");

        InstallProposalBuilder builder = createTestBuilder();

        builder.setChaincodeInputStream(new MockInputStream());

        builder.build();
    }


    // ==========================================================================================
    // Helper methods
    // ==========================================================================================

    // Instantiates a basic InstallProposalBuilder with no chaincode source specified
    private InstallProposalBuilder createTestBuilder() {

        InstallProposalBuilder builder = InstallProposalBuilder.newBuilder();

        builder.chaincodeName("mycc");
        builder.chaincodeVersion("1.0");
        builder.setChaincodeLanguage(TransactionRequest.Type.GO_LANG);

        return builder;
    }

}
