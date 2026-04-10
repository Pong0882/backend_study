package com.pongtorich.pong_to_rich.exception.auth;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class ExpiredTokenException extends BusinessException {

    public ExpiredTokenException() {
        super(ErrorCode.EXPIRED_TOKEN);
    }
}
