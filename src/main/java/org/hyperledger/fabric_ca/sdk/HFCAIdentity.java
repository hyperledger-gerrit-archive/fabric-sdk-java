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

package org.hyperledger.fabric_ca.sdk;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Fabric Certificate authority information
 * Contains information for the Fabric certificate authority
 */
public class HFCAIdentity {

    // The enrollment ID of the user
    private String enrollmentID;
    // Type of identity
    private String type = "user";
    // Optional secret
    private String secret;
    // Maximum number of enrollments with the secret
    private Integer maxEnrollments = null;
    // Affiliation for a user
    private String affiliation;
    // Array of attribute names and values
    private Collection<Attribute> attrs = new ArrayList<Attribute>();

    private String caName;


    public HFCAIdentity(String id, String type, Integer maxEnrollments, String affiliation, Collection<Attribute> attrs, String caName) {
        this.enrollmentID = id;
        this.type = type;
        this.maxEnrollments = maxEnrollments;
        this.affiliation = affiliation;
        this.attrs = attrs;
        this.caName = caName;
    }

    public HFCAIdentity(String id, String type, String secret, Integer maxEnrollments, String affiliation, Collection<Attribute> attrs, String caName) {
        this(id, type, maxEnrollments, affiliation, attrs, caName);
        this.secret = secret;
    }

    /**
     * The name of the identity
     *
     * @return The identity name.
     */

    public String getId() {
        return enrollmentID;
    }

    /**
     * The type of the identity
     *
     * @return The identity type.
     */

    public String getType() {
        return type;
    }

    /**
     * The secret of the identity
     *
     * @return The identity secret.
     */

    public String getSecret() {
        return secret;
    }

    /**
     * The max enrollment value of the identity
     *
     * @return The identity max enrollment.
     */

    public Integer getMaxEnrollments() {
        return maxEnrollments;
    }

    /**
     * The affiliation of the identity
     *
     * @return The identity affiliation.
     */

    public String getAffiliation() {
        return affiliation;
    }

    /**
     * The attributes of the identity
     *
     * @return The identity attributes.
     */

    public Collection<Attribute> getAttributes() {
        return attrs;
    }

    /**
     * The CAName for the Fabric Certificate Authority.
     *
     * @return The CA Name.
     */

    public String getCAName() {
        return caName;
    }

}