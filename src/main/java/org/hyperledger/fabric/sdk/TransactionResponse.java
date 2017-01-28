/*
 *  Copyright 2016 DTCC, Fujitsu Australia Software Technology - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import static org.hyperledger.fabric.sdk.TransactionResponse.Status.UNDEFINED;

public class TransactionResponse {
    private Status status = UNDEFINED;
    private String message = null;
    private String transactionID = null;
    private String chainCodeID = null;
    public TransactionResponse(String transactionID, String chainCodeID, Status status, String message) {
        construct(transactionID, chainCodeID, status, message);
    }

    public TransactionResponse(String transactionID, String chainCodeID, int statusVal, String message) {
        Status status = Status.findByValue(statusVal);

        construct(transactionID, chainCodeID, status, message);
    }

    private void construct(String transactionID, String chainCodeID, Status status, String message) {
        this.status = status;
        this.message = message;
        this.transactionID = transactionID;
        this.chainCodeID = chainCodeID;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the transactionID
     */
    public String getTransactionID() {
        return transactionID;
    }

    /**
     * @return the chainCodeID
     */
    public String getChainCodeID() {
        return chainCodeID;
    }

    public enum Status {
        UNDEFINED(0),
        SUCCESS(200),
        FAILURE(500);

        private int status = 0;

        Status(int status) {
            this.status = status;
        }

        public int getValue() {
            return this.status;
        }

        public static Status findByValue(final int value) {
            for (Status status : values()) {
                if (value == status.getValue()) {
                    return status;
                }
            }
            return UNDEFINED;
        }
    }

}
