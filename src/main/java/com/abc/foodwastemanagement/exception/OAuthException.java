package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

public class OAuthException extends AppException{

    public OAuthException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
