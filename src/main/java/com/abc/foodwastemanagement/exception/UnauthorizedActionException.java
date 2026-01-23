package com.abc.foodwastemanagement.exception;

import com.abc.foodwastemanagement.enums.ErrorCode;

// Used when:
// -> User tries admin-only action
// -> Collector tries donor action
public class UnauthorizedActionException extends AppException {

    public UnauthorizedActionException(ErrorCode code, String message) {
        super(code, message);
    }
}
