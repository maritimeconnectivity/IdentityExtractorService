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

package net.maritimeconnectivity.extractid.exceptions;

import net.maritimeconnectivity.extractid.model.ExceptionModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MCPExceptionResolver {

    @ExceptionHandler(MCPBasicRestException.class)
    public ResponseEntity<ExceptionModel> processRestError(MCPBasicRestException e) {
        ExceptionModel exceptionModel = new ExceptionModel(e.getTimestamp(), e.getStatus().value(), e.getError(), e.getErrorMessage(), e.path);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(exceptionModel, httpHeaders, e.getStatus());
    }
}
