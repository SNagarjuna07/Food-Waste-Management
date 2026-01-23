package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

import lombok.Getter;

@Getter
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
