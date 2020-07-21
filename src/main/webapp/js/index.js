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

$(document).ready(function() {
    let fileUploader = $("#certFileUploader");
    let issuerFileUploader = $("#issuerFileUploader");
    let certPemText = $("#certPemInput");
    let issuerPemText = $("#issuerPemInput");
    let decodeBtn = $("#decodeBtn");
    let readAttrBtn = $("#readAttrBtn");
    let ocspBtn = $("#ocspBtn");
    let resultDiv = $("#result");
    let clearBtn = $("#clearBtn");
    let fileContent;
    let issuerFileContent;
    let decoded;

    clear();

    const mcpTypeAttributes ={
        "organization":["ou", "mrn", "name", "email", "url", "address", "country", "mrnSubsidiary", "homeMMSUrl"],
        "vessel":["ou", "o", "mrn", "cn", "country", "permissions", "imoNumber", "mmsiNumber", "callSign", "flagState", "aisShipType", "portOfRegister", "country", "mrnSubsidiary", "homeMmsUrl"],
        "mms":["ou", "o", "mrn", "cn", "country", "url", "mrnSubsidiary", "homeMmsUrl", "permissions"],
        "user":["ou", "o", "mrn", "cn", "country", "email", "firstName", "lastName", "permissions"],
        "service":["ou", "o", "mrn", "cn", "country", "shipMrn", "mrnSubsidiary", "homeMmsUrl", "permissions"],
        "device":["ou", "o", "mrn", "cn", "country", "mrnSubsidiary", "homeMmsUrl", "permissions"]
    };

    certPemText.on('change', () => {
        fileContent = certPemText.val();
    });

    issuerPemText.on('change', () => {
        issuerFileContent = issuerPemText.val();
    });

    fileUploader.on('change', () => {
        let file = fileUploader.prop('files')[0];
        if (file) {
            let reader = new FileReader();
            reader.onloadend = function () {
                fileContent = reader.result;
                certPemText.val(fileContent);
            }
            reader.readAsText(file);
        }
    });

    issuerFileUploader.on('change', () => {
        let file = issuerFileUploader.prop('files')[0];
        if (file) {
            let reader = new FileReader();
            reader.onloadend = function () {
                issuerFileContent = reader.result;
                issuerPemText.val(issuerFileContent);
            }
            reader.readAsText(file);
        }
    });

    decodeBtn.click(() => {
        let toBeSent = setToBeSent(fileUploader.prop('files')[0], certPemText, fileContent);

       if (toBeSent) {
           $.post({
               url: '/api/extract/mcp',
               data: toBeSent,
               success: data => {
                   decoded = data;
                   resultDiv.empty();
                   for (const [key, value] of Object.entries(decoded)) {
                       if (mcpTypeAttributes[decoded['ou']].includes(key)){
                           if(value)
                               resultDiv.append(`<p><b>${key}</b> : ${value}</p>`);
                           else
                               resultDiv.append(`<p><b>${key}</b> : <em>${value}</em></p>`);
                       }
                   }
               },
               error: e => {
                   alert(e.responseText);
               },
               contentType: 'application/x-pem-file'
           });
       }
    });

    readAttrBtn.click(() => {
        let toBeSent = setToBeSent(fileUploader.prop('files')[0], certPemText, fileContent);

        if (toBeSent) {
            $.post({
                url: '/api/extract/x509',
                data: toBeSent,
                success: data => {
                    decoded = data;
                    resultDiv.empty();
                    for (const [key, value] of Object.entries(decoded)) {
                        resultDiv.append(`<p><b>${key}</b> : ${value}</p>`);
                    }
                },
                error: e => {
                    alert(e.responseText);
                },
                contentType: 'application/x-pem-file'
            });
        }
    });

    ocspBtn.click(() => {
        resultDiv.empty();
        if (issuerFileContent === null || issuerPemText.val() === ''){
            $("#issuerCertInput").show();
            resultDiv.append('<p>ERROR: Please enter issuer\'s certificate in PEM format.</p>')
            issuerPemText.focus();
            return;
        }

        let toBeSent = setToBeSent(fileUploader.prop('files')[0], certPemText, fileContent);
        let toBeSentIssuer = setToBeSent(issuerFileUploader.prop('files')[0], issuerPemText, issuerFileContent);
        if (toBeSent && toBeSentIssuer) {
            resultDiv.append('<p>OCSP request has sent! Wait for response..........</p>');
            let jsonString = {certificate: toBeSent, issuerCertificate: toBeSentIssuer};
            $.post({
                url: '/api/extract/ocsp',
                data: JSON.stringify(jsonString),
                success: data => {
                    decoded = data;
                    resultDiv.empty();
                    for (const [key, value] of Object.entries(decoded)) {
                        resultDiv.append(`<p><b>${key}</b> : ${value}</p>`);
                    }
                },
                error: e => {
                    alert(e.responseText);
                },
                contentType: 'application/json'
            });
        }
    });

    clearBtn.click(() => {
        clear();
    });

    function clear(){
        resultDiv.empty();
        certPemText.val(null);
        fileUploader.val('');
        fileContent = null;
        decoded = null;
        issuerPemText.val(null);
        issuerFileUploader.val('');
        issuerFileContent = null;
        $("#issuerCertInput").hide();
    }

    function setToBeSent(file, textAreaElement, loadedContent){
        let toBeSent;
        if (loadedContent) {
            toBeSent = loadedContent;
        } else if (file) {
            let reader = new FileReader();
            reader.onloadend = function () {
                toBeSent = reader.result;
                textAreaElement.val(toBeSent);
            }
            reader.readAsText(file);
        } else if (textAreaElement.val()) {
            toBeSent = textAreaElement.val();
        }
        return toBeSent;
    }
});
