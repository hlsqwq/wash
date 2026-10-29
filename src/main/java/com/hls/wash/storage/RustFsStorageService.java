package com.hls.wash.storage;

import com.hls.wash.config.StorageProperties;
import com.hls.wash.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * RustFS S3 兼容存储适配器。
 *
 * <p>使用 path-style 访问，endpoint 必须是 S3 API 地址（默认 9000 端口），
 * 不能使用控制台地址（9001/rustfs/console）。凭证只从环境变量或本地 profile 读取。</p>
 */
@Service
public class RustFsStorageService implements ObjectStorageService {

    private static final Logger log = LoggerFactory.getLogger(RustFsStorageService.class);

    private final StorageProperties properties;
    private final S3Client client;
    private volatile boolean bucketReady = false;

    public RustFsStorageService(StorageProperties properties) {
        this.properties = properties;
        this.client = buildClient(properties);
    }

    private S3Client buildClient(StorageProperties props) {
        if (!props.hasCredentials() || props.getEndpoint() == null || props.getEndpoint().isBlank()) {
            // 不在构造期抛异常：允许应用在未配置对象存储时启动，真正使用存储时再给出明确错误。
            return null;
        }
        Duration requestTimeout = Duration.ofSeconds(Math.max(5, props.getRequestTimeoutSeconds()));
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of(props.getRegion() == null || props.getRegion().isBlank()
                        ? "us-east-1" : props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(requestTimeout)
                        .apiCallAttemptTimeout(Duration.ofSeconds(Math.max(5, props.getRequestTimeoutSeconds() / 2)))
                        .build())
                .build();
    }

    private S3Client requireClient() {
        if (client == null) {
            throw new StorageException("尚未配置 RustFS 凭证（RUSTFS_ACCESS_KEY / RUSTFS_SECRET_KEY）");
        }
        return client;
    }

    private String bucket() {
        if (properties.getBucket() == null || properties.getBucket().isBlank()) {
            throw new StorageException("尚未配置 RustFS bucket（RUSTFS_BUCKET）");
        }
        return properties.getBucket();
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            S3Client s3 = requireClient();
            String bucket = bucket();
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
                bucketReady = true;
                return;
            } catch (NoSuchBucketException e) {
                if (!properties.isAutoCreateBucket()) {
                    throw new StorageException("RustFS bucket 不存在且未启用自动创建: " + bucket, e);
                }
                createBucket(s3, bucket);
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    if (!properties.isAutoCreateBucket()) {
                        throw new StorageException("RustFS bucket 不存在且未启用自动创建: " + bucket, e);
                    }
                    createBucket(s3, bucket);
                } else {
                    throw new StorageException("无法访问 RustFS bucket: " + e.statusCode(), e);
                }
            } catch (SdkException e) {
                throw new StorageException("无法连接 RustFS: " + e.getMessage(), e);
            }
        }
    }

    private void createBucket(S3Client s3, String bucket) {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("已创建 RustFS bucket: {}", bucket);
        } catch (SdkException e) {
            throw new StorageException("创建 RustFS bucket 失败: " + bucket, e);
        }
    }

    @Override
    public void put(String key, byte[] data, String contentType) {
        ensureBucket();
        try {
            requireClient().putObject(PutObjectRequest.builder()
                            .bucket(bucket())
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) data.length)
                            .build(),
                    RequestBody.fromBytes(data));
        } catch (SdkException e) {
            throw new StorageException("上传对象失败: " + key, e);
        }
    }

    @Override
    public InputStream get(String key) {
        ensureBucket();
        try {
            ResponseInputStream<GetObjectResponse> stream = requireClient().getObject(
                    GetObjectRequest.builder().bucket(bucket()).key(key).build());
            return stream;
        } catch (NoSuchKeyException e) {
            throw new StorageException("对象不存在: " + key, e);
        } catch (SdkException e) {
            throw new StorageException("读取对象失败: " + key, e);
        }
    }

    @Override
    public boolean exists(String key) {
        ensureBucket();
        try {
            requireClient().headObject(HeadObjectRequest.builder().bucket(bucket()).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new StorageException("检查对象失败: " + key, e);
        } catch (SdkException e) {
            throw new StorageException("检查对象失败: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        ensureBucket();
        try {
            requireClient().deleteObject(DeleteObjectRequest.builder().bucket(bucket()).key(key).build());
        } catch (SdkException e) {
            throw new StorageException("删除对象失败: " + key, e);
        }
    }

    @Override
    public String bucketName() {
        return bucket();
    }
}
