package com.hls.wash.service;

import com.hls.wash.dto.ImageUploadResponse;
import com.hls.wash.entity.MediaEntity;
import com.hls.wash.exception.ApiException;
import com.hls.wash.exception.StorageException;
import com.hls.wash.repository.MediaRepository;
import com.hls.wash.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * 图片上传、去重与读取。
 *
 * <p>对象路径由内容 MD5 + 规范扩展名组成，不使用用户文件名，
 * 从根源上避免路径穿越和任意文件上传。</p>
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final MediaRepository mediaRepository;
    private final ObjectStorageService storage;
    private final UrlService urlService;

    public MediaService(MediaRepository mediaRepository, ObjectStorageService storage, UrlService urlService) {
        this.mediaRepository = mediaRepository;
        this.storage = storage;
        this.urlService = urlService;
    }

    public record LoadedImage(byte[] bytes, String contentType, String filename) {
    }

    public ImageUploadResponse uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.param("请选择要上传的图片");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.param("读取上传文件失败");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        MediaEntity media = storeImage(filename, bytes);
        return new ImageUploadResponse(media.getId(), urlService.imageUrl(media.getId()), media.getName());
    }

    /**
     * 幂等保存图片：相同内容只保留一条 media 记录和一个对象。
     * 种子数据导入复用该方法。
     */
    public MediaEntity storeImage(String filename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw ApiException.param("图片内容为空");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw ApiException.imageTooLarge("图片不能超过 5 MB");
        }
        ImageTypeDetector.DetectedImage detected = ImageTypeDetector.detect(bytes);
        String md5 = DigestUtils.md5DigestAsHex(bytes);
        String key = "models/" + md5 + "." + detected.extension();

        Optional<MediaEntity> existing = mediaRepository.findById(md5);
        if (existing.isPresent()) {
            MediaEntity media = existing.get();
            // 修复“数据库有记录但对象缺失”的不一致状态。
            if (media.getPath() != null && !storage.exists(media.getPath())) {
                storage.put(media.getPath(), bytes, detected.contentType());
            }
            return media;
        }

        storage.put(key, bytes, detected.contentType());

        MediaEntity media = new MediaEntity();
        media.setId(md5);
        media.setName(filename);
        media.setType("pic");
        media.setFileSize((long) bytes.length);
        media.setUrl("/api/v1/files/images/" + md5);
        media.setBucket(storage.bucketName());
        media.setPath(key);
        media.setContentType(detected.contentType());
        try {
            return mediaRepository.save(media);
        } catch (RuntimeException e) {
            // 数据库写入失败时清理刚上传的对象，避免产生孤儿文件。
            try {
                storage.delete(key);
            } catch (StorageException cleanupError) {
                log.warn("清理对象 {} 失败: {}", key, cleanupError.getMessage());
            }
            throw e;
        }
    }

    public LoadedImage loadImage(String id) {
        MediaEntity media = mediaRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("图片不存在"));
        if (!"pic".equals(media.getType())) {
            throw ApiException.notFound("图片不存在");
        }
        if (media.getPath() == null || media.getPath().isBlank()) {
            throw new StorageException("媒体记录缺少对象路径: " + id);
        }
        try (InputStream in = storage.get(media.getPath())) {
            byte[] bytes = in.readAllBytes();
            String contentType = media.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = ImageTypeDetector.detect(bytes).contentType();
            }
            return new LoadedImage(bytes, contentType, media.getName());
        } catch (IOException e) {
            throw new StorageException("读取图片失败: " + id, e);
        }
    }

    static String sanitizeFilename(String original) {
        if (original == null) {
            return "image";
        }
        String name = original.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) {
            return "image";
        }
        return name.length() > 200 ? name.substring(0, 200) : name;
    }
}
