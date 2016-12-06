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

package org.hyperledger.fabric.sdk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.exception.GetTCertBatchException;
import org.hyperledger.fabric.sdk.stats.Rate;
import org.hyperledger.fabric.sdk.stats.ResponseTime;

import java.util.List;
import java.util.Stack;

// A class to get TCerts.
// There is one class per set of attributes requested by each member.
public class TCertGetter {

    private static final Log logger = LogFactory.getLog(TCertGetter.class);

    private Chain chain;
    private Member member;
    private List<String> attrs;
    private String key;
    private MemberServices memberServices;
    private Stack<TCert> tcerts;
//TODO implement stats
    private Rate arrivalRate = new Rate();
    private ResponseTime getTCertResponseTime = new ResponseTime();
//    private getTCertWaiters:GetTCertCallback[] = [];
    private boolean gettingTCerts = false;

    /**
    * Constructor for a member.
    * @param cfg {string | RegistrationRequest} The member name or registration request.
    * @returns {Member} A member who is neither registered nor enrolled.
    */
    public TCertGetter(Member member, List<String> attrs, String key) {
        this.member = member;
        this.attrs = attrs;
        this.key = key;
        this.chain = member.getChain();
        this.memberServices = member.getMemberServices();
        this.tcerts = new Stack<>();
    }

    /**
    * Get the chain.
    * @returns {Chain} The chain.
    */
    public Chain getChain() {
        return this.chain;
    };

    public void getUserCert() {
        this.getNextTCert();
    }

    /**
    * Get the next available transaction certificate.
    */
    public TCert getNextTCert() {

    	arrivalRate.tick();

        if (tcerts.size() == 0 && shouldGetTCerts()) {
            getTCerts();
        }

        if (tcerts.size() > 0) {
            return tcerts.pop();
        } else {
            return null;
        }
    }

    // Determine if we should issue a request to get more tcerts now.
    private boolean shouldGetTCerts() {
        // Do nothing if we are already getting more tcerts
        if (gettingTCerts) {
            logger.debug("shouldGetTCerts: no, already getting tcerts");
            return false;
        }
        // If there are none, then definitely get more
        if (tcerts.size() == 0) {
            logger.debug("shouldGetTCerts: yes, we have no tcerts");
            return true;
        }
        // If we aren't in prefetch mode, return false;
        if (!this.chain.isPreFetchMode()) {
            logger.debug("shouldGetTCerts: no, prefetch disabled");
            return false;
        }
        // Otherwise, see if we should prefetch based on the arrival rate
        // (i.e. the rate at which tcerts are requested) and the response
        // time.
        // "arrivalRate" is in req/ms and "responseTime" in ms,
        // so "tcertCountThreshold" is number of tcerts at which we should
        // request the next batch of tcerts so we don't have to wait on the
        // transaction path.  Note that we add 1 sec to the average response
        // time to add a little buffer time so we don't have to wait.
        double arrivalRate = this.arrivalRate.getValue();
        double responseTime = this.getTCertResponseTime.getValue() + 1000;
        int tcertThreshold = (int) (arrivalRate * responseTime);
        int tcertCount = this.tcerts.size();
        boolean result = tcertCount <= tcertThreshold;
        logger.debug(String.format("shouldGetTCerts: %d, threshold=%d, count=%d, rate=%d, responseTime=%d",
                result, tcertThreshold, tcertCount, arrivalRate, responseTime));
        return result;
    }

    // Call member services to get more tcerts
    private void getTCerts() {
        GetTCertBatchRequest req = new GetTCertBatchRequest(this.member.getName(), this.member.getEnrollment(),
                this.member.getTCertBatchSize(), attrs);
        getTCertResponseTime.start();
        try {
            List<TCert> tcerts = this.memberServices.getTCertBatch(req);
            getTCertResponseTime.stop();
            // Add to member's tcert list
            for (TCert tcert : tcerts) {
                this.tcerts.push(tcert);
            }
        } catch (GetTCertBatchException e) {
            getTCertResponseTime.cancel();
        }
    }
} // end TCertGetter
