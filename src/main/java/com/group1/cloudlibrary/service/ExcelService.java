package com.group1.cloudlibrary.service;

import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.dto.BookSaveDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 导入导出服务（图书批量导入模板）
 *
 * <p>解析时不再写死列序号，而是先读表头确认「书名 / 作者 / 平台 / 是否完结 / 出版社 / 价格 / 简介 / 库存」
 * 各自在第几列，再<b>逐行</b>按这个列映射取值，所以 Excel 里列的顺序不同、多加了空列也能正确导入；
 * 单元格无论是文本、数字还是公式都能取到值。库存列没填时按默认库存 10 处理。</p>
 */
@Slf4j
@Service
public class ExcelService {

    private static final String[] HEADERS = {"书名*", "作者*", "平台", "是否完结(0连载中/1已完结)", "出版社", "价格", "简介", "库存"};

    /** 表头名 → 内部字段名的各种写法，统一取第一个遇到的列 */
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
            "title", List.of("书名", "图书名称", "标题"),
            "author", List.of("作者"),
            "platform", List.of("平台"),
            "finished", List.of("是否完结", "完结状态", "状态"),
            "publisher", List.of("出版社"),
            "price", List.of("价格", "定价", "售价"),
            "description", List.of("简介", "描述", "图书简介"),
            "stock", List.of("库存", "库存数量", "数量", "馆藏数量", "可借数量")
    );

    /** 最多在前几行里找表头行 */
    private static final int MAX_HEADER_SCAN_ROWS = 10;

    /**
     * 写出图书导入模板
     */
    public void writeBookTemplate(OutputStream outputStream) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("图书导入模板");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                sheet.setColumnWidth(i, i == 6 ? 60 * 256 : 20 * 256);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("三体");
            sample.createCell(1).setCellValue("刘慈欣");
            sample.createCell(2).setCellValue("起点");
            sample.createCell(3).setCellValue("1");
            sample.createCell(4).setCellValue("重庆出版社");
            sample.createCell(5).setCellValue("45.00");
            sample.createCell(6).setCellValue("地球文明与三体文明的信息交流、生死搏杀及两个文明在宇宙中的兴衰历程。");
            // 示例库存：这一列不填也可以，导入时按默认库存处理
            sample.createCell(7).setCellValue("10");
            workbook.write(outputStream);
        } catch (Exception e) {
            log.error("生成导入模板失败", e);
            throw BusinessException.of("生成导入模板失败：" + e.getMessage());
        }
    }

    /**
     * 解析上传的图书 Excel
     *
     * @param inputStream Excel 文件流（.xlsx / .xls 均可）
     * @return 逐行解析出的图书列表，出版社、价格等字段每一行都会读取
     */
    public List<BookSaveDTO> parseBooks(InputStream inputStream) {
        List<BookSaveDTO> list = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        // WorkbookFactory 同时支持 .xlsx 和 .xls，避免老格式文件直接报错
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 0) {
                throw BusinessException.of("Excel 内容为空");
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 1) 找到表头行，并确定每个字段所在的列序号
            Map<String, Integer> columnMap = new HashMap<>();
            int dataStartRow = 1;
            for (int i = 0; i <= Math.min(MAX_HEADER_SCAN_ROWS, sheet.getLastRowNum()); i++) {
                Map<String, Integer> found = resolveColumns(sheet.getRow(i), formatter, evaluator);
                if (found.containsKey("title")) {
                    columnMap = found;
                    dataStartRow = i + 1;
                    break;
                }
            }
            // 没找到表头（无表头的极简表格）：退回模板默认列序
            if (columnMap.isEmpty()) {
                log.warn("未在 Excel 前 {} 行内识别到表头，按模板默认列序解析", MAX_HEADER_SCAN_ROWS + 1);
                columnMap = new HashMap<>();
                columnMap.put("title", 0);
                columnMap.put("author", 1);
                columnMap.put("platform", 2);
                columnMap.put("finished", 3);
                columnMap.put("publisher", 4);
                columnMap.put("price", 5);
                columnMap.put("description", 6);
                columnMap.put("stock", 7);
                // 第一行若看着不像标题（比如直接就是书名），就把它当数据行读进来，别整行丢掉
                String first = cellText(sheet.getRow(0), 0, formatter, evaluator);
                if (first != null && !first.contains("书名") && !first.contains("图书")) {
                    dataStartRow = 0;
                }
            }

            // 2) 从表头下一行开始，每一行都完整读取一遍
            for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                BookSaveDTO dto = toBook(row, columnMap, formatter, evaluator);
                if (dto != null) {
                    list.add(dto);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 Excel 失败", e);
            throw BusinessException.of("Excel 解析失败，请使用系统提供的模板：" + e.getMessage());
        }
        if (list.isEmpty()) {
            throw BusinessException.of("未解析到有效数据，请检查 Excel 内容");
        }
        return list;
    }

    /**
     * 把一行数据转成 DTO，书名或作者为空的行视为无效行跳过
     */
    private BookSaveDTO toBook(Row row, Map<String, Integer> columnMap,
                               DataFormatter formatter, FormulaEvaluator evaluator) {
        String title = cellText(row, columnMap.get("title"), formatter, evaluator);
        String author = cellText(row, columnMap.get("author"), formatter, evaluator);
        if (title == null || title.isBlank() || author == null || author.isBlank()) {
            return null;
        }
        BookSaveDTO dto = new BookSaveDTO();
        dto.setTitle(normalize(title));
        dto.setAuthor(normalize(author));
        dto.setPlatform(cellText(row, columnMap.get("platform"), formatter, evaluator));
        dto.setIsFinished(parseFinished(cellText(row, columnMap.get("finished"), formatter, evaluator)));
        dto.setPublisher(cellText(row, columnMap.get("publisher"), formatter, evaluator));
        dto.setPrice(parsePrice(cellText(row, columnMap.get("price"), formatter, evaluator)));
        dto.setDescription(cellText(row, columnMap.get("description"), formatter, evaluator));
        // 库存：没填 / 填了非数字 → 用默认库存
        dto.setStock(parseStock(cellText(row, columnMap.get("stock"), formatter, evaluator)));
        return dto;
    }

    /**
     * 读取某一行的某一列，取不到值返回 null
     */
    private String cellText(Row row, Integer columnIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null || columnIndex == null || columnIndex < 0) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        return normalize(cellValue(cell, formatter, evaluator));
    }

    /**
     * 单元格取值：兼容文本、数字、布尔、公式以及空白 / 错误单元格
     */
    private String cellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            CellValue value = evaluator.evaluate(cell);
            if (value == null) {
                return null;
            }
            switch (value.getCellType()) {
                case NUMERIC:
                    return plainNumber(value.getNumberValue());
                case STRING:
                    return value.getStringValue();
                case BOOLEAN:
                    return value.getBooleanValue() ? "1" : "0";
                default:
                    return null;
            }
        }
        switch (type) {
            case NUMERIC:
                return plainNumber(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue() ? "1" : "0";
            case BLANK:
            case ERROR:
            case _NONE:
            default:
                return null;
        }
    }

    /**
     * 数字原样输出，避免 POI 默认的科学计数法与多余的 0（45.0 → 45、45.50 → 45.5）
     */
    private String plainNumber(double value) {
        String text = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        return "0".equals(text) ? "0" : text;
    }

    /**
     * 价格解析：兼容「45.00」「¥45.00」「￥ 45 元」「1,299」等写法，解析不出来返回 null
     */
    private BigDecimal parsePrice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String cleaned = text.replaceAll("[¥￥$,\\\\s元]", "");
        if (cleaned.isBlank()) {
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(cleaned);
            return price.signum() < 0 ? null : price;
        } catch (NumberFormatException e) {
            log.warn("无法识别的价格：{}", text);
            return null;
        }
    }

    /**
     * 库存解析：兼容「10」「10 本」「10.0」等写法；空着或解析不出来 → 默认 10，负数按 0 处理
     */
    private int parseStock(String text) {
        if (text == null || text.isBlank()) {
            return BookSaveDTO.DEFAULT_STOCK;
        }
        String cleaned = text.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isBlank()) {
            return BookSaveDTO.DEFAULT_STOCK;
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            return Math.max(value.intValue(), 0);
        } catch (NumberFormatException e) {
            log.warn("无法识别的库存：{}", text);
            return BookSaveDTO.DEFAULT_STOCK;
        }
    }

    /**
     * 完结状态：1 / 是 / true / 完结 / 已完结 都算完结
     */
    private int parseFinished(String text) {
        if (text == null) {
            return 0;
        }
        String value = text.trim();
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "是".equals(value)
                || value.contains("完结") ? 1 : 0;
    }

    /**
     * 从表头行解析出每个字段对应的列序号
     */
    private Map<String, Integer> resolveColumns(Row headerRow, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (headerRow == null) {
            return Collections.emptyMap();
        }
        Map<String, Integer> header = new HashMap<>();
        int lastCell = headerRow.getLastCellNum();
        for (int i = 0; i < lastCell; i++) {
            String name = normalize(cellValue(headerRow.getCell(i), formatter, evaluator));
            if (name == null || name.isBlank()) {
                continue;
            }
            String field = matchField(name);
            if (field != null) {
                header.putIfAbsent(field, i);
            }
        }
        return header;
    }

    /**
     * 表头名 → 字段名：忽略空格、星号、括号里的补充说明
     */
    private String matchField(String headerName) {
        String name = headerName.replaceAll("[*＊\\s]", "");
        int bracket = name.indexOf('(');
        if (bracket < 0) {
            bracket = name.indexOf('（');
        }
        if (bracket >= 0) {
            name = name.substring(0, bracket);
        }
        for (Map.Entry<String, List<String>> entry : HEADER_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (alias.equalsIgnoreCase(name)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 去掉首尾空格，空字符串统一转成 null
     */
    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
