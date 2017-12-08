/*
 *  Copyright 2016, 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric_ca.sdk;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.hyperledger.fabric.sdk.security.CryptoPrimitives;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.GenerateCRLException;
import org.hyperledger.fabric_ca.sdk.exception.IdentityException;
import org.hyperledger.fabric_ca.sdk.exception.InfoException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric_ca.sdk.exception.RegistrationException;
import org.hyperledger.fabric_ca.sdk.exception.RevocationException;
import org.hyperledger.fabric_ca.sdk.helper.Config;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
/**
 * HFCAClient Hyperledger Fabric Certificate Authority Client.
 */

public class HFCAClient {
    private static final Log logger = LogFactory.getLog(HFCAClient.class);
    private static final String HFCA_CONTEXT_ROOT = "/api/v1/";
    private static final String HFCA_ENROLL = HFCA_CONTEXT_ROOT + "enroll";
    private static final String HFCA_REGISTER = HFCA_CONTEXT_ROOT + "register";
    private static final String HFCA_REENROLL = HFCA_CONTEXT_ROOT + "reenroll";
    private static final String HFCA_REVOKE = HFCA_CONTEXT_ROOT + "revoke";
    private static final String HFCA_INFO = HFCA_CONTEXT_ROOT + "cainfo";
    private static final String HFCA_GENCRL = HFCA_CONTEXT_ROOT + "gencrl";
    private static final String HFCA_IDENTITY = HFCA_CONTEXT_ROOT + "identities";

    static final String FABRIC_CA_REQPROP = "caname";

    private final String url;
    private final boolean isSSL;
    private final Properties properties;

    /**
     * The Certificate Authority name.
     *
     * @return May return null or empty string for default certificate authority.
     */
    public String getCAName() {
        return caName;
    }

    private final String caName;

    private CryptoSuite cryptoSuite;

    /**
     * HFCAClient constructor
     *
     * @param url        Http URL for the Fabric's certificate authority services endpoint
     * @param properties PEM used for SSL .. not implemented.
     *                   <p>
     *                   Supported properties
     *                   <ul>
     *                   <li>pemFile - File location for x509 pem certificate for SSL.</li>
     *                   <li>allowAllHostNames - boolen(true/false) override certificates CN Host matching -- for development only.</li>
     *                   </ul>
     * @throws MalformedURLException
     */
    HFCAClient(String caName, String url, Properties properties) throws MalformedURLException {
        logger.debug(format("new HFCAClient %s", url));
        this.url = url;

        this.caName = caName; //name may be null

        URL purl = new URL(url);
        final String proto = purl.getProtocol();
        if (!"http".equals(proto) && !"https".equals(proto)) {
            throw new IllegalArgumentException("HFCAClient only supports http or https not " + proto);
        }
        final String host = purl.getHost();

        if (Utils.isNullOrEmpty(host)) {
            throw new IllegalArgumentException("HFCAClient url needs host");
        }

        final String path = purl.getPath();

        if (!Utils.isNullOrEmpty(path)) {

            throw new IllegalArgumentException("HFCAClient url does not support path portion in url remove path: '" + path + "'.");
        }

        final String query = purl.getQuery();

        if (!Utils.isNullOrEmpty(query)) {

            throw new IllegalArgumentException("HFCAClient url does not support query portion in url remove query: '" + query + "'.");
        }

        isSSL = "https".equals(proto);

        if (properties != null) {
            this.properties = (Properties) properties.clone(); //keep our own copy.
        } else {
            this.properties = null;
        }

    }

    public static HFCAClient createNewInstance(String url, Properties properties) throws MalformedURLException {

        return new HFCAClient(null, url, properties);

    }

    public static HFCAClient createNewInstance(String name, String url, Properties properties) throws MalformedURLException, InvalidArgumentException {

        if (name == null || name.isEmpty()) {

            throw new InvalidArgumentException("name must not be null or an empty string.");
        }

        return new HFCAClient(name, url, properties);

    }

    public void setCryptoSuite(CryptoSuite cryptoSuite) {
        this.cryptoSuite = cryptoSuite;
    }

    public CryptoSuite getCryptoSuite() {
        return cryptoSuite;
    }

    /**
     * Register a user.
     *
     * @param request   Registration request with the following fields: name, role.
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the enrollment secret.
     * @throws RegistrationException    if registration fails.
     * @throws InvalidArgumentException
     */

    public String register(RegistrationRequest request, User registrar) throws RegistrationException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (Utils.isNullOrEmpty(request.getEnrollmentID())) {
            throw new InvalidArgumentException("EntrollmentID cannot be null or empty");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }
        logger.debug(format("register  url: %s, registrar: %s", url, registrar.getName()));

        setUpSSL();

        try {
            String body = request.toJson();
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), body);
            JsonObject resp = httpPost(url + HFCA_REGISTER, body, authHdr);
            String secret = resp.getString("secret");
            if (secret == null) {
                throw new Exception("secret was not found in response");
            }
            logger.debug(format("register  url: %s, registrar: %s done.", url, registrar));
            return secret;
        } catch (Exception e) {

            RegistrationException registrationException = new RegistrationException(format("Error while registering the user %s url: %s  %s ", registrar, url, e.getMessage()), e);
            logger.error(registrar);
            throw registrationException;

        }

    }

    /**
     * Enroll the user with member service
     *
     * @param user   Identity name to enroll
     * @param secret Secret returned via registration
     * @return enrollment
     * @throws EnrollmentException
     * @throws InvalidArgumentException
     */
    public Enrollment enroll(String user, String secret) throws EnrollmentException, InvalidArgumentException {
        return enroll(user, secret, new EnrollmentRequest());
    }

    /**
     * Enroll the user with member service
     *
     * @param user   Identity name to enroll
     * @param secret Secret returned via registration
     * @param req    Enrollment request with the following fields: hosts, profile, csr, label, keypair
     * @return enrollment
     * @throws EnrollmentException
     * @throws InvalidArgumentException
     */

    public Enrollment enroll(String user, String secret, EnrollmentRequest req) throws EnrollmentException, InvalidArgumentException {

        logger.debug(format("url:%s enroll user: %s", url, user));

        if (Utils.isNullOrEmpty(user)) {
            throw new InvalidArgumentException("enrollment user is not set");
        }
        if (Utils.isNullOrEmpty(secret)) {
            throw new InvalidArgumentException("enrollment secret is not set");
        }

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        setUpSSL();

        try {
            String pem = req.getCsr();
            KeyPair keypair = req.getKeyPair();
            if (null != pem && keypair == null) {
                throw new InvalidArgumentException("If certificate signing request is supplied the key pair needs to be supplied too.");
            }
            if (keypair == null) {
                logger.debug("[HFCAClient.enroll] Generating keys...");

                // generate ECDSA keys: signing and encryption keys
                keypair = cryptoSuite.keyGen();

                logger.debug("[HFCAClient.enroll] Generating keys...done!");
            }

            if (pem == null) {
                String csr = cryptoSuite.generateCertificationRequest(user, keypair);
                req.setCSR(csr);
            }

            if (caName != null && !caName.isEmpty()) {
                req.setCAName(caName);
            }
            String body = req.toJson();

            String responseBody = httpPost(url + HFCA_ENROLL, body,
                    new UsernamePasswordCredentials(user, secret));

            logger.debug("response:" + responseBody);

            JsonReader reader = Json.createReader(new StringReader(responseBody));
            JsonObject jsonst = (JsonObject) reader.read();

            boolean success = jsonst.getBoolean("success");
            logger.debug(format("[HFCAClient] enroll success:[%s]", success));

            if (!success) {
                throw new EnrollmentException(format("FabricCA failed enrollment for user %s response success is false.", user));
            }

            JsonObject result = jsonst.getJsonObject("result");
            if (result == null) {
                throw new EnrollmentException(format("FabricCA failed enrollment for user %s - response did not contain a result", user));
            }

            Base64.Decoder b64dec = Base64.getDecoder();

            String signedPem = new String(b64dec.decode(result.getString("Cert").getBytes(UTF_8)));
            logger.debug(format("[HFCAClient] enroll returned pem:[%s]", signedPem));

            JsonArray messages = jsonst.getJsonArray("messages");
            if (messages != null && !messages.isEmpty()) {
                JsonObject jo = messages.getJsonObject(0);
                String message = format("Enroll request response message [code %d]: %s", jo.getInt("code"), jo.getString("message"));
                logger.info(message);
            }
            logger.debug("Enrollment done.");

            return new HFCAEnrollment(keypair, signedPem);

        } catch (EnrollmentException ee) {
            logger.error(format("url:%s, user:%s  error:%s", url, user, ee.getMessage()), ee);
            throw ee;
        } catch (Exception e) {
            EnrollmentException ee = new EnrollmentException(format("Url:%s, Failed to enroll user %s ", url, user), e);
            logger.error(e.getMessage(), e);
            throw ee;
        }

    }

    /**
     * Return information on the Fabric Certificate Authority.
     * No credentials are needed for this API.
     *
     * @return {@link HFCAInfo}
     * @throws InfoException
     * @throws InvalidArgumentException
     */

    public HFCAInfo info() throws InfoException, InvalidArgumentException {

        logger.debug(format("info url:%s", url));
        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        setUpSSL();

        try {

            JsonObjectBuilder factory = Json.createObjectBuilder();

            if (caName != null) {
                factory.add(HFCAClient.FABRIC_CA_REQPROP, caName);
            }
            JsonObject body = factory.build();

            String responseBody = httpPost(url + HFCA_INFO, body.toString(),
                    (UsernamePasswordCredentials) null);

            logger.debug("response:" + responseBody);

            JsonReader reader = Json.createReader(new StringReader(responseBody));
            JsonObject jsonst = (JsonObject) reader.read();

            boolean success = jsonst.getBoolean("success");
            logger.debug(format("[HFCAClient] enroll success:[%s]", success));

            if (!success) {
                throw new EnrollmentException(format("FabricCA failed info %s", url));
            }

            JsonObject result = jsonst.getJsonObject("result");
            if (result == null) {
                throw new InfoException(format("FabricCA info error  - response did not contain a result url %s", url));
            }

            String caName = result.getString("CAName");
            String caChain = result.getString("CAChain");

            return new HFCAInfo(caName, caChain);

        } catch (Exception e) {
            InfoException ee = new InfoException(format("Url:%s, Failed to get info", url), e);
            logger.error(e.getMessage(), e);
            throw ee;
        }

    }

    /**
     * Re-Enroll the user with member service
     *
     * @param user User to be re-enrolled
     * @return enrollment
     * @throws EnrollmentException
     * @throws InvalidArgumentException
     */
    public Enrollment reenroll(User user) throws EnrollmentException, InvalidArgumentException {
        return reenroll(user, new EnrollmentRequest());
    }

    /**
     * Re-Enroll the user with member service
     *
     * @param user User to be re-enrolled
     * @param req  Enrollment request with the following fields: hosts, profile, csr, label
     * @return enrollment
     * @throws EnrollmentException
     * @throws InvalidArgumentException
     */

    public Enrollment reenroll(User user, EnrollmentRequest req) throws EnrollmentException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (user == null) {
            throw new InvalidArgumentException("reenrollment user is missing");
        }
        if (user.getEnrollment() == null) {
            throw new InvalidArgumentException("reenrollment user is not a valid user object");
        }

        logger.debug(format("re-enroll user: %s, url: %s", user.getName(), url));

        try {
            setUpSSL();

            PublicKey publicKey = cryptoSuite.bytesToCertificate(user.getEnrollment().getCert()
                    .getBytes(StandardCharsets.UTF_8)).getPublicKey();

            KeyPair keypair = new KeyPair(publicKey, user.getEnrollment().getKey());

            // generate CSR

            String pem = cryptoSuite.generateCertificationRequest(user.getName(), keypair);

            // build request body
            req.setCSR(pem);
            if (caName != null && !caName.isEmpty()) {
                req.setCAName(caName);
            }
            String body = req.toJson();

            // build authentication header
            String authHdr = getHTTPAuthCertificate(user.getEnrollment(), body);
            JsonObject result = httpPost(url + HFCA_REENROLL, body, authHdr);

            // get new cert from response
            Base64.Decoder b64dec = Base64.getDecoder();
            String signedPem = new String(b64dec.decode(result.getString("Cert").getBytes(UTF_8)));
            logger.debug(format("[HFCAClient] re-enroll returned pem:[%s]", signedPem));

            logger.debug(format("reenroll user %s done.", user.getName()));
            return new HFCAEnrollment(keypair, signedPem);

        } catch (EnrollmentException ee) {
            logger.error(ee.getMessage(), ee);
            throw ee;
        } catch (Exception e) {
            EnrollmentException ee = new EnrollmentException(format("Failed to re-enroll user %s", user), e);
            logger.error(e.getMessage(), e);
            throw ee;
        }
    }

    /**
     * revoke one enrollment of user
     *
     * @param revoker    admin user who has revoker attribute configured in CA-server
     * @param enrollment the user enrollment to be revoked
     * @param reason     revoke reason, see RFC 5280
     * @throws RevocationException
     * @throws InvalidArgumentException
     */

    public void revoke(User revoker, Enrollment enrollment, String reason) throws RevocationException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (enrollment == null) {
            throw new InvalidArgumentException("revokee enrollment is not set");
        }
        if (revoker == null) {
            throw new InvalidArgumentException("revoker is not set");
        }

        logger.debug(format("revoke revoker: %s, reason: %s, url: %s", revoker.getName(), reason, url));

        try {
            setUpSSL();

            // get cert from to-be-revoked enrollment
            BufferedInputStream pem = new BufferedInputStream(new ByteArrayInputStream(enrollment.getCert().getBytes()));
            CertificateFactory certFactory = CertificateFactory.getInstance(Config.getConfig().getCertificateFormat());
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(pem);

            // get its serial number
            String serial = DatatypeConverter.printHexBinary(certificate.getSerialNumber().toByteArray());

            // get its aki
            // 2.5.29.35 : AuthorityKeyIdentifier
            byte[] extensionValue = certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
            ASN1OctetString akiOc = ASN1OctetString.getInstance(extensionValue);
            String aki = DatatypeConverter.printHexBinary(AuthorityKeyIdentifier.getInstance(akiOc.getOctets()).getKeyIdentifier());

            // build request body
            RevocationRequest req = new RevocationRequest(caName, null, serial, aki, reason);
            String body = req.toJson();

            String authHdr = getHTTPAuthCertificate(revoker.getEnrollment(), body);

            // send revoke request
            httpPost(url + HFCA_REVOKE, body, authHdr);
            logger.debug("revoke done");
        } catch (CertificateException e) {
            logger.error("Cannot validate certificate. Error is: " + e.getMessage());
            throw new RevocationException("Error while revoking cert. " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RevocationException("Error while revoking the user. " + e.getMessage(), e);

        }
    }

    /**
     * revoke one enrollment of user
     *
     * @param revoker    admin user who has revoker attribute configured in CA-server
     * @param enrollment the user enrollment to be revoked
     * @param reason     revoke reason, see RFC 5280
     * @param genCRL     generate CRL list
     * @throws RevocationException
     * @throws InvalidArgumentException
     */

    public String revoke(User revoker, Enrollment enrollment, String reason, Boolean genCRL) throws RevocationException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (enrollment == null) {
            throw new InvalidArgumentException("revokee enrollment is not set");
        }
        if (revoker == null) {
            throw new InvalidArgumentException("revoker is not set");
        }

        logger.debug(format("revoke revoker: %s, reason: %s, url: %s", revoker.getName(), reason, url));

        try {
            setUpSSL();

            // get cert from to-be-revoked enrollment
            BufferedInputStream pem = new BufferedInputStream(new ByteArrayInputStream(enrollment.getCert().getBytes()));
            CertificateFactory certFactory = CertificateFactory.getInstance(Config.getConfig().getCertificateFormat());
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(pem);

            // get its serial number
            String serial = DatatypeConverter.printHexBinary(certificate.getSerialNumber().toByteArray());

            // get its aki
            // 2.5.29.35 : AuthorityKeyIdentifier
            byte[] extensionValue = certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
            ASN1OctetString akiOc = ASN1OctetString.getInstance(extensionValue);
            String aki = DatatypeConverter.printHexBinary(AuthorityKeyIdentifier.getInstance(akiOc.getOctets()).getKeyIdentifier());

            // build request body
            RevocationRequest req = new RevocationRequest(caName, null, serial, aki, reason, genCRL);
            String body = req.toJson();

            String authHdr = getHTTPAuthCertificate(revoker.getEnrollment(), body);

            // send revoke request
            JsonObject resp = httpPost(url + HFCA_REVOKE, body, authHdr);
            logger.debug("revoke done");

            if (genCRL) {
                if (resp.isEmpty()) {
                    throw new RevocationException("Failed to return CRL, revoke response is empty");
                }
                if (resp.isNull("CRL")) {
                    throw new RevocationException("Failed to return CRL");
                }
                return resp.getString("CRL");
            }
            return "";
        } catch (CertificateException e) {
            logger.error("Cannot validate certificate. Error is: " + e.getMessage());
            throw new RevocationException("Error while revoking cert. " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RevocationException("Error while revoking the user. " + e.getMessage(), e);

        }
    }

    /**
     * revoke one user (including his all enrollments)
     *
     * @param revoker admin user who has revoker attribute configured in CA-server
     * @param revokee user who is to be revoked
     * @param reason  revoke reason, see RFC 5280
     * @throws RevocationException
     * @throws InvalidArgumentException
     */

    public void revoke(User revoker, String revokee, String reason) throws RevocationException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        logger.debug(format("revoke revoker: %s, revokee: %s, reason: %s", revoker, revokee, reason));

        if (Utils.isNullOrEmpty(revokee)) {
            throw new InvalidArgumentException("revokee user is not set");
        }
        if (revoker == null) {
            throw new InvalidArgumentException("revoker is not set");
        }

        try {
            setUpSSL();

            // build request body
            RevocationRequest req = new RevocationRequest(caName, revokee, null, null, reason);
            String body = req.toJson();

            // build auth header
            String authHdr = getHTTPAuthCertificate(revoker.getEnrollment(), body);

            // send revoke request
            httpPost(url + HFCA_REVOKE, body, authHdr);
            logger.debug(format("revoke revokee: %s done.", revokee));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RevocationException("Error while revoking the user. " + e.getMessage(), e);
        }

    }

    /**
     * revoke one user (including his all enrollments)
     *
     * @param revoker admin user who has revoker attribute configured in CA-server
     * @param revokee user who is to be revoked
     * @param reason  revoke reason, see RFC 5280
     * @param genCRL  generate CRL
     * @throws RevocationException
     * @throws InvalidArgumentException
     */

    public String revoke(User revoker, String revokee, String reason, Boolean genCRL) throws RevocationException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        logger.debug(format("revoke revoker: %s, revokee: %s, reason: %s", revoker, revokee, reason));

        if (Utils.isNullOrEmpty(revokee)) {
            throw new InvalidArgumentException("revokee user is not set");
        }
        if (revoker == null) {
            throw new InvalidArgumentException("revoker is not set");
        }

        try {
            setUpSSL();

            // build request body
            RevocationRequest req = new RevocationRequest(caName, revokee, null, null, reason, genCRL);
            String body = req.toJson();

            // build auth header
            String authHdr = getHTTPAuthCertificate(revoker.getEnrollment(), body);

            // send revoke request
            JsonObject resp = httpPost(url + HFCA_REVOKE, body, authHdr);

            logger.debug(format("revoke revokee: %s done.", revokee));

            if (genCRL) {
                if (resp.isEmpty()) {
                    throw new RevocationException("Failed to return CRL, revoke response is empty");
                }
                if (resp.isNull("CRL")) {
                    throw new RevocationException("Failed to return CRL");
                }
                return resp.getString("CRL");
            }
            return "";
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RevocationException("Error while revoking the user. " + e.getMessage(), e);
        }

    }

    /**
     * Generate certificate revocation list.
     *
     * @param registrar     admin user configured in CA-server
     * @param revokedBefore Restrict certificates returned to revoked before this date if not null.
     * @param revokedAfter  Restrict certificates returned to revoked after this date if not null.
     * @param expireBefore  Restrict certificates returned to expired before this date if not null.
     * @param expireAfter   Restrict certificates returned to expired after this date if not null.
     * @throws InvalidArgumentException
     */

    public String generateCRL(User registrar, Date revokedBefore, Date revokedAfter, Date expireBefore, Date expireAfter)
            throws InvalidArgumentException, GenerateCRLException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("registrar is not set");
        }

        try {
            setUpSSL();

            //---------------------------------------
            JsonObjectBuilder factory = Json.createObjectBuilder();
            if (revokedBefore != null) {
                factory.add("revokedBefore", toJson(revokedBefore));
            }
            if (revokedAfter != null) {
                factory.add("revokedAfter", toJson(revokedAfter));
            }
            if (expireBefore != null) {
                factory.add("expireBefore", toJson(expireBefore));
            }
            if (expireAfter != null) {
                factory.add("expireAfter", toJson(expireAfter));
            }
            if (caName != null) {
                factory.add(HFCAClient.FABRIC_CA_REQPROP, caName);
            }

            JsonObject jsonObject = factory.build();

            StringWriter stringWriter = new StringWriter();
            JsonWriter jsonWriter = Json.createWriter(new PrintWriter(stringWriter));
            jsonWriter.writeObject(jsonObject);
            jsonWriter.close();
            String body = stringWriter.toString();

            //---------------------------------------

            // build auth header
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), body);

            // send revoke request
            JsonObject ret = httpPost(url + HFCA_GENCRL, body, authHdr);

            return ret.getString("CRL");

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new GenerateCRLException(e.getMessage(), e);
        }
    }

    /**
     * gets all identities that the registrar is allowed to see
     *
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was requested
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public Collection<HFCAIdentity> getAllIdentities(User registrar) throws IdentityException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            setUpSSL();

            String getAllURL = getURL(url + HFCA_IDENTITY);
            String caname = "";
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = httpGet(getAllURL, authHdr);

            if (result.containsKey("caname")) {
                caname = result.getString("caname");
            }
            Collection<HFCAIdentity> allIdentities = new ArrayList<HFCAIdentity>();

            JsonArray identities = result.getJsonArray("Identities");
            if (identities != null && !identities.isEmpty()) {
                for (int i = 0; i < identities.size(); i++) {
                    JsonObject identity = identities.getJsonObject(i);

                    String id = identity.getString("id");
                    String type = identity.getString("type");
                    Integer maxenrollments = identity.getInt("max_enrollments");
                    String affiliation = identity.getString("affiliation");

                    JsonArray attributes = identity.getJsonArray("attrs");

                    Collection<Attribute> attrs = new ArrayList<Attribute>();
                    if (attributes != null && !attributes.isEmpty()) {
                        for (int j = 0; j < attributes.size(); j++) {
                            JsonObject attribute = attributes.getJsonObject(j);
                            Attribute attr = new Attribute(attribute.getString("name"), attribute.getString("value"), attribute.getBoolean("ecert", false));
                            attrs.add(attr);
                        }
                     }

                    HFCAIdentity idObj = new HFCAIdentity(id, type, maxenrollments, affiliation, attrs, caname);
                    allIdentities.add(idObj);
                }
            }

            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return allIdentities;
        } catch (Exception e) {
            String msg = format("Error while getting all users url:%s %s ", url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * gets a specific identity
     *
     * @param getId   Name if the id being requested
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was requested
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity getIdentity(String getId, User registrar) throws IdentityException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            setUpSSL();

            String getIdURL = getURL(url + HFCA_IDENTITY + "/" + getId);
            String caname = "";
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = httpGet(getIdURL, authHdr);

            String id = result.getString("id");
            String type = result.getString("type");
            Integer maxenrollments = result.getInt("max_enrollments");
            String affiliation = result.getString("affiliation");
            if (result.containsKey("caname")) {
                caname = result.getString("caname");
            }
            JsonArray attributes = result.getJsonArray("attrs");

            Collection<Attribute> attrs = new ArrayList<Attribute>();
            if (attributes != null && !attributes.isEmpty()) {
                for (int i = 0; i < attributes.size(); i++) {
                    JsonObject attribute = attributes.getJsonObject(i);
                    Attribute attr = new Attribute(attribute.getString("name"), attribute.getString("value"), attribute.getBoolean("ecert", false));
                    attrs.add(attr);
                }
            }

            HFCAIdentity resp = new HFCAIdentity(id, type, maxenrollments, affiliation, attrs, caname);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while getting the user %s url: %s  %s ", getId, url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * add an identity
     *
     * @param request   Adding an identity request
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was added along with secret
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity addIdentity(IdentityRequest request, User registrar) throws IdentityException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (Utils.isNullOrEmpty(request.getEnrollmentID())) {
            throw new InvalidArgumentException("EntrollmentID cannot be null or empty");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            setUpSSL();

            String addURL = getURL(url + HFCA_IDENTITY);
            String body = request.toJson();
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), body);
            JsonObject result = httpPost(addURL, body, authHdr);

            HFCAIdentity resp = new HFCAIdentity(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while adding the user %s url: %s  %s ", request.getEnrollmentID(), url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * modify an identity
     *
     * @param request   Modification request for an identity
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was added along with secret
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity modifyIdentity(IdentityRequest request, User registrar) throws IdentityException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (Utils.isNullOrEmpty(request.getEnrollmentID())) {
            throw new InvalidArgumentException("ID to be modified can't be null or empty");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            setUpSSL();

            String putURL = getURL(url + HFCA_IDENTITY + "/" + request.getEnrollmentID());
            String body = request.toJson();
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), body);

            JsonObject result = httpPut(putURL, body, authHdr);

            HFCAIdentity resp = new HFCAIdentity(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while modifying the user %s by %s url: %s  %s ", request.getEnrollmentID(), registrar.getMspId(), url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    /**
     * delete an identity
     *
     * @param request   Modification request for an identity
     * @param registrar The identity of the registrar (i.e. who is performing the registration).
     * @return the identity that was added along with secret
     * @throws IdentityException    if adding an identity fails.
     * @throws InvalidArgumentException
     */

    public HFCAIdentity deleteIdentity(String id, User registrar) throws IdentityException, InvalidArgumentException {

        if (cryptoSuite == null) {
            throw new InvalidArgumentException("Crypto primitives not set.");
        }

        if (registrar == null) {
            throw new InvalidArgumentException("Registrar should be a valid member");
        }

        if (id == null) {
            throw new InvalidArgumentException("Id to be removed is required");
        }

        logger.debug(format("identity  url: %s, registrar: %s", url, registrar.getName()));

        try {
            setUpSSL();

            String deleteURL = getURL(url + HFCA_IDENTITY + "/" + id);
            String authHdr = getHTTPAuthCertificate(registrar.getEnrollment(), "");
            JsonObject result = httpDelete(deleteURL, authHdr);

            HFCAIdentity resp = new HFCAIdentity(result);
            logger.debug(format("identity  url: %s, registrar: %s done.", url, registrar));
            return resp;
        } catch (Exception e) {
            String msg = format("Error while deleting the user %s url: %s  %s ", id, url, e.getMessage());
            IdentityException identityException = new IdentityException(msg, e);
            logger.error(msg);
            throw identityException;
        }

    }

    private String toJson(Date date) {
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        SimpleDateFormat tformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        tformat.setTimeZone(utc);
        return tformat.format(date);
    }

    /**
     * Http Post Request.
     *
     * @param url         Target URL to POST to.
     * @param body        Body to be sent with the post.
     * @param credentials Credentials to use for basic auth.
     * @return Body of post returned.
     * @throws Exception
     */
    String httpPost(String url, String body, UsernamePasswordCredentials credentials) throws Exception {
        logger.debug(format("httpPost %s, body:%s", url, body));

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        CredentialsProvider provider = null;
        if (credentials != null) {
            provider = new BasicCredentialsProvider();

            provider.setCredentials(AuthScope.ANY, credentials);
            httpClientBuilder.setDefaultCredentialsProvider(provider);
        }

        if (registry != null) {

            httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager(registry));

        }

        HttpClient client = httpClientBuilder.build();

        HttpPost httpPost = new HttpPost(url);

        AuthCache authCache = new BasicAuthCache();

        HttpHost targetHost = new HttpHost(httpPost.getURI().getHost(), httpPost.getURI().getPort());

        if (credentials != null) {
            authCache.put(targetHost, new

                    BasicScheme());

        }

        final HttpClientContext context = HttpClientContext.create();

        if (null != provider) {
            context.setCredentialsProvider(provider);
        }

        if (credentials != null) {
            context.setAuthCache(authCache);
        }

        httpPost.setEntity(new StringEntity(body));
        if (credentials != null) {
            httpPost.addHeader(new

                    BasicScheme().

                    authenticate(credentials, httpPost, context));
        }

        HttpResponse response = client.execute(httpPost, context);
        int status = response.getStatusLine().getStatusCode();

        HttpEntity entity = response.getEntity();
        logger.trace(format("httpPost %s  sending...", url));
        String responseBody = entity != null ? EntityUtils.toString(entity) : null;
        logger.trace(format("httpPost %s  responseBody %s", url, responseBody));

        if (status >= 400) {

            Exception e = new Exception(format("POST request to %s  with request body: %s, " +
                    "failed with status code: %d. Response: %s", url, body, status, responseBody));
            logger.error(e.getMessage());
            throw e;
        }
        logger.debug(format("httpPost Status: %d returning: %s ", status, responseBody));

        return responseBody;
    }

    JsonObject httpPost(String url, String body, String authHTTPCert) throws Exception {

        HttpPost httpPost = new HttpPost(url);
        logger.debug(format("httpPost %s, body:%s, authHTTPCert: %s", url, body, authHTTPCert));

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (registry != null) {
            httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager(registry));
        }
        HttpClient client = httpClientBuilder.build();

        final HttpClientContext context = HttpClientContext.create();
        httpPost.setEntity(new StringEntity(body));
        httpPost.addHeader("Authorization", authHTTPCert);

        HttpResponse response = client.execute(httpPost, context);

        return getResult(response, body, "POST");
    }

    JsonObject httpGet(String url, String authHTTPCert) throws Exception {

        HttpGet httpGet = new HttpGet(url);
        logger.debug(format("httpGet %s, authHTTPCert: %s", url, authHTTPCert));

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (registry != null) {
            httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager(registry));
        }
        HttpClient client = httpClientBuilder.build();

        final HttpClientContext context = HttpClientContext.create();
        httpGet.addHeader("Authorization", authHTTPCert);

        HttpResponse response = client.execute(httpGet, context);

        return getResult(response, "", "GET");
    }

    JsonObject httpPut(String url, String body, String authHTTPCert) throws Exception {

        HttpPut httpPut = new HttpPut(url);
        logger.debug(format("httpPutt %s, body:%s, authHTTPCert: %s", url, body, authHTTPCert));

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (registry != null) {
            httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager(registry));
        }
        HttpClient client = httpClientBuilder.build();

        final HttpClientContext context = HttpClientContext.create();
        httpPut.setEntity(new StringEntity(body));
        httpPut.addHeader("Authorization", authHTTPCert);

        HttpResponse response = client.execute(httpPut, context);

        return getResult(response, body, "PUT");
    }

    JsonObject httpDelete(String url, String authHTTPCert) throws Exception {

        HttpDelete httpDelete = new HttpDelete(url);
        logger.debug(format("httpPut %s, authHTTPCert: %s", url, authHTTPCert));

        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (registry != null) {
            httpClientBuilder.setConnectionManager(new PoolingHttpClientConnectionManager(registry));
        }
        HttpClient client = httpClientBuilder.build();

        final HttpClientContext context = HttpClientContext.create();
        httpDelete.addHeader("Authorization", authHTTPCert);

        HttpResponse response = client.execute(httpDelete, context);

        return getResult(response, "", "DELETE");
    }

    JsonObject getResult(HttpResponse response, String body, String type) throws Exception {

        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        logger.trace(format("response status %d, HttpEntity %s ", status, "" + entity));
        String responseBody = entity != null ? EntityUtils.toString(entity) : null;
        logger.trace(format("responseBody: %s ", responseBody));

        if (status >= 400) {
            Exception e = new Exception(format("%s request to %s failed request body %s with status code: %d. Response: %s",
                    type, url, body, status, responseBody));
            logger.error(e.getMessage());
            throw e;
        }
        if (responseBody == null) {

            Exception e = new Exception(format("%s request to %s failed request body %s with null response body returned.", type, url, body));
            logger.error(e.getMessage());
            throw e;

        }
        logger.debug("Status: " + status);

        JsonReader reader = Json.createReader(new StringReader(responseBody));
        JsonObject jobj = (JsonObject) reader.read();
        boolean success = jobj.getBoolean("success");
        if (!success) {
            EnrollmentException e = new EnrollmentException(
                    format("%s request to %s failed request body %s Body of response did not contain success", type, url, body),
                    new Exception());
            logger.error(e.getMessage());
            throw e;
        }
        JsonObject result = jobj.getJsonObject("result");
        if (result == null) {
            EnrollmentException e = new EnrollmentException(format("%s request to %s failed request body %s " +
                    "Body of response did not contain result", type, url, body), new Exception());
            logger.error(e.getMessage());
            throw e;
        }
        JsonArray messages = jobj.getJsonArray("messages");
        if (messages != null && !messages.isEmpty()) {
            JsonObject jo = messages.getJsonObject(0);
            String message = format("%s request to %s failed request body %s response message [code %d]: %s",
                    type, url, body, jo.getInt("code"), jo.getString("message"));
            logger.info(message);
        }

        logger.debug(format("%s %s, body:%s result: %s", type, url, body, "" + result));
        return result;
    }

    private String getHTTPAuthCertificate(Enrollment enrollment, String body) throws Exception {
        Base64.Encoder b64 = Base64.getEncoder();
        String cert = b64.encodeToString(enrollment.getCert().getBytes(UTF_8));
        body = b64.encodeToString(body.getBytes(UTF_8));
        String signString = body + "." + cert;
        byte[] signature = cryptoSuite.sign(enrollment.getKey(), signString.getBytes(UTF_8));
        return cert + "." + b64.encodeToString(signature);
    }

    private Registry<ConnectionSocketFactory> registry = null;
    //Only use crypto primitives for reuse of its truststore on TLS
    CryptoPrimitives cryptoPrimitives = null;

    private void setUpSSL() throws InvalidArgumentException {

        if (cryptoPrimitives == null) {
            try {
                cryptoPrimitives = new CryptoPrimitives();
                cryptoPrimitives.init();
            } catch (Exception e) {
                throw new InvalidArgumentException(e);
            }
        }

        if (isSSL && null == registry) {
            if (properties.containsKey("pemBytes") && properties.containsKey("pemFile")) {

                throw new InvalidArgumentException("Properties can not have both \"pemBytes\" and \"pemFile\" specified. ");

            }
            try {

                if (properties.containsKey("pemBytes")) {
                    byte[] pemBytes = (byte[]) properties.get("pemBytes");

                    cryptoPrimitives.addCACertificateToTrustStore(pemBytes, pemBytes.toString());

                } else {
                    String pemFile = properties.getProperty("pemFile");
                    if (pemFile != null) {

                        cryptoPrimitives.addCACertificateToTrustStore(new File(pemFile), pemFile);

                    }

                }

                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(cryptoPrimitives.getTrustStore(), null)
                        .build();

                ConnectionSocketFactory sf;
                if (null != properties &&
                        "true".equals(properties.getProperty("allowAllHostNames"))) {
                    AllHostsSSLSocketFactory msf = new AllHostsSSLSocketFactory(cryptoPrimitives.getTrustStore());
                    msf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                    sf = msf;
                } else {
                    sf = new SSLConnectionSocketFactory(sslContext);
                }

                registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", sf)
                        .register("http", new PlainConnectionSocketFactory())
                        .build();

            } catch (Exception e) {
                logger.error(e);
                throw new InvalidArgumentException(e);
            }
        }

    }

    private class AllHostsSSLSocketFactory extends SSLSocketFactory {
        final SSLContext sslContext = SSLContext.getInstance("TLS");

        AllHostsSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] {tm}, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }

    private String getURL(String url) throws URISyntaxException, MalformedURLException {
        URIBuilder uri = new URIBuilder(url);
        if (caName != null) {
             uri.addParameter("ca", caName);
        }
        return uri.build().toURL().toString();
    }

}

