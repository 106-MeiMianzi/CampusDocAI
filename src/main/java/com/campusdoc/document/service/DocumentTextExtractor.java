package com.campusdoc.document.service;

import com.campusdoc.common.BusinessException;
import com.campusdoc.common.ErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocumentTextExtractor {

    public String extract(Path filePath, String extension) {
        try {
            return switch (extension.toLowerCase()) {
                case "pdf" -> extractPdf(filePath);
                case "docx" -> extractDocx(filePath);
                case "xlsx" -> extractXlsx(filePath);
                default -> throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
            };
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARSE_FAILED, "文档解析失败: " + e.getMessage());
        }
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractXlsx(Path path) throws IOException {
        DataFormatter formatter = new DataFormatter();
        StringBuilder sb = new StringBuilder();
        try (InputStream in = Files.newInputStream(path);
             Workbook workbook = new XSSFWorkbook(in)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null) {
                    continue;
                }
                sb.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                for (Row row : sheet) {
                    if (row == null) {
                        continue;
                    }
                    int lastCell = row.getLastCellNum();
                    for (int c = 0; c < lastCell; c++) {
                        if (c > 0) {
                            sb.append('\t');
                        }
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (cell != null) {
                            sb.append(formatter.formatCellValue(cell));
                        }
                    }
                    sb.append('\n');
                }
                sb.append('\n');
            }
        }
        return sb.toString().trim();
    }
}
