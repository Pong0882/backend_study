package com.pongtorich.pong_to_rich.exception.auth;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
