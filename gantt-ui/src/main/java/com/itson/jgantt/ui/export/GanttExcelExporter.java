package com.itson.jgantt.ui.export;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.ui.util.Messages;

public final class GanttExcelExporter {

	private static final Color HEADER_COLOR = new Color(0x24, 0x56, 0xA6);
	private static final Color BAND_COLOR = new Color(0xEF, 0xF3, 0xFA);
	private static final String DATE_FORMAT = "yyyy-mm-dd";

	private GanttExcelExporter() {
		// ...
	}

	public static void export(ProjectDto project, Path target) throws IOException {
		try (OutputStream out = Files.newOutputStream(target); XSSFWorkbook workbook = new XSSFWorkbook()) {
			writeTaskSheet(workbook, project);
			writeCalendarSheet(workbook, project.nonWorkingDays());
			workbook.write(out);
		}
	}

	private static void writeTaskSheet(XSSFWorkbook workbook, ProjectDto project) {
		XSSFSheet sheet = workbook.createSheet(Messages.get("export.sheet.tasks"));
		XSSFCellStyle headerStyle = headerStyle(workbook);
		XSSFCellStyle bodyStyle = bodyStyle(workbook);
		XSSFCellStyle bandStyle = bandStyle(workbook);
		XSSFCellStyle centerStyle = centerStyle(workbook);
		XSSFCellStyle bandCenterStyle = bandStyle(workbook);
		bandCenterStyle.setAlignment(HorizontalAlignment.CENTER);
		XSSFCellStyle dateStyle = dateStyle(workbook);
		XSSFCellStyle bandDateStyle = bandStyle(workbook);
		bandDateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(DATE_FORMAT));
		XSSFCellStyle percentStyle = percentStyle(workbook);
		XSSFCellStyle bandPercentStyle = bandStyle(workbook);
		bandPercentStyle.setAlignment(HorizontalAlignment.CENTER);
		bandPercentStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0%"));

		String[] headers = {
			Messages.get("column.name"),
			Messages.get("column.start"),
			Messages.get("column.end"),
			Messages.get("column.length"),
			Messages.get("column.progress"),
			Messages.get("column.milestone")};
		XSSFRow header = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			XSSFCell cell = header.createCell(i);
			cell.setCellValue(headers[i]);
			cell.setCellStyle(headerStyle);
		}

		List<TaskDto> tasks = project.tasks();
		for (int i = 0; i < tasks.size(); i++) {
			TaskDto task = tasks.get(i);
			boolean band = i % 2 == 1;
			XSSFRow row = sheet.createRow(i + 1);
			StringBuilder name = new StringBuilder();
			name.append("  ".repeat(Math.max(0, task.outlineLevel() - 1)));
			name.append(task.name());
			XSSFCell nameCell = row.createCell(0);
			nameCell.setCellValue(name.toString());
			nameCell.setCellStyle(band ? bandStyle : bodyStyle);

			XSSFCell start = row.createCell(1);
			start.setCellValue(task.start());
			start.setCellStyle(band ? bandDateStyle : dateStyle);

			XSSFCell end = row.createCell(2);
			end.setCellValue(task.end());
			end.setCellStyle(band ? bandDateStyle : dateStyle);

			XSSFCell len = row.createCell(3);
			len.setCellValue(ChronoUnit.DAYS.between(task.start(), task.end()) + 1);
			len.setCellStyle(band ? bandCenterStyle : centerStyle);

			XSSFCell progress = row.createCell(4);
			progress.setCellValue(task.progress() / 100.0);
			progress.setCellStyle(band ? bandPercentStyle : percentStyle);

			XSSFCell milestone = row.createCell(5);
			milestone.setCellValue(task.milestone() ? Messages.get("export.yes") : Messages.get("export.no"));
			milestone.setCellStyle(band ? bandCenterStyle : centerStyle);
		}

		sheet.createFreezePane(0, 1);
		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
		}
		sheet.setColumnWidth(0, Math.min(sheet.getColumnWidth(0) + 800, 200 * 256));
	}

	private static void writeCalendarSheet(XSSFWorkbook workbook, List<LocalDate> dates) {
		XSSFSheet sheet = workbook.createSheet(Messages.get("export.sheet.calendar"));
		XSSFCellStyle headerStyle = headerStyle(workbook);
		XSSFCellStyle dateStyle = dateStyle(workbook);
		XSSFRow header = sheet.createRow(0);
		XSSFCell cell = header.createCell(0);
		cell.setCellValue(Messages.get("dialog.nonWorkingDays.date"));
		cell.setCellStyle(headerStyle);
		int rowIndex = 1;
		for (LocalDate date : dates) {
			XSSFRow row = sheet.createRow(rowIndex++);
			XSSFCell valueCell = row.createCell(0);
			valueCell.setCellValue(date);
			valueCell.setCellStyle(dateStyle);
		}
		sheet.autoSizeColumn(0);
	}

	private static XSSFCellStyle headerStyle(XSSFWorkbook workbook) {
		XSSFCellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setColor(new XSSFColor(HEADER_COLOR, new DefaultIndexedColorMap()));
		style.setFont(font);
		style.setFillForegroundColor(new XSSFColor(HEADER_COLOR, new DefaultIndexedColorMap()));
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(HorizontalAlignment.LEFT);
		style.setWrapText(true);
		return style;
	}

	private static XSSFCellStyle dateStyle(XSSFWorkbook workbook) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(DATE_FORMAT));
		return style;
	}

	private static XSSFCellStyle bodyStyle(XSSFWorkbook workbook) {
		return workbook.createCellStyle();
	}

	private static XSSFCellStyle bandStyle(XSSFWorkbook workbook) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFillForegroundColor(new XSSFColor(BAND_COLOR, new DefaultIndexedColorMap()));
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}

	private static XSSFCellStyle centerStyle(XSSFWorkbook workbook) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		return style;
	}

	private static XSSFCellStyle percentStyle(XSSFWorkbook workbook) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("0%"));
		return style;
	}

}
