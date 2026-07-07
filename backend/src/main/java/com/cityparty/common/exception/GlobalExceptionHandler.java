package com.cityparty.common.exception;

import com.cityparty.common.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception e) {
        String message = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException validException && validException.getBindingResult().hasErrors()) {
            message = validException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        if (e instanceof BindException bindException && bindException.getBindingResult().hasErrors()) {
            message = bindException.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return Result.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException() {
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("Unhandled server error", e);
        return Result.fail(500, "服务器内部错误");
    }
}
