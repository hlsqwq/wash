package com.hls.wash.validation;

import com.hls.wash.dto.RowDto;
import com.hls.wash.dto.SeparatorDto;
import com.hls.wash.exception.ApiException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模型几何数据的服务端校验。统计值（行数/管数）永远由服务端重新计算。
 */
@Component
public class ModelRequestValidator {

    private static final Pattern CELL = Pattern.compile("^c_(\\d{1,9})_(\\d{1,9})$");
    private static final double MAX_OFFSET = 1000.0;

    public void validate(List<RowDto> rows, List<String> hideCells,
                         List<String> blockCells, List<SeparatorDto> separators) {
        if (rows == null || rows.isEmpty()) {
            throw ApiException.param("rows 不能为空");
        }
        if (rows.size() > 300) {
            throw ApiException.param("行数不能超过 300");
        }
        for (int i = 0; i < rows.size(); i++) {
            RowDto row = rows.get(i);
            if (row == null) {
                throw ApiException.param("第 " + i + " 行数据缺失");
            }
            if (row.rowIndex() != i) {
                throw ApiException.param("rows.rowIndex 必须从 0 开始连续递增，第 " + i + " 行实际为 " + row.rowIndex());
            }
            if (row.tubeCount() < 1 || row.tubeCount() > 200) {
                throw ApiException.param("第 " + i + " 行管数必须在 1-200 之间");
            }
            if (!Double.isFinite(row.offsetX()) || !Double.isFinite(row.offsetY())) {
                throw ApiException.param("第 " + i + " 行偏移必须是有限数值");
            }
            if (Math.abs(row.offsetX()) > MAX_OFFSET || Math.abs(row.offsetY()) > MAX_OFFSET) {
                throw ApiException.param("第 " + i + " 行偏移超出合理范围（±" + MAX_OFFSET + "）");
            }
        }

        validateCells(hideCells, "hideCells", rows);
        validateCells(blockCells, "blockCells", rows);

        if (separators != null) {
            for (SeparatorDto separator : separators) {
                if (separator == null) {
                    throw ApiException.param("separators 中存在空值");
                }
                if (separator.afterRow() < 0 || separator.afterRow() >= rows.size()) {
                    throw ApiException.param("separators.afterRow 超出有效行范围: " + separator.afterRow());
                }
                if (separator.heightDp() <= 0 || separator.heightDp() > 500) {
                    throw ApiException.param("separators.heightDp 必须在 1-500 之间");
                }
            }
        }
    }

    private void validateCells(List<String> cells, String field, List<RowDto> rows) {
        if (cells == null) {
            return;
        }
        for (String cell : cells) {
            if (cell == null || cell.isBlank()) {
                throw ApiException.param(field + " 中存在空孔位");
            }
            Matcher matcher = CELL.matcher(cell.trim());
            if (!matcher.matches()) {
                throw ApiException.param(field + " 中的孔位格式必须为 c_<行>_<列>: " + cell);
            }
            int row;
            int col;
            try {
                row = Integer.parseInt(matcher.group(1));
                col = Integer.parseInt(matcher.group(2));
            } catch (NumberFormatException e) {
                throw ApiException.param(field + " 中的孔位越界: " + cell);
            }
            if (row >= rows.size()) {
                throw ApiException.param(field + " 中的孔位行号越界: " + cell);
            }
            if (col >= rows.get(row).tubeCount()) {
                throw ApiException.param(field + " 中的孔位列号越界: " + cell);
            }
        }
    }
}
