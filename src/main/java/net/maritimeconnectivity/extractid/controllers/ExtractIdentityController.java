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

import net.maritimecloud.pki.CertificateHandler;
import net.maritimecloud.pki.PKIIdentity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.cert.X509Certificate;

@RestController
public class ExtractIdentityController {

    /**
     * Takes a PEM certificate and returns the PKI Identity of the entity within the certificate
     * @param pemCert the PEM certificate as a string
     * @return        the PKI Identity within the certificate
     */
    @RequestMapping(
            value = "/api/extract",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public PKIIdentity extractIdentityFromCert(@RequestBody String pemCert) {
        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);
        PKIIdentity identity = CertificateHandler.getIdentityFromCert(cert);
        return identity;
    }

}
