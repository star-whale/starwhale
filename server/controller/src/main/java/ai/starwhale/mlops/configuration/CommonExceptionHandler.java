/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
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



