package com.hls.wash;

import com.hls.wash.entity.ModelEntity;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ModelApiTest extends ApiTestSupport {

    @Test
    void listIsEmptyInitially() throws Exception {
        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void createReturnsDetailWithServerComputedCounts() throws Exception {
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(modelRequest("e999"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value("e999"))
                .andExpect(jsonPath("$.data.rowCount").value(3))
                .andExpect(jsonPath("$.data.tubeCount").value(12))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.rows.length()").value(3))
                .andExpect(jsonPath("$.data.rows[1].tubeCount").value(4))
                .andExpect(jsonPath("$.data.rows[1].offsetX").value(0.5))
                .andExpect(jsonPath("$.data.rows[1].offsetY").value(1.0))
                .andExpect(jsonPath("$.data.hideCells[0]").value("c_0_0"))
                .andExpect(jsonPath("$.data.blockCells[0]").value("c_1_1"))
                .andExpect(jsonPath("$.data.separators[0].afterRow").value(1))
                .andExpect(jsonPath("$.data.separators[0].heightDp").value(12))
                .andExpect(jsonPath("$.data.description").value("共3行，合计12根管子"));

        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("e999"))
                .andExpect(jsonPath("$.data[0].tubeCount").value(12));
    }

    @Test
    void detailOfUnknownModelReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/models/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void duplicateCodeReturns409() throws Exception {
        createModel("dup");
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(modelRequest("dup"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    void invalidRowArrayIsRejected() throws Exception {
        Map<String, Object> request = modelRequest("bad-row");
        request.put("rows", List.of(row(0, 0, 0, 0)));
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void nonContinuousRowIndexIsRejected() throws Exception {
        Map<String, Object> request = modelRequest("bad-index");
        request.put("rows", List.of(row(0, 3, 0, 0), row(2, 4, 0, 1)));
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void outOfRangeCellIsRejected() throws Exception {
        Map<String, Object> request = modelRequest("bad-cell");
        request.put("hideCells", List.of("c_0_99"));
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void invalidCodeIsRejected() throws Exception {
        Map<String, Object> request = modelRequest("bad code!");
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void updateIncrementsVersionAndReplacesRows() throws Exception {
        long id = createModel("upd");

        Map<String, Object> update = modelRequest("upd");
        update.put("name", "更新后的模型");
        update.put("rows", List.of(row(0, 2, 0.0, 0.0), row(1, 5, 1.0, 1.0)));
        update.put("hideCells", List.of());
        update.put("blockCells", List.of());
        update.put("separators", List.of());

        mockMvc.perform(put("/api/v1/models/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.name").value("更新后的模型"))
                .andExpect(jsonPath("$.data.rowCount").value(2))
                .andExpect(jsonPath("$.data.tubeCount").value(7));

        ModelEntity entity = modelRepository.findById(id).orElseThrow();
        assertThat(entity.getVersion()).isEqualTo(2);
        assertThat(entity.getTotalRow()).isEqualTo(2);
    }

    @Test
    void deleteHidesModelFromListAndDetail() throws Exception {
        long id = createModel("del");

        mockMvc.perform(delete("/api/v1/models/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/models/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        ModelEntity entity = modelRepository.findById(id).orElseThrow();
        assertThat(entity.isEnabled()).isFalse();
    }

    @Test
    void separatorOutOfRangeIsRejected() throws Exception {
        Map<String, Object> request = modelRequest("bad-sep");
        request.put("separators", List.of(Map.of("afterRow", 99, "heightDp", 10)));
        mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    private long createModel(String code) throws Exception {
        String body = mockMvc.perform(post("/api/v1/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(modelRequest(code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(body, "$.data.id");
        return id.longValue();
    }
}
