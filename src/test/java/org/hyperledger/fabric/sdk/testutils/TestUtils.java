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

package org.hyperledger.fabric.sdk.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import org.hyperledger.fabric.sdk.ChaincodeEvent;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.helper.Config;

import static org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE;

public class TestUtils {

    private TestUtils() {
    }

    /**
     * Sets the value of a field on an object
     *
     * @param o         The object that contains the field
     * @param fieldName The name of the field
     * @param value     The new value
     * @return The previous value of the field
     */
    public static Object setField(Object o, String fieldName, Object value) {
        Object oldVal = null;
        try {
            final Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            oldVal = field.get(o);
            field.set(o, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get value of field " + fieldName, e);
        }
        return oldVal;
    }

    /**
     * Invokes method on object.
     * Used to access private methods.
     *
     * @param o          The object that contains the field
     * @param methodName The name of the field
     * @param args       The arguments.
     * @return Result of method.
     */
    public static Object invokeMethod(Object o, String methodName, Object... args) throws Throwable {
        Object oldVal = null;

        Method[] methods = o.getClass().getDeclaredMethods();
        List<Method> reduce = new ArrayList<>(Arrays.asList(methods));
        for (Iterator<Method> i = reduce.iterator(); i.hasNext();
                ) {
            Method m = i.next();
            if (!methodName.equals(m.getName())) {
                i.remove();
                continue;
            }
            Class<?>[] parameterTypes = m.getParameterTypes();
            if (parameterTypes.length != args.length) {
                i.remove();
                continue;
            }
        }
        if (reduce.isEmpty()) {
            throw new RuntimeException(String.format("TEST ISSUE Could not find method %s on %s with %d arguments.",
                    methodName, o.getClass().getName(), args.length));
        }
        if (reduce.size() > 1) {
            throw new RuntimeException(String.format("TEST ISSUE Could not find unique method %s on %s. Found with %d matches.",
                    methodName, o.getClass().getName(), reduce.size()));
        }

        Method method = reduce.iterator().next();
        method.setAccessible(true);
        try {
            return method.invoke(o, args);
        } catch (IllegalAccessException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

    }

    /**
     * Gets the value of a field on an object
     *
     * @param o         The object that contains the field
     * @param fieldName The name of the field
     * @return The value of the field
     */
    public static Object getField(Object o, String fieldName) {

        try {
            final Field field = o.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(o);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get value of field " + fieldName, e);
        }
    }

    /**
     * Reset config.
     */
    public static void resetConfig() {

        try {
            final Field field = Config.class.getDeclaredField("config");
            field.setAccessible(true);
            field.set(Config.class, null);
            Config.getConfig();
        } catch (Exception e) {
            throw new RuntimeException("Cannot reset config", e);
        }

    }

    /**
     * Sets a Config property value
     * <p>
     * The Config instance is initialized once on startup which means that
     * its properties don't change throughout its lifetime.
     * This method allows a Config property to be changed temporarily for testing purposes
     *
     * @param key   The key of the property (eg Config.LOGGERLEVEL)
     * @param value The new value
     * @return The previous value
     */
    public static String setConfigProperty(String key, String value) throws Exception {

        String oldVal = null;

        try {
            Config config = Config.getConfig();

            final Field sdkPropertiesInstance = config.getClass().getDeclaredField("sdkProperties");
            sdkPropertiesInstance.setAccessible(true);

            final Properties sdkProperties = (Properties) sdkPropertiesInstance.get(config);
            oldVal = sdkProperties.getProperty(key);
            sdkProperties.put(key, value);

        } catch (Exception e) {
            throw new RuntimeException("Failed to set Config property " + key, e);
        }

        return oldVal;
    }

    public static void blockWalker(BlockEvent blockEvent) {
        try {

            BlockInfo returnedBlock = blockEvent;
            final long blockNumber = returnedBlock.getBlockNumber();
            final boolean isFilterBlock = returnedBlock.isFiltered();

            if (!returnedBlock.isFiltered()) {
                out("current block number %d has data hash: %s", blockNumber, Hex.encodeHexString(returnedBlock.getDataHash()));
                out("current block number %d has previous hash id: %s", blockNumber, Hex.encodeHexString(returnedBlock.getPreviousHash()));
//            out("current block number %d has calculated block hash is %s", blockNumber, Hex.encodeHexString(SDKUtils.calculateBlockHash(client,
//                    blockNumber, returnedBlock.getPreviousHash(), returnedBlock.getDataHash())));
            }

            final int envelopeCount = returnedBlock.getEnvelopeCount();
            //     assertEquals(1, envelopeCount);
            out("current block number %d has %d envelope count:", blockNumber, returnedBlock.getEnvelopeCount());
            int i = 0;
            for (BlockInfo.EnvelopeInfo envelopeInfo : returnedBlock.getEnvelopeInfos()) {
                ++i;

                out("  Transaction number %d has transaction id: %s", i, envelopeInfo.getTransactionID());
                final String channelId = envelopeInfo.getChannelId();
                //           assertTrue("foo".equals(channelId) || "bar".equals(channelId));

                out("  Transaction number %d has channel id: %s", i, channelId);
                out("  Transaction number %d has type id: %s", i, "" + envelopeInfo.getType());
                if (!isFilterBlock) {
                    out("  Transaction number %d has epoch: %d", i, envelopeInfo.getEpoch());
                    out("  Transaction number %d has transaction timestamp: %tB %<te,  %<tY  %<tT %<Tp", i, envelopeInfo.getTimestamp());
                }

                if (envelopeInfo.getType() == TRANSACTION_ENVELOPE) {
                    BlockInfo.TransactionEnvelopeInfo transactionEnvelopeInfo = (BlockInfo.TransactionEnvelopeInfo) envelopeInfo;

                    out("  Transaction number %d has %d actions", i, transactionEnvelopeInfo.getTransactionActionInfoCount());
                    //              assertEquals(1, transactionEnvelopeInfo.getTransactionActionInfoCount()); // for now there is only 1 action per transaction.
                    out("  Transaction number %d isValid %b", i, transactionEnvelopeInfo.isValid());
                    //             assertEquals(transactionEnvelopeInfo.isValid(), true);
                    out("  Transaction number %d validation code %d", i, transactionEnvelopeInfo.getValidationCode());
                    //              assertEquals(0, transactionEnvelopeInfo.getValidationCode());

                    int j = 0;
                    for (BlockInfo.TransactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : transactionEnvelopeInfo.getTransactionActionInfos()) {
                        ++j;
                        if (!isFilterBlock) {
                            out("   Transaction action %d has response status %d", j, transactionActionInfo.getResponseStatus());
                            //                 assertEquals(200, transactionActionInfo.getResponseStatus());
                            out("   Transaction action %d has response message bytes as string: %s", j,
                                    new String(transactionActionInfo.getResponseMessageBytes(), "UTF-8"));
                            out("   Transaction action %d has %d endorsements", j, transactionActionInfo.getEndorsementsCount());
                            //                 assertEquals(2, transactionActionInfo.getEndorsementsCount());

                            for (int n = 0; n < transactionActionInfo.getEndorsementsCount(); ++n) {
                                BlockInfo.EndorserInfo endorserInfo = transactionActionInfo.getEndorsementInfo(n);
                                out("Endorser %d signature: %s", n, Hex.encodeHexString(endorserInfo.getSignature()));
                                out("Endorser %d endorser: %s", n, new String(endorserInfo.getEndorser(), "UTF-8"));
                            }
                            out("   Transaction action %d has %d chaincode input arguments", j, transactionActionInfo.getChaincodeInputArgsCount());
                            for (int z = 0; z < transactionActionInfo.getChaincodeInputArgsCount(); ++z) {
                                out("     Transaction action %d has chaincode input argument %d is: %s", j, z,
                                        new String(transactionActionInfo.getChaincodeInputArgs(z), "UTF-8"));
                            }

                            out("   Transaction action %d proposal response status: %d", j,
                                    transactionActionInfo.getProposalResponseStatus());
                            out("   Transaction action %d proposal response payload: %s", j,
                                    new String(transactionActionInfo.getProposalResponsePayload()));
                        }

                        // Check to see if we have our expected event.

                        ChaincodeEvent chaincodeEvent = transactionActionInfo.getEvent();
                        if (null != chaincodeEvent) {
                            out("Chaincode event txid:%s, getChaincodeId:%s", chaincodeEvent.getTxId(), chaincodeEvent.getChaincodeId());
                        }

                        //                               assertNotNull(chaincodeEvent);

//                                assertTrue(Arrays.equals(EXPECTED_EVENT_DATA, chaincodeEvent.getPayload()));
//                                assertEquals(testTxID, chaincodeEvent.getTxId());
//                                assertEquals(CHAIN_CODE_NAME, chaincodeEvent.getChaincodeId());
//                                assertEquals(EXPECTED_EVENT_NAME, chaincodeEvent.getEventName());

                        TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
                        if (null != rwsetInfo) {
                            out("   Transaction action %d has %d name space read write sets", j, rwsetInfo.getNsRwsetCount());

                            for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {
                                final String namespace = nsRwsetInfo.getNamespace();
                                KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();

                                int rs = -1;
//                                    for (KvRwset.KVRead readList : rws.getReadsList()) {
//                                        rs++;
//
//                                        out("     Namespace %s read set %d key %s  version [%d:%d]", namespace, rs, readList.getKey(),
//                                                readList.getVersion().getBlockNum(), readList.getVersion().getTxNum());
//
//                                        if ("bar".equals(channelId) && blockNumber == 2) {
//                                            if ("example_cc_go".equals(namespace)) {
//                                                if (rs == 0) {
////                                                    assertEquals("a", readList.getKey());
////                                                    assertEquals(1, readList.getVersion().getBlockNum());
////                                                    assertEquals(0, readList.getVersion().getTxNum());
//                                                } else if (rs == 1) {
////                                                    assertEquals("b", readList.getKey());
////                                                    assertEquals(1, readList.getVersion().getBlockNum());
////                                                    assertEquals(0, readList.getVersion().getTxNum());
//                                                } else {
//                                                    //  fail(format("unexpected readset %d", rs));
//                                                }
//
//                                                //                                               TX_EXPECTED.remove("readset1");
//                                            }
//                                        }
//                                    }

                                rs = -1;
                                for (KvRwset.KVWrite writeList : rws.getWritesList()) {
                                    rs++;
                                    String valAsString = new String(writeList.getValue().toByteArray(), "UTF-8");

                                    out("     Namespace %s write set %d key %s has value '%s' ", namespace, rs,
                                            writeList.getKey(),
                                            valAsString);

//                                        if ("bar".equals(channelId) && blockNumber == 2) {
//                                            if (rs == 0) {
//                                                assertEquals("a", writeList.getKey());
//                                                assertEquals("400", valAsString);
//                                            } else if (rs == 1) {
//                                                assertEquals("b", writeList.getKey());
//                                                assertEquals("400", valAsString);
//                                            } else {
//                                             //   fail(format("unexpected writeset %d", rs));
//                                            }
//
//                                      //      TX_EXPECTED.remove("writeset1");
//                                        }
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(String.format(format, args));
        System.err.flush();
        System.out.flush();

    }

}
