package com.hls.wash;

import com.hls.wash.repository.MediaRepository;
import com.hls.wash.repository.ModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected MediaRepository mediaRepository;

    /**
     * 测试替身：不连接真实 RustFS。真实存储验证在联调阶段单独执行。
     */
    @MockitoBean
    protected com.hls.wash.storage.ObjectStorageService objectStorageService;

    protected final ObjectMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void cleanDatabase() {
        modelRepository.deleteAll();
        mediaRepository.deleteAll();
    }

    protected String json(Object value) {
        return mapper.writeValueAsString(value);
    }

    protected Map<String, Object> modelRequest(String code) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("name", code.toUpperCase() + " 测试模型");
        request.put("description", "");
        request.put("imageUrl", null);
        request.put("dx", 1730);
        request.put("dy", 1488);
        request.put("wideCenterGap", false);
        request.put("rows", List.of(
                row(0, 3, 0.0, 0.0),
                row(1, 4, 0.5, 1.0),
                row(2, 5, 0.0, 2.0)
        ));
        request.put("hideCells", List.of("c_0_0"));
        request.put("blockCells", List.of("c_1_1"));
        request.put("separators", List.of(Map.of("afterRow", 1, "heightDp", 12)));
        return request;
    }

    protected Map<String, Object> row(int index, int tubes, double offsetX, double offsetY) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rowIndex", index);
        row.put("tubeCount", tubes);
        row.put("offsetX", offsetX);
        row.put("offsetY", offsetY);
        return row;
    }

    protected byte[] samplePng() {
        try {
            BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
