package com.hls.wash.dto;

/**
 * 横向隔断：在第 afterRow 行之后插入一条 heightDp 高的分隔行（0 基行号）。
 */
public record SeparatorDto(int afterRow, int heightDp) {
}
