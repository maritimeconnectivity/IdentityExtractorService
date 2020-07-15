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
    let fileUploader = $("#fileUploader");
    let textArea = $("#textinput");
    let decodeBtn = $("#decodeBtn");
    let readAttrBtn = $("#readAttrBtn");
    let resultDiv = $("#result");
    let clearBtn = $("#clearBtn");
    let fileContent;
    let decoded;

    const mcpTypeAttributes ={
        "organization":["ou", "mrn", "name", "email", "url", "address", "country", "mrnSubsidiary", "homeMMSUrl"],
        "vessel":["ou", "o", "mrn", "cn", "country", "permissions", "imoNumber", "mmsiNumber", "callSign", "flagState", "aisShipType", "portOfRegister", "country", "mrnSubsidiary", "homeMmsUrl"],
        "mms":["ou", "o", "mrn", "cn", "country", "url", "mrnSubsidiary", "homeMmsUrl", "permissions"],
        "user":["ou", "o", "mrn", "cn", "country", "email", "firstName", "lastName", "permissions"],
        "service":["ou", "o", "mrn", "cn", "country", "shipMrn", "mrnSubsidiary", "homeMmsUrl", "permissions"],
        "device":["ou", "o", "mrn", "cn", "country", "mrnSubsidiary", "homeMmsUrl", "permissions"]
    };

    textArea.on('change', () => {
        fileContent = textArea.val();
    });

    fileUploader.on('change', () => {
        let file = fileUploader.prop('files')[0];
        if (file) {
            let reader = new FileReader();
            reader.onloadend = function () {
                fileContent = reader.result;
                textArea.val(fileContent);
            }
            reader.readAsText(file);
        }
    });

    decodeBtn.click(() => {
       let file = fileUploader.prop('files')[0];
       let toBeSent;
       if (fileContent) {
           toBeSent = fileContent;
       } else if (file) {
           let reader = new FileReader();
           reader.onloadend = function () {
               toBeSent = reader.result;
               textArea.val(toBeSent);
           }
           reader.readAsText(file);
       } else if (textArea.val()) {
           toBeSent = textArea.val();
       }
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
        let file = fileUploader.prop('files')[0];
        let toBeSent;
        if (fileContent) {
            toBeSent = fileContent;
        } else if (file) {
            let reader = new FileReader();
            reader.onloadend = function () {
                toBeSent = reader.result;
                textArea.val(toBeSent);
            }
            reader.readAsText(file);
        } else if (textArea.val()) {
            toBeSent = textArea.val();
        }
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

    clearBtn.click(() => {
        resultDiv.empty();
        textArea.val(null);
        fileUploader.val('');
        fileContent = null;
        decoded = null;
    });
});