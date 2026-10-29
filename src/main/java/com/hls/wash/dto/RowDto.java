package com.hls.wash.dto;

/**
 * 一行管子的几何参数。
 *
 * @param rowIndex  0 基行号，必须从 0 开始连续
 * @param tubeCount 本行管数
 * @param offsetX   行首 X 偏移，单位为一个管间距（可为小数）
 * @param offsetY   行首 Y 偏移，单位为一个行间距（可为小数）
 */
public record RowDto(int rowIndex, int tubeCount, double offsetX, double offsetY) {
}
