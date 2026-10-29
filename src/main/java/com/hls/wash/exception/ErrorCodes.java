package com.hls.wash.exception;

/**
 * 业务错误码。
 */
public final class ErrorCodes {

    public static final int OK = 0;
    public static final int PARAM_INVALID = 40001;
    public static final int NOT_FOUND = 40401;
    public static final int DUPLICATE_CODE = 40901;
    public static final int UNSUPPORTED_IMAGE_TYPE = 41501;
    public static final int IMAGE_TOO_LARGE = 41301;
    public static final int SERVER_ERROR = 50000;

    private ErrorCodes() {
    }
}
