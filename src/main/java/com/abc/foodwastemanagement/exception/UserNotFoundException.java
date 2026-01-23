package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

public class UserNotFoundException extends AppException {

    public UserNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
