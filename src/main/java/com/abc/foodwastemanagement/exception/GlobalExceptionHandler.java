package com.abc.foodwastemanagement.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.abc.foodwastemanagement.enums.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {

        log.warn("Resource not found exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(ResourceAlreadyExistsException ex) {

        log.warn("Resource already exists exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {

        log.warn("Invalid request exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(UnauthorizedActionException ex) {

        log.warn("Unauthorized action exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleConflict(OperationNotAllowedException ex) {

        log.warn("Operation not allowed exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidPasswordResetTokenException ex) {

        log.warn("Invalid password reset token exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PasswordResetTokenAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleUsedToken(PasswordResetTokenAlreadyUsedException ex) {

        log.warn("Password reset token already used exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ExpiredPasswordResetTokenException.class)
    public ResponseEntity<ErrorResponse> handleExpiredToken(ExpiredPasswordResetTokenException ex) {

        log.warn("Password reset token expired exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.GONE);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUser(UserNotFoundException ex) {

        log.warn("User not found exception occured: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OAuthException.class)
    public ResponseEntity<ErrorResponse> handleOAuth(OAuthException ex) {

        log.warn("OAuth authentication exception occurred: code = {} message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {

        log.warn("Handled application exception: code = {}  message = {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {

        log.error("Unkown exception occured: ({})", ex.getMessage(), ex);

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorCode.INTERNAL_ERROR.name(),
                "Something went wrong. Please try again later."
        );
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {

        log.warn("Access denied exception occured: ", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.UNAUTHORIZED_ACTION.name(),
                "Access denied"
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }


    private ResponseEntity<ErrorResponse> buildResponse(AppException ex, HttpStatus status) {
        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                ex.getErrorCode().name(),
                ex.getMessage()
        );
        return ResponseEntity.status(status).body(response);
    }
}
