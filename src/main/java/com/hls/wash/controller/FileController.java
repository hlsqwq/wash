package com.hls.wash.controller;

import com.hls.wash.dto.ApiResponse;
import com.hls.wash.dto.ImageUploadResponse;
import com.hls.wash.service.MediaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 图片上传与代理读取。客户端不接触 RustFS 地址与凭证。
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final MediaService mediaService;

    public FileController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(mediaService.uploadImage(file));
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> image(@PathVariable String id) {
        MediaService.LoadedImage image = mediaService.loadImage(id);
        String filename = image.filename() == null || image.filename().isBlank() ? "image" : image.filename();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .eTag("\"" + id + "\"")
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(image.bytes());
    }
}
