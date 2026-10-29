package com.hls.wash.dto;

import java.util.List;

/**
 * 模型详情，Android 端据此动态构造并渲染盘面。
 */
public record ModelDetailDto(
        Long id,
        String code,
        String name,
        String description,
        String imageUrl,
        int rowCount,
        int tubeCount,
        int version,
        String updatedAt,
        int dx,
        int dy,
        boolean wideCenterGap,
        List<RowDto> rows,
        List<String> hideCells,
        List<String> blockCells,
        List<SeparatorDto> separators
) {
}
