package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

// Used when:
// Username already exists
// Email already exists
public class ResourceAlreadyExistsException extends AppException {

    public ResourceAlreadyExistsException(ErrorCode code, String message) {
        super(code, message);
    }
}
