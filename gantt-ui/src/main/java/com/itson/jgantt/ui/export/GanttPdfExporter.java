package com.itson.jgantt.ui.export;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.imageio.ImageIO;

import com.itson.jgantt.app.dto.ProjectDto;
import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.ui.util.Messages;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public final class GanttPdfExporter {

	private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD);
	private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 11, Font.ITALIC);
	private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
	private static final Font BODY_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
	private static final float MARGIN = 42;

	private GanttPdfExporter() {
		// ...
	}

	public static void export(ProjectDto project, BufferedImage chart, Path target) throws IOException {
		try (OutputStream out = Files.newOutputStream(target)) {
			Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
			PdfWriter.getInstance(document, out);
			document.open();
			try {
				writeTitle(document, project);
				writeTaskTable(document, project.tasks());
				if (chart != null) {
					writeChartPage(document, chart);
				}
			} finally {
				document.close();
			}
		} catch (DocumentException ex) {
			throw new IOException("PDF generation failed", ex);
		}
	}

	private static void writeTitle(Document document, ProjectDto project) throws DocumentException {
		document.add(new Paragraph(project.name(), TITLE_FONT));
		LocalDate min = project.tasks().stream().map(TaskDto::start)
			.min(LocalDate::compareTo).orElse(null);
		LocalDate max = project.tasks().stream().map(TaskDto::end)
			.max(LocalDate::compareTo).orElse(null);
		if (min != null && max != null) {
			Paragraph subtitle = new Paragraph(min + " .. " + max, SUBTITLE_FONT);
			subtitle.setSpacingAfter(18);
			document.add(subtitle);
		}
	}

	private static void writeTaskTable(Document document, List<TaskDto> tasks) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[]{36, 13, 13, 12, 13, 13});
		table.setWidthPercentage(100);
		table.setSpacingBefore(6);
		String[] headers = {
			Messages.get("column.name"),
			Messages.get("column.start"),
			Messages.get("column.end"),
			Messages.get("column.length"),
			Messages.get("column.progress"),
			Messages.get("column.milestone")};
		for (String header : headers) {
			PdfPCell cell = new PdfPCell(new Paragraph(header, HEADER_FONT));
			cell.setBackgroundColor(new java.awt.Color(0x24, 0x56, 0xA6));
			cell.setBorderColor(java.awt.Color.WHITE);
			cell.setPadding(5);
			table.addCell(cell);
		}

		for (TaskDto task : tasks) {
			table.addCell(cell(indent(task)));
			table.addCell(cell(task.start().toString()));
			table.addCell(cell(task.end().toString()));
			table.addCell(cell(String.valueOf(ChronoUnit.DAYS.between(task.start(), task.end()) + 1)));
			table.addCell(cell(String.format("%.0f%%", task.progress())));
			table.addCell(cell(task.milestone() ? Messages.get("export.yes") : Messages.get("export.no")));
		}
		document.add(table);
	}

	private static void writeChartPage(Document document, BufferedImage chart) throws DocumentException, IOException {
		document.setPageSize(PageSize.A4.rotate());
		document.newPage();
		byte[] png;
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
			ImageIO.write(chart, "png", bytes);
			png = bytes.toByteArray();
		} catch (IOException ex) {
			throw new DocumentException(ex);
		}
		com.lowagie.text.Image image = Image.getInstance(png);
		float maxWidth = PageSize.A4.rotate().getWidth() - 2 * MARGIN;
		float maxHeight = PageSize.A4.rotate().getHeight() - 2 * MARGIN;
		if (image.getWidth() > maxWidth || image.getHeight() > maxHeight) {
			float scale = Math.min(maxWidth / image.getWidth(), maxHeight / image.getHeight());
			image.scalePercent(scale * 100);
		}
		image.setAlignment(Element.ALIGN_CENTER);
		document.add(image);
	}

	private static String indent(TaskDto task) {
		StringBuilder name = new StringBuilder();
		name.append("    ".repeat(Math.max(0, task.outlineLevel() - 1)));
		name.append(task.name());
		return name.toString();
	}

	private static PdfPCell cell(String text) {
		PdfPCell cell = new PdfPCell(new Paragraph(text, BODY_FONT));
		cell.setPadding(4);
		return cell;
	}

}
