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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        System.out.println(cert.toString());
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
    public ResponseEntity<?> extractCertAttributes(@RequestBody String pemCert) throws IOException {
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

    private String getGracefulDate(Date date){
        String pattern = "yyyy/MM/dd HH:mm:ss Z";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        System.out.println(sdf.format(date));
        return sdf.format(date);
    }

}
