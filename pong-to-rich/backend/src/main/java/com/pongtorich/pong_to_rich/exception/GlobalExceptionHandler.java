package com.pongtorich.pong_to_rich.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 비즈니스 예외 (DuplicateEmailException, InvalidCredentialsException 등)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        log.warn("[BusinessException] {}", e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    // 위에서 못 잡은 예외 전부
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return ResponseEntity
                .status(500)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
