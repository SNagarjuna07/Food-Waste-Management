package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

// Used when:
// -> Quantity is negative
// -> Pickup date is in the past
// -> Required field missing
public class InvalidRequestException extends AppException {

    public InvalidRequestException(ErrorCode code, String message) {
        super(code, message);
    }
}
