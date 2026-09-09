package com.ielts.courses.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResult<Void>> handleApi(ApiException ex) {
        BizCode bc = ex.bizCode();
        HttpStatus status = switch (bc) {
            case SESSION_KICKED, NEED_BIND_PHONE -> HttpStatus.UNAUTHORIZED;
            case NO_ENTITLEMENT, COURSE_EXPIRED -> HttpStatus.FORBIDDEN;
            case COUPON_USED -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(ApiResult.error(bc.code(), ex.getMessage(), bc.name()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(BizCode.NOT_FOUND.code(), "资源不存在", BizCode.NOT_FOUND.name()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleOther(Exception ex) {
        log.error("未处理异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(50000, "服务异常", null));
    }
}
