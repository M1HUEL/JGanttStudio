package com.itson.jgantt.ui.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.ProjectId;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.domain.valueobject.TaskType;
import com.lowagie.text.pdf.PdfReader;

class ExportersTest {

	@TempDir
	Path tempDir;

	@Test
	void excelExportProducesWorkbook() throws IOException {
		Path file = tempDir.resolve("demo.xlsx");
		GanttExcelExporter.export(sampleProject(), file);

		assertTrue(Files.size(file) > 0);
		try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
			new org.apache.poi.xssf.usermodel.XSSFWorkbook(Files.newInputStream(file))) {
			assertEquals(2, workbook.getNumberOfSheets());
			assertEquals(3, workbook.getSheetAt(0).getLastRowNum() + 1);
			assertEquals("Alpha", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
			assertEquals("Beta", workbook.getSheetAt(0).getRow(2).getCell(0).getStringCellValue());
		}
	}

	@Test
	void pdfExportProducesDocument() throws IOException {
		Path file = tempDir.resolve("demo.pdf");
		BufferedImage chart = new BufferedImage(600, 300, BufferedImage.TYPE_INT_RGB);
		GanttPdfExporter.export(sampleProject(), chart, file);

		assertTrue(Files.size(file) > 0);
		try (PdfReader reader = new PdfReader(Files.readAllBytes(file))) {
			assertEquals(2, reader.getNumberOfPages());
		}
	}

	private static ProjectDto sampleProject() {
		ProjectId projectId = ProjectId.random();
		TaskDto alpha = new TaskDto(TaskId.random(), "Alpha",
			LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 9), TaskType.TASK, 50,
			null, 1, false);
		TaskDto beta = new TaskDto(TaskId.random(), "Beta",
			LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 13), TaskType.TASK, 0,
			null, 1, true);
		return new ProjectDto(projectId, "Demo", List.of(alpha, beta), List.of(),
			List.of(LocalDate.of(2026, 1, 6)));
	}
}