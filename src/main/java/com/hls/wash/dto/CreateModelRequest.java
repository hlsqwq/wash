package com.hls.wash.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建模型请求。rowCount / tubeCount 由服务端重新计算，客户端不得传入。
 */
public record CreateModelRequest(
        @NotBlank(message = "模型编码不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "模型编码只能包含字母、数字、下划线和中划线，长度 1-64")
        String code,

        @NotBlank(message = "模型名称不能为空")
        @Size(max = 128, message = "模型名称不能超过 128 个字符")
        String name,

        @Size(max = 255, message = "模型说明不能超过 255 个字符")
        String description,

        @Size(max = 1024, message = "图片地址过长")
        String imageUrl,

        @NotNull(message = "dx 不能为空")
        @Min(value = 1, message = "dx 必须大于 0")
        @Max(value = 1000000, message = "dx 超出合理范围")
        Integer dx,

        @NotNull(message = "dy 不能为空")
        @Min(value = 1, message = "dy 必须大于 0")
        @Max(value = 1000000, message = "dy 超出合理范围")
        Integer dy,

        Boolean wideCenterGap,

        @NotEmpty(message = "rows 不能为空")
        @Size(max = 300, message = "行数不能超过 300")
        List<RowDto> rows,

        List<String> hideCells,

        List<String> blockCells,

        List<SeparatorDto> separators
) {
}
