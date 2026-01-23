package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

public class ExpiredPasswordResetTokenException extends AppException {

    public ExpiredPasswordResetTokenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
