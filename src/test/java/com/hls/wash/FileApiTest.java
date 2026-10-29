package com.hls.wash;

import com.hls.wash.entity.MediaEntity;
import com.hls.wash.entity.ModelEntity;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileApiTest extends ApiTestSupport {

    private final AtomicReference<byte[]> storedBytes = new AtomicReference<>();

    @BeforeEach
    void stubStorage() {
        storedBytes.set(null);
        doNothing().when(objectStorageService).put(anyString(), any(byte[].class), anyString());
        when(objectStorageService.exists(anyString())).thenReturn(true);
        when(objectStorageService.bucketName()).thenReturn("wash-model-images");
        when(objectStorageService.get(anyString())).thenAnswer(invocation ->
                new ByteArrayInputStream(storedBytes.get() == null ? new byte[0] : storedBytes.get()));
    }

    @Test
    void uploadPngStoresMediaAndServesImage() throws Exception {
        byte[] png = samplePng();
        MockMultipartFile file = new MockMultipartFile("file", "model.png", "image/png", png);

        String body = mockMvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.url").value(
                        org.hamcrest.Matchers.endsWith("/api/v1/files/images/" + md5(png))))
                .andExpect(jsonPath("$.data.filename").value("model.png"))
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(body, "$.data.id");
        MediaEntity media = mediaRepository.findById(id).orElseThrow();
        assertThat(media.getType()).isEqualTo("pic");
        assertThat(media.getBucket()).isEqualTo("wash-model-images");
        assertThat(media.getPath()).startsWith("models/").endsWith(".png");
        assertThat(media.getFileSize()).isEqualTo(png.length);

        storedBytes.set(png);
        mockMvc.perform(get("/api/v1/files/images/" + id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
    }

    @Test
    void duplicateUploadReusesSameMediaRecord() throws Exception {
        byte[] png = samplePng();
        MockMultipartFile first = new MockMultipartFile("file", "a.png", "image/png", png);
        MockMultipartFile second = new MockMultipartFile("file", "b.png", "image/png", png);

        String body = mockMvc.perform(multipart("/api/v1/files/images").file(first))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String firstId = JsonPath.read(body, "$.data.id");

        String secondBody = mockMvc.perform(multipart("/api/v1/files/images").file(second))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String secondId = JsonPath.read(secondBody, "$.data.id");

        assertThat(secondId).isEqualTo(firstId);
        assertThat(mediaRepository.count()).isEqualTo(1);
        // 第一次上传写入对象；第二次命中已有记录，不再重复上传。
        verify(objectStorageService, times(1)).put(anyString(), any(byte[].class), anyString());
    }

    @Test
    void unsupportedImageTypeIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.svg",
                "image/svg+xml", "<svg>not allowed</svg>".getBytes());
        mockMvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(41501));
        assertThat(mediaRepository.count()).isZero();
    }

    @Test
    void oversizedImageIsRejected() throws Exception {
        byte[] pngHeader = new byte[]{ (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
        byte[] tooLarge = new byte[(int) (5L * 1024 * 1024) + 1];
        System.arraycopy(pngHeader, 0, tooLarge, 0, pngHeader.length);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooLarge);

        mockMvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value(41301));
        assertThat(mediaRepository.count()).isZero();
    }

    @Test
    void createModelAssociatesUploadedImage() throws Exception {
        byte[] png = samplePng();
        MockMultipartFile file = new MockMultipartFile("file", "model.png", "image/png", png);
        String body = mockMvc.perform(multipart("/api/v1/files/images").file(file))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String imageId = JsonPath.read(body, "$.data.id");
        String imageUrl = JsonPath.read(body, "$.data.url");

        var request = modelRequest("img");
        request.put("imageUrl", imageUrl);

        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value(imageUrl));

        ModelEntity entity = modelRepository.findByCode("img").orElseThrow();
        assertThat(entity.getImageId()).isEqualTo(imageId);
        assertThat(mediaRepository.existsById(imageId)).isTrue();
    }

    @Test
    void createModelWithUnknownImageIsRejected() throws Exception {
        var request = modelRequest("bad-img");
        request.put("imageUrl", "http://test.local/api/v1/files/images/00000000000000000000000000000000");
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    private String md5(byte[] bytes) {
        return org.springframework.util.DigestUtils.md5DigestAsHex(bytes);
    }
}
