/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.configuration;

import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.exception.SWAuthException;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.StarWhaleException;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;

@ControllerAdvice
public class CommonExceptionHandler {

    private final Logger logger = LogManager.getLogger();

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ResponseMessage<String>> handleValidationException(HttpServletRequest request, ValidationException ex) {
        logger.error("ValidationException {}\n", request.getRequestURI(), ex);

        return ResponseEntity
                .badRequest()
                .body(new ResponseMessage<>(Code.validationException.name(), ex.getMessage()));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseMessage<String>> handleAccessDeniedException(HttpServletRequest request, AccessDeniedException ex) {
        logger.error("handleAccessDeniedException {}\n", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ResponseMessage<>(Code.accessDenied.name(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage<String>> handleInternalServerError(HttpServletRequest request, Exception ex) {
        logger.error("handleInternalServerError {}\n", request.getRequestURI(), ex);

        return ResponseEntity
                .internalServerError()
                .body(new ResponseMessage<>(Code.internalServerError.name(), ex.getMessage()));
    }

    @ExceptionHandler(StarWhaleApiException.class)
    public ResponseEntity<ResponseMessage<String>> handleStarWhaleApiException(HttpServletRequest request, StarWhaleApiException ex) {
        logger.error("handleInternalServerError {}\n", request.getRequestURI(), ex);

        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(new ResponseMessage<>(ex.getCode(), ex.getTip()));
    }

    @ExceptionHandler(StarWhaleException.class)
    public ResponseEntity<ResponseMessage<String>> handleStarWhaleException(HttpServletRequest request, StarWhaleException ex) {
        logger.error("handleInternalServerError {}\n", request.getRequestURI(), ex);
        HttpStatus httpStatus;
        if (ex instanceof SWValidationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof SWAuthException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof SWProcessException) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
            .status(httpStatus)
            .body(new ResponseMessage<>(ex.getCode(), ex.getTip()));
    }

}



