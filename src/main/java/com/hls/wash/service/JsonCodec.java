package com.hls.wash.service;

import com.hls.wash.dto.SeparatorDto;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库 JSON/TEXT 字段与强类型结构之间的转换。
 * 业务层只接触 List&lt;Integer&gt;、OffsetValue 等类型，不直接操作原始 JSON 字符串。
 */
@Component
public class JsonCodec {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    public record OffsetValue(double offsetX, double offsetY) {
    }

    public String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (RuntimeException e) {
            throw new IllegalStateException("序列化模型数据失败", e);
        }
    }

    public List<Integer> readIntList(String json) {
        return read(json, new TypeReference<List<Integer>>() {
        });
    }

    public List<String> readStringList(String json) {
        return read(json, new TypeReference<List<String>>() {
        });
    }

    public List<OffsetValue> readOffsets(String json) {
        return read(json, new TypeReference<List<OffsetValue>>() {
        });
    }

    public List<SeparatorDto> readSeparators(String json) {
        return read(json, new TypeReference<List<SeparatorDto>>() {
        });
    }

    private <T> List<T> read(String json, TypeReference<List<T>> type) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<T> parsed = mapper.readValue(json, type);
            return parsed == null ? new ArrayList<>() : parsed;
        } catch (RuntimeException e) {
            throw new IllegalStateException("数据库中的模型 JSON 数据格式不正确", e);
        }
    }
}
