package net.maritimeconnectivity.extractid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.maritimeconnectivity.pki.ocsp.CertStatus;
import net.maritimeconnectivity.pki.ocsp.OCSPClient;

import java.net.URL;

@Getter
@Setter
@ToString
public class OCSPResult {
    private String ocspResponderUri;
    private String certStatus;

    public OCSPResult(URL uri, CertStatus status){
        ocspResponderUri = uri.toString();
        certStatus = status.toString();
    }
}
