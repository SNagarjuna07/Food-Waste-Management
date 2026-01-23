package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

// Used when:
// -> Donation not found
// -> User not found
// -> Collection center not found
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}

