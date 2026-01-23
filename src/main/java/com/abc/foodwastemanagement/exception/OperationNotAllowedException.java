package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

// Used when:
// Donation already collected
// Donation already cancelled
// Invalid state transition
public class OperationNotAllowedException extends AppException {

    public OperationNotAllowedException(ErrorCode code, String message) {
        super(code, message);
    }
}
