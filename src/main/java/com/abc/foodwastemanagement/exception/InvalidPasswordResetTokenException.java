package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

public class InvalidPasswordResetTokenException extends AppException {

    public InvalidPasswordResetTokenException(ErrorCode code, String message) {
        super(code, message);
    }
}
