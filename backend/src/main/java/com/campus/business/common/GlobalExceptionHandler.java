package com.campus.business.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> business(BusinessException e) {
    return new ApiResponse<>(400, e.getMessage(), null);
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiResponse<Void> validation(Exception e) {
    String msg =
        e instanceof MethodArgumentNotValidException m
            ? m.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(x -> x.getDefaultMessage())
                .orElse("参数错误")
            : e.getMessage();
    return new ApiResponse<>(400, msg, null);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiResponse<Void> notFound() {
    return new ApiResponse<>(404, "资源不存在", null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiResponse<Void> denied() {
    return new ApiResponse<>(403, "无权执行此操作", null);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiResponse<Void> unknown(Exception e) {
    return new ApiResponse<>(500, "系统异常，请稍后重试", null);
  }
}
