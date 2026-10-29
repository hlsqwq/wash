package com.hls.wash.service;

import com.hls.wash.dto.CreateModelRequest;
import com.hls.wash.dto.ModelDetailDto;
import com.hls.wash.dto.ModelSummaryDto;
import com.hls.wash.dto.RowDto;
import com.hls.wash.dto.SeparatorDto;
import com.hls.wash.dto.UpdateModelRequest;
import com.hls.wash.entity.ModelEntity;
import com.hls.wash.validation.ModelRequestValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 请求数据 &lt;-&gt; 实体 &lt;-&gt; DTO 的映射，集中处理 JSON 字段，避免业务层操作原始字符串。
 */
@Component
public class ModelAssembler {

    private final JsonCodec jsonCodec;
    private final UrlService urlService;
    private final ModelRequestValidator validator;

    public ModelAssembler(JsonCodec jsonCodec, UrlService urlService, ModelRequestValidator validator) {
        this.jsonCodec = jsonCodec;
        this.urlService = urlService;
        this.validator = validator;
    }

    public ModelEntity build(CreateModelRequest request, String imageId) {
        validator.validate(request.rows(), request.hideCells(), request.blockCells(), request.separators());
        ModelEntity entity = new ModelEntity();
        entity.setVersion(1);
        entity.setEnabled(true);
        apply(entity, request.code(), request.name(), request.description(), imageId,
                request.dx(), request.dy(), request.wideCenterGap(), request.rows(),
                request.hideCells(), request.blockCells(), request.separators());
        return entity;
    }

    public void applyUpdate(ModelEntity entity, UpdateModelRequest request, String imageId) {
        validator.validate(request.rows(), request.hideCells(), request.blockCells(), request.separators());
        int nextVersion = entity.getVersion() == null ? 1 : entity.getVersion() + 1;
        apply(entity, request.code(), request.name(), request.description(), imageId,
                request.dx(), request.dy(), request.wideCenterGap(), request.rows(),
                request.hideCells(), request.blockCells(), request.separators());
        entity.setVersion(nextVersion);
    }

    private void apply(ModelEntity entity, String code, String name, String description, String imageId,
                       Integer dx, Integer dy, Boolean wideCenterGap, List<RowDto> rows,
                       List<String> hideCells, List<String> blockCells, List<SeparatorDto> separators) {
        List<Integer> rowLens = rows.stream().map(RowDto::tubeCount).collect(Collectors.toList());
        List<JsonCodec.OffsetValue> offsets = rows.stream()
                .map(r -> new JsonCodec.OffsetValue(r.offsetX(), r.offsetY()))
                .collect(Collectors.toList());
        List<SeparatorDto> safeSeparators = separators == null ? List.of() : separators.stream()
                .sorted(Comparator.comparingInt(SeparatorDto::afterRow))
                .collect(Collectors.toList());
        List<String> safeHide = dedupe(hideCells);
        List<String> safeBlock = dedupe(blockCells);

        entity.setCode(code.trim());
        entity.setName(name.trim());
        entity.setDescription(description == null || description.isBlank()
                ? defaultDescription(rows.size(), rowLens.stream().mapToInt(Integer::intValue).sum())
                : description.trim());
        entity.setImageId(imageId);
        entity.setDx(dx);
        entity.setDy(dy);
        entity.setWideCenterGap(Boolean.TRUE.equals(wideCenterGap));
        entity.setTotalRow(rows.size());
        entity.setRowLen(jsonCodec.encode(rowLens));
        entity.setRowOffset(jsonCodec.encode(offsets));
        entity.setHideHoles(jsonCodec.encode(safeHide));
        entity.setBlockHoles(jsonCodec.encode(safeBlock));
        entity.setHorizonPartitionPresent(!safeSeparators.isEmpty());
        entity.setHorizonPartition(jsonCodec.encode(safeSeparators));
        // is_vertical_partition 与 wideCenterGap 语义不同，当前没有对应输入，保持 false。
        entity.setVerticalPartition(false);
    }

    private List<String> dedupe(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return new ArrayList<>(values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public ModelSummaryDto toSummary(ModelEntity entity) {
        List<Integer> rowLens = jsonCodec.readIntList(entity.getRowLen());
        int tubeCount = rowLens.stream().mapToInt(Integer::intValue).sum();
        int rowCount = entity.getTotalRow() != null ? entity.getTotalRow() : rowLens.size();
        return new ModelSummaryDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                descriptionOf(entity, rowCount, tubeCount),
                urlService.imageUrl(entity.getImageId()),
                rowCount,
                tubeCount,
                entity.getVersion(),
                iso(entity.getUpdateTime())
        );
    }

    public ModelDetailDto toDetail(ModelEntity entity) {
        List<Integer> rowLens = jsonCodec.readIntList(entity.getRowLen());
        List<JsonCodec.OffsetValue> offsets = jsonCodec.readOffsets(entity.getRowOffset());
        if (rowLens.size() != offsets.size()) {
            throw new IllegalStateException("模型 " + entity.getCode() + " 的 row_len 与 row_offset 长度不一致");
        }
        List<RowDto> rows = new ArrayList<>(rowLens.size());
        for (int i = 0; i < rowLens.size(); i++) {
            rows.add(new RowDto(i, rowLens.get(i), offsets.get(i).offsetX(), offsets.get(i).offsetY()));
        }
        int tubeCount = rowLens.stream().mapToInt(Integer::intValue).sum();
        int rowCount = entity.getTotalRow() != null ? entity.getTotalRow() : rowLens.size();
        return new ModelDetailDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                descriptionOf(entity, rowCount, tubeCount),
                urlService.imageUrl(entity.getImageId()),
                rowCount,
                tubeCount,
                entity.getVersion(),
                iso(entity.getUpdateTime()),
                entity.getDx(),
                entity.getDy(),
                entity.isWideCenterGap(),
                rows,
                jsonCodec.readStringList(entity.getHideHoles()),
                jsonCodec.readStringList(entity.getBlockHoles()),
                jsonCodec.readSeparators(entity.getHorizonPartition())
        );
    }

    private String descriptionOf(ModelEntity entity, int rowCount, int tubeCount) {
        if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
            return entity.getDescription();
        }
        return defaultDescription(rowCount, tubeCount);
    }

    public static String defaultDescription(int rowCount, int tubeCount) {
        return "共" + rowCount + "行，合计" + tubeCount + "根管子";
    }

    private String iso(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.toInstant(ZoneOffset.UTC).toString();
    }
}
