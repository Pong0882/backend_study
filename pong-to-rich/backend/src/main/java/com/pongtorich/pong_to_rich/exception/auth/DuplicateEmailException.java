package com.pongtorich.pong_to_rich.exception.auth;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
