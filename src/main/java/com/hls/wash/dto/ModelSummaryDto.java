package com.hls.wash.dto;

/**
 * 模型列表项，不包含完整行数组。
 */
public record ModelSummaryDto(
        Long id,
        String code,
        String name,
        String description,
        String imageUrl,
        int rowCount,
        int tubeCount,
        int version,
        String updatedAt
) {
}
