package com.campusdoc.document;

import com.campusdoc.document.service.DocumentTextExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextExtractorTest {

  @TempDir
  Path tempDir;

  private final DocumentTextExtractor extractor = new DocumentTextExtractor();

  @Test
  void extractsXlsxWithSheetHeadersAndTabSeparatedCells() throws IOException {
    Path xlsx = tempDir.resolve("sample.xlsx");
    try (XSSFWorkbook workbook = new XSSFWorkbook();
         OutputStream out = Files.newOutputStream(xlsx)) {
      var sheet = workbook.createSheet("奖学金");
      var header = sheet.createRow(0);
      header.createCell(0).setCellValue("等级");
      header.createCell(1).setCellValue("金额");
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("一等");
      row.createCell(1).setCellValue(8000);
      workbook.write(out);
    }

    String text = extractor.extract(xlsx, "xlsx");

    assertThat(text).contains("【Sheet: 奖学金】");
    assertThat(text).contains("等级");
    assertThat(text).contains("一等");
    assertThat(text).contains("8000");
  }
}
