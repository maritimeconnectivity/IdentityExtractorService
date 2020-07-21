/*
 * Copyright 2020 Maritime Connectivity Platform Consortium
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

package net.maritimeconnectivity.extractid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;

@Getter
@Setter
@ToString
public class X509CertAttribute {
    private String validFrom;
    private String validTo;
    private String subject;
    private String issuer;

    public X509CertAttribute(X509Certificate cert){
        this.validFrom = getGracefulDate(cert.getNotBefore());
        this.validTo = getGracefulDate(cert.getNotAfter());
        this.subject = cert.getSubjectDN().getName();
        this.issuer = cert.getIssuerDN().getName();
    }

    private String getGracefulDate(Date date){
        String pattern = "yyyy/MM/dd HH:mm:ss Z";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
}
