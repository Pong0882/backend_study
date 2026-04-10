package com.pongtorich.pong_to_rich.exception.auth;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
