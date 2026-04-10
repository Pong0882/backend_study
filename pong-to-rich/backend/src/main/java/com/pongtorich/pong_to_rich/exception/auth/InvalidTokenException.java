package com.pongtorich.pong_to_rich.exception.auth;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
