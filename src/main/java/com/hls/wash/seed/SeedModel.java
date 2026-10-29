package com.hls.wash.seed;

import com.hls.wash.dto.SeparatorDto;

import java.util.List;

/**
 * 种子数据文件 seed/models.json 的结构。
 */
public record SeedModel(
        String code,
        String name,
        int dx,
        int dy,
        boolean wideCenterGap,
        List<SeedRow> rows,
        List<String> hideCells,
        List<String> blockCells,
        List<SeparatorDto> separators,
        String imageFile,
        String sourceFile
) {
    public record SeedRow(int tubeCount, double offsetX, double offsetY) {
    }
}
