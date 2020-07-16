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
import net.maritimeconnectivity.pki.CertificateHandler;
import net.maritimeconnectivity.pki.PKIIdentity;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.JSONObject;
import sun.security.provider.certpath.OCSP;
import sun.security.x509.X509CertImpl;

import java.io.IOException;
import java.net.URI;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

import static sun.security.provider.certpath.OCSP.getResponderURI;

@RestController
@Slf4j
public class ExtractIdentityController {

    private final static String PEM_START = "-----BEGIN CERTIFICATE-----";
    private final static String PEM_END = "-----END CERTIFICATE-----";

    /**
     * Takes a PEM certificate and returns the PKI Identity of the entity within the certificate
     * @param pemCert the PEM certificate as a string
     * @return        the PKI Identity within the certificate
     */
    @RequestMapping(
            value = "/api/extract/mcp",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = "application/x-pem-file"
    )
    public ResponseEntity<?> extractIdentityFromCert(@RequestBody String pemCert) {
        if (pemCert.endsWith("\n")) {
            pemCert = pemCert.trim();
        }
        if (pemCert.startsWith("-----BEGIN PRIVATE KEY-----")) {
            return new ResponseEntity<>("This is a private key. You should NEVER give your private key to anybody!", HttpStatus.BAD_REQUEST);
        } else if (!pemCert.startsWith(PEM_START) || !pemCert.endsWith(PEM_END)) {
            return new ResponseEntity<>("Request does not contain a valid PEM encoded certificate", HttpStatus.BAD_REQUEST);
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
            value = "/api/extract/x509",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = "application/x-pem-file"
    )
    public ResponseEntity<?> extractCertAttributes(@RequestBody String pemCert) {
        if (pemCert.endsWith("\n")) {
            pemCert = pemCert.trim();
        }
        if (pemCert.startsWith("-----BEGIN PRIVATE KEY-----")) {
            return new ResponseEntity<>("This is a private key. You should NEVER give your private key to anybody!", HttpStatus.BAD_REQUEST);
        } else if (!pemCert.startsWith(PEM_START) || !pemCert.endsWith(PEM_END)) {
            return new ResponseEntity<>("Request does not contain a valid PEM encoded certificate", HttpStatus.BAD_REQUEST);
        }

        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);

        JSONObject obj = new JSONObject();
        obj.put("valid from", getGracefulDate(cert.getNotBefore()));
        obj.put("valid to", getGracefulDate(cert.getNotAfter()));
        obj.put("subject",cert.getSubjectDN().getName());
        obj.put("issuer",cert.getIssuerDN().getName());

        return new ResponseEntity<>(obj, HttpStatus.OK);
    }

    /**
     * Takes a PEM certificate and returns the X.509 certificate attributes
     * @param integratedCertsJson the integrated string of the PEM certificate and the issuer certificate
     * @return        the certificate attributes
     */
    @RequestMapping(
            value = "/api/extract/ocsp",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = "application/json"
    )
    public ResponseEntity<?> checkOCSP(@RequestBody String integratedCertsJson) throws CertificateException, IOException, CertPathValidatorException, ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        try{
            jsonObject = (JSONObject) parser.parse(integratedCertsJson);
        } catch (ParseException e) {
            return new ResponseEntity<>("Given request body is not properly structured.", HttpStatus.BAD_REQUEST);
        }
        String pemCert = (String) jsonObject.get("certificate");
        String pemCertSubCA = (String) jsonObject.get("issuerCertificate");

        if (pemCert.endsWith("\n")) {
            pemCert = pemCert.trim();
        }
        if (pemCert.startsWith("-----BEGIN PRIVATE KEY-----")) {
            return new ResponseEntity<>("This is a private key. You should NEVER give your private key to anybody!", HttpStatus.BAD_REQUEST);
        } else if (!pemCert.startsWith(PEM_START) || !pemCert.endsWith(PEM_END)) {
            return new ResponseEntity<>("Request does not contain a valid PEM encoded certificate", HttpStatus.BAD_REQUEST);
        }

        if (pemCertSubCA.endsWith("\n")) {
            pemCertSubCA = pemCertSubCA.trim();
        }
        if (pemCertSubCA.startsWith("-----BEGIN PRIVATE KEY-----")) {
            return new ResponseEntity<>("This is a private key. You should NEVER give your private key to anybody!", HttpStatus.BAD_REQUEST);
        } else if (!pemCertSubCA.startsWith(PEM_START) || !pemCertSubCA.endsWith(PEM_END)) {
            return new ResponseEntity<>("Request does not contain a valid PEM encoded issuer certificate", HttpStatus.BAD_REQUEST);
        }

        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);
        X509Certificate issuerCert = CertificateHandler.getCertFromPem(pemCertSubCA);
        OCSP.RevocationStatus status = null;
        try{
            status = check(cert,issuerCert);
        } catch (IOException e) {
            return new ResponseEntity<>("Error occurred in checking revocation status: " + e.toString(), HttpStatus.BAD_REQUEST);
        } catch (CertPathValidatorException e) {
            return new ResponseEntity<>("Error occurred in checking revocation status: " + e.toString(), HttpStatus.BAD_REQUEST);
        } catch (CertificateException e) {
            return new ResponseEntity<>("Error occurred in checking revocation status: " + e.toString(), HttpStatus.BAD_REQUEST);
        }
        JSONObject obj = new JSONObject();
        obj.put("ocsp responder uri", getResponderURI(X509CertImpl.toImpl(cert)));
        obj.put("cert status", status.getCertStatus().toString());
        if(status.getRevocationTime() != null)
            obj.put("revoked time", getGracefulDate(status.getRevocationTime()));
        return new ResponseEntity<>(obj, HttpStatus.OK);
    }

    private String getGracefulDate(Date date){
        String pattern = "yyyy/MM/dd HH:mm:ss Z";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    public OCSP.RevocationStatus check(X509Certificate cert,
                                              X509Certificate issuerCert)
            throws IOException, CertPathValidatorException, CertificateException {
        URI responderURI = null;

        X509CertImpl certImpl = X509CertImpl.toImpl(cert);
        responderURI = getResponderURI(certImpl);
        if (responderURI == null) {
            throw new CertPathValidatorException
                    ("No OCSP Responder URI in certificate");
        }
        return OCSP.check(cert, issuerCert, responderURI, cert, null);
    }

}
