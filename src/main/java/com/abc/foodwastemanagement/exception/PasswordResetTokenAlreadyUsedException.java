package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

public class PasswordResetTokenAlreadyUsedException extends AppException{

    public PasswordResetTokenAlreadyUsedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
