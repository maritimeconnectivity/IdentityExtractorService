package net.maritimeconnectivity.extractid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.simple.JSONObject;

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
