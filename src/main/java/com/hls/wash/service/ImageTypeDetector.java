package com.hls.wash.service;

import com.hls.wash.exception.ApiException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * 依据文件真实内容（magic bytes + 实际解码）识别图片类型，不信任客户端提交的 MIME 或扩展名。
 */
public final class ImageTypeDetector {

    public record DetectedImage(String extension, String contentType) {
    }

    private ImageTypeDetector() {
    }

    public static DetectedImage detect(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            throw ApiException.unsupportedImageType("仅支持 PNG、JPEG、WebP 图片");
        }
        if (isPng(bytes)) {
            verifyDecodable(bytes, "png");
            return new DetectedImage("png", "image/png");
        }
        if (isJpeg(bytes)) {
            verifyDecodable(bytes, "jpeg");
            return new DetectedImage("jpg", "image/jpeg");
        }
        if (isWebp(bytes)) {
            return new DetectedImage("webp", "image/webp");
        }
        throw ApiException.unsupportedImageType("仅支持 PNG、JPEG、WebP 图片");
    }

    private static boolean isPng(byte[] b) {
        return (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A;
    }

    private static boolean isJpeg(byte[] b) {
        return (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isWebp(byte[] b) {
        return b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static void verifyDecodable(byte[] bytes, String label) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw ApiException.unsupportedImageType("图片内容不是有效的 " + label.toUpperCase(Locale.ROOT));
            }
        } catch (IOException e) {
            throw ApiException.unsupportedImageType("图片内容损坏，无法解析");
        }
    }
}
