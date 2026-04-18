package com.pongtorich.pong_to_rich.exception.strategy;

import com.pongtorich.pong_to_rich.exception.BusinessException;
import com.pongtorich.pong_to_rich.exception.ErrorCode;

public class StrategyNotFoundException extends BusinessException {

    public StrategyNotFoundException() {
        super(ErrorCode.STRATEGY_NOT_FOUND);
    }
}
