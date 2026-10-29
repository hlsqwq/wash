package com.hls.wash.exception;

/**
 * 对象存储访问异常。对外只返回通用提示，详细原因只写服务端日志，避免泄漏凭证与内部地址。
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
