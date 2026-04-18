package com.pongtorich.pong_to_rich.exception.strategy;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class StrategyInvalidStatusException extends BusinessException {

    public StrategyInvalidStatusException() {
        super(ErrorCode.STRATEGY_INVALID_STATUS);
    }
}
