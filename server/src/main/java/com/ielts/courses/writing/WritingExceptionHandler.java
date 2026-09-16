package com.ielts.courses.writing;

import com.ielts.courses.common.ApiResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 写作模块业务异常 → 业务码 40303（HTTP 403，保留登录态，前端引导开通）。 */
@RestControllerAdvice
public class WritingExceptionHandler {

    @ExceptionHandler(WritingQuotaService.WritingAccessDeniedException.class)
    public ApiResult<Void> handleAccessDenied(WritingQuotaService.WritingAccessDeniedException ex) {
        return ApiResult.error(40303, ex.getMessage(), "WRITING_NO_ACCESS");
    }
}
