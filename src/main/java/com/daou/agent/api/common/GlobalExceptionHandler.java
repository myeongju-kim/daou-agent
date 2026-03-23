package com.daou.agent.api.common;

import com.daou.agent.infrastructure.logging.CorrelationIdHolder;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ApiContract.VERSION,
                        "bad_request",
                        "BAD_REQUEST",
                        e.getMessage(),
                        CorrelationIdHolder.getOrCreate()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ApiContract.VERSION,
                        "bad_request",
                        "VALIDATION_ERROR",
                        "요청 값 검증에 실패했습니다.",
                        CorrelationIdHolder.getOrCreate()
                ));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        ApiContract.VERSION,
                        "not_found",
                        "NOT_FOUND",
                        e.getMessage(),
                        CorrelationIdHolder.getOrCreate()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("event=api.error.unhandled correlationId={} message={}", CorrelationIdHolder.getOrCreate(), e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ApiContract.VERSION,
                        "error",
                        "INTERNAL_ERROR",
                        "내부 오류가 발생했습니다.",
                        CorrelationIdHolder.getOrCreate()
                ));
    }
}
