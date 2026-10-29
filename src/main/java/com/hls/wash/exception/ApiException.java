package com.hls.wash.exception;

import org.springframework.http.HttpStatus;

/**
 * 带业务错误码的异常，由 GlobalExceptionHandler 统一转换为 ApiResponse。
 */
public class ApiException extends RuntimeException {

    private final int code;
    private final HttpStatus status;

    public ApiException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ApiException param(String message) {
        return new ApiException(ErrorCodes.PARAM_INVALID, HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCodes.NOT_FOUND, HttpStatus.NOT_FOUND, message);
    }

    public static ApiException duplicateCode(String message) {
        return new ApiException(ErrorCodes.DUPLICATE_CODE, HttpStatus.CONFLICT, message);
    }

    public static ApiException unsupportedImageType(String message) {
        return new ApiException(ErrorCodes.UNSUPPORTED_IMAGE_TYPE, HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
    }

    public static ApiException imageTooLarge(String message) {
        return new ApiException(ErrorCodes.IMAGE_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    public static ApiException server(String message) {
        return new ApiException(ErrorCodes.SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
