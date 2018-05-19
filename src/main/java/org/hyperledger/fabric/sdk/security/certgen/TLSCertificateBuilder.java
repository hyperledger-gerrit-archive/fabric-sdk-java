/*
 *
 *  Copyright 2018 IBM - All Rights Reserved.
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
package org.hyperledger.fabric.sdk.security.certgen;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class TLSCertificateBuilder {
    private static final SecureRandom rand = new SecureRandom();
    private static final String defaultSignatureAlgorithm = "SHA256withECDSA";
    private static final String defaultKeyType = "EC";

    private String commonName;
    private String signatureAlgorithm;
    private String keyType;

    public TLSCertificateBuilder() {
        // Initialize a default random common name
        commonName = UUID.randomUUID().toString();
        // Initialize the signature algorithm to be ECDSA over SHA256 by default
        signatureAlgorithm = defaultSignatureAlgorithm;
        // Initialize the key type to be elliptic curve by default
        keyType = defaultKeyType;
    }

    public TLSCertificateKeyPair clientCert() throws Exception {
        return createCert(CertType.CLIENT, null);
    }

    public TLSCertificateKeyPair serverCert(String san) throws Exception {
        return createCert(CertType.SERVER, san);
    }

    private TLSCertificateKeyPair createCert(CertType certType, String san) throws Exception {
        KeyPair keyPair = createKeyPair();
        X509Certificate cert = createSelfSignedCertificate(certType, keyPair, san);
        TLSCertificateKeyPair tlsCert = TLSCertificateKeyPair.fromX509CertKeyPair(cert, keyPair);
        return tlsCert;
    }

    private X509Certificate createSelfSignedCertificate(CertType certType, KeyPair keyPair, String san) throws Exception {
        X509v3CertificateBuilder certBuilder = createCertBuilder(keyPair);

        SelfSignedKeyIdentifier keyIdentifier = new SelfSignedKeyIdentifier();
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false, keyIdentifier.subjectKeyIdentifier());
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false, keyIdentifier.authorityKeyIdentifier());
        BasicConstraints constraints = new BasicConstraints(true);
        certBuilder.addExtension(
                Extension.basicConstraints,
                true,
                constraints.getEncoded());
        KeyUsage usage = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature);
        certBuilder.addExtension(Extension.keyUsage, false, usage.getEncoded());
        certBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                certType.keyUsage().getEncoded());

        if (san != null) {
            addSAN(certBuilder, san);
        }

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
                .build(keyPair.getPrivate());
        X509CertificateHolder holder = certBuilder.build(signer);

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter.setProvider(new BouncyCastleProvider());
        return converter.getCertificate(holder);
    }

    private void addSAN(X509v3CertificateBuilder certBuilder, String san) throws Exception {
        ASN1Encodable[] subjectAlternativeNames = new ASN1Encodable[]{new GeneralName(GeneralName.dNSName, san)};
        certBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(subjectAlternativeNames));
    }

    private X509v3CertificateBuilder createCertBuilder(KeyPair keyPair) {
        X500Name subject = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, commonName)
                .build();

        Calendar notBefore = new GregorianCalendar();
        notBefore.add(Calendar.DAY_OF_MONTH, -1);
        Calendar notAfter = new GregorianCalendar();
        notAfter.add(Calendar.YEAR, 10);

        return new JcaX509v3CertificateBuilder(
                subject,
                new BigInteger(160, rand),
                notBefore.getTime(),
                notAfter.getTime(),
                subject,
                keyPair.getPublic());
    }

    private KeyPair createKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keypairGen = KeyPairGenerator.getInstance(keyType);
        keypairGen.initialize(256, rand);
        return keypairGen.generateKeyPair();
    }


    private enum CertType {
        CLIENT, SERVER;

        ExtendedKeyUsage keyUsage() {
            KeyPurposeId[] kpid = new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth};
            if (this.ordinal() == 1) {
                kpid[0] = KeyPurposeId.id_kp_serverAuth;
            }
            return new ExtendedKeyUsage(kpid);
        }
    }
}
