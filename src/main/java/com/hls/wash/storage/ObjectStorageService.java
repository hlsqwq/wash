package com.hls.wash.storage;

import java.io.InputStream;

/**
 * 对象存储抽象。当前唯一实现是 RustFS（S3 兼容）。
 */
public interface ObjectStorageService {

    /** 上传对象，key 为桶内路径。已存在时覆盖。 */
    void put(String key, byte[] data, String contentType);

    /** 下载对象；调用方负责关闭流。 */
    InputStream get(String key);

    boolean exists(String key);

    void delete(String key);

    String bucketName();
}
