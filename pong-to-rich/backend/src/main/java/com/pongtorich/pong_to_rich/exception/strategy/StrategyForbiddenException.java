package com.pongtorich.pong_to_rich.exception.strategy;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class StrategyForbiddenException extends BusinessException {

    public StrategyForbiddenException() {
        super(ErrorCode.STRATEGY_FORBIDDEN);
    }
}
