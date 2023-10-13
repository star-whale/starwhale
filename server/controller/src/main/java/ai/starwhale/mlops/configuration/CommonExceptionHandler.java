/*
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

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.configuration.security.JwtLoginToken;
import ai.starwhale.mlops.exception.StarwhaleException;
import ai.starwhale.mlops.exception.SwAuthException;
import ai.starwhale.mlops.exception.SwNotFoundException;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwRequestFrequentException;
import ai.starwhale.mlops.exception.SwUnavailableException;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

    private final Logger logger = LogManager.getLogger();


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseMessage<String>> handleMethodArgumentNotValidException(HttpServletRequest request,
            ConstraintViolationException ex) {
        logger.error("ConstraintViolationException {}\n", request.getRequestURI(), ex);
        return ResponseEntity
                .badRequest()
                .body(new ResponseMessage<>(Code.validationException.name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseMessage<String>> handleMethodArgumentNotValidException(HttpServletRequest request,
            MethodArgumentNotValidException ex) {
        logger.error("MethodArgumentNotValidException {}\n", request.getRequestURI(), ex);
        return ResponseEntity
                .badRequest()
                .body(new ResponseMessage<>(Code.validationException.name(),
                        ex.getAllErrors()
                                .stream()
                                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                .collect(Collectors.joining())));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ResponseMessage<String>> handleValidationException(HttpServletRequest request,
            ValidationException ex) {
        logger.error("ValidationException {}\n", request.getRequestURI(), ex);

        return ResponseEntity
                .badRequest()
                .body(new ResponseMessage<>(Code.validationException.name(), ex.getMessage()));
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseMessage<String>> handleAccessDeniedException(HttpServletRequest request,
            AccessDeniedException ex) {
        logger.error("handleAccessDeniedException {}\n", request.getRequestURI(), ex);
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtLoginToken) {
            JwtLoginToken auth = (JwtLoginToken) SecurityContextHolder.getContext().getAuthentication();
            if (auth.getPrincipal() == null) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ResponseMessage<>(Code.Unauthorized.name(), Code.Unauthorized.name()));
            }
        }
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

    @ExceptionHandler(StarwhaleApiException.class)
    public ResponseEntity<ResponseMessage<String>> handleStarwhaleApiException(HttpServletRequest request,
            StarwhaleApiException ex) {
        logger.error("handleInternalServerError {}\n", request.getRequestURI(), ex);

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(new ResponseMessage<>(ex.getCode(), ex.getTip()));
    }

    @ExceptionHandler(StarwhaleException.class)
    public ResponseEntity<ResponseMessage<String>> handleStarwhaleException(HttpServletRequest request,
            StarwhaleException ex) {
        logger.error("handleInternalServerError {}\n", request.getRequestURI(), ex);
        HttpStatus httpStatus;
        if (ex instanceof SwValidationException) {
            httpStatus = HttpStatus.BAD_REQUEST;
        } else if (ex instanceof SwAuthException) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof SwNotFoundException) {
            httpStatus = HttpStatus.NOT_FOUND;
        } else if (ex instanceof SwProcessException) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (ex instanceof SwUnavailableException) {
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof SwRequestFrequentException) {
            httpStatus = HttpStatus.TOO_MANY_REQUESTS;
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity
                .status(httpStatus)
                .body(new ResponseMessage<>(ex.getCode(), ex.getTip()));
    }

}



