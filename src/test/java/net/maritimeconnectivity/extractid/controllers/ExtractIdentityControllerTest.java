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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.maritimecloud.pki.CertificateHandler;
import net.maritimecloud.pki.PKIIdentity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ExtractIdentityController.class, secure = false)
public class ExtractIdentityControllerTest {

    @Autowired
    private MockMvc mvc;

    private Gson gson = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

    @Test
    public void testExtractIdentityFromCert() {
        String certPath = "src/test/resources/Certificate_My_vessel.pem";
        String pemCert = null;
        try {
            pemCert = Files.lines(Paths.get(certPath)).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not load certificate from file");
        }
        X509Certificate cert = CertificateHandler.getCertFromPem(pemCert);
        PKIIdentity identity = CertificateHandler.getIdentityFromCert(cert);

        String identityJson = gson.toJson(identity);

        try {
            MvcResult result = mvc.perform(post("/api/extract").content(pemCert)
                    .contentType("application/x-pem-file")).andReturn();
            MockHttpServletResponse response = result.getResponse();
            assertEquals(200, response.getStatus());
            assertEquals(identityJson, response.getContentAsString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed");
        }
    }
}