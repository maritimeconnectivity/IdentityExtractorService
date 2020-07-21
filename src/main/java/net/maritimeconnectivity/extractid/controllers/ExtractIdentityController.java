/*
 * Copyright 2017 Danish Maritime Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.maritimeconnectivity.extractid.controllers;

import lombok.extern.slf4j.Slf4j;
import net.maritimeconnectivity.extractid.model.IntegratedCerts;
import net.maritimeconnectivity.extractid.model.OCSPResult;
import net.maritimeconnectivity.extractid.model.X509CertAttribute;
import net.maritimeconnectivity.pki.CertificateHandler;
import net.maritimeconnectivity.pki.PKIIdentity;
import net.maritimeconnectivity.pki.ocsp.CertStatus;
import net.maritimeconnectivity.pki.ocsp.OCSPClient;
import net.maritimeconnectivity.pki.ocsp.OCSPValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;

@RestController
@RequestMapping("/api")
@Slf4j
public class ExtractIdentityController {

    private static final String PEM_START = "-----BEGIN CERTIFICATE-----";
    private static final String PEM_END = "-----END CERTIFICATE-----";
    private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_WARNING = "This is a private key. You should NEVER give your private key to anybody!";
    private static final String NOT_VALID_WARNING = "Request does not contain a valid PEM encoded certificate";

    /**
     * Takes a PEM certificate and returns the PKI Identity of the entity within the certificate
     * @param pemCert the PEM certificate as a string
     * @return        the PKI Identity within the certificate
     */
    @RequestMapping(
            value = "/extract/mcp",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = "application/x-pem-file"
    )
    public ResponseEntity<?> extractIdentityFromCert(@RequestBody String pemCert) {
        pemCert = pemCert.trim();
        if (pemCert.startsWith(PRIVATE_KEY_HEADER)) {
            return new ResponseEntity<>(PRIVATE_KEY_WARNING, HttpStatus.BAD_REQUEST);
        } else if (!pemCert.startsWith(PEM_START) || !pemCert.endsWith(PEM_END)) {
            return new ResponseEntity<>(NOT_VALID_WARNING, HttpStatus.BAD_REQUEST);
        }

        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);
        PKIIdentity identity = CertificateHandler.getIdentityFromCert(cert);
        return new ResponseEntity<>(identity, HttpStatus.OK);
    }

    /**
     * Takes a PEM certificate and returns the X.509 certificate attributes
     * @param pemCert the PEM certificate as a string
     * @return        the certificate attributes
     */
    @RequestMapping(
            value = "/extract/x509",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = "application/x-pem-file"
    )
    public ResponseEntity<?> extractCertAttributes(@RequestBody String pemCert) {
        pemCert = pemCert.trim();
        if (pemCert.startsWith(PRIVATE_KEY_HEADER)) {
            return new ResponseEntity<>(PRIVATE_KEY_WARNING, HttpStatus.BAD_REQUEST);
        } else if (!pemCert.startsWith(PEM_START) || !pemCert.endsWith(PEM_END)) {
            return new ResponseEntity<>(NOT_VALID_WARNING, HttpStatus.BAD_REQUEST);
        }

        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);

        X509CertAttribute attr = new X509CertAttribute(cert);

        return new ResponseEntity<>(attr, HttpStatus.OK);
    }

    /**
     * Checks the revocation status of a certificate and its issuer using OCSP
     * @param integratedCerts a JSON object containing the PEM encoded certificate and the issuer certificate
     * @return        the certificate attributes
     */
    @RequestMapping(
            value = "/extract/ocsp",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> checkOCSP(@RequestBody IntegratedCerts integratedCerts) {
        String pemCert = integratedCerts.getCertificate();
        String pemCertSubCA = integratedCerts.getIssuerCertificate();

        pemCert = pemCert.trim();
        if (pemCert.startsWith(PRIVATE_KEY_HEADER)) {
            return new ResponseEntity<>(PRIVATE_KEY_WARNING, HttpStatus.BAD_REQUEST);
        } else if (!pemCert.startsWith(PEM_START) || !pemCert.endsWith(PEM_END)) {
            return new ResponseEntity<>(NOT_VALID_WARNING, HttpStatus.BAD_REQUEST);
        }

        pemCertSubCA = pemCertSubCA.trim();
        if (pemCertSubCA.startsWith(PRIVATE_KEY_HEADER)) {
            return new ResponseEntity<>(PRIVATE_KEY_WARNING, HttpStatus.BAD_REQUEST);
        } else if (!pemCertSubCA.startsWith(PEM_START) || !pemCertSubCA.endsWith(PEM_END)) {
            return new ResponseEntity<>("Request does not contain a valid PEM encoded issuer certificate", HttpStatus.BAD_REQUEST);
        }

        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);
        X509Certificate issuerCert = CertificateHandler.getCertFromPem(pemCertSubCA);

        CertStatus status;
        try {
            OCSPClient ocspClient = new OCSPClient(issuerCert, cert);
            status = ocspClient.getCertificateStatus();
        } catch (OCSPValidationException e) {
            log.error("OCSP failed", e);
            status = CertStatus.UNKNOWN;
        }
        OCSPResult result = new OCSPResult(OCSPClient.getOcspUrlFromCertificate(cert), status);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
