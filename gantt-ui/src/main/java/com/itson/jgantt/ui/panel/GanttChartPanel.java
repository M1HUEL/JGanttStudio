package com.itson.jgantt.ui.panel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.app.dto.TaskLinkDto;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.util.TimeScale;

public final class GanttChartPanel extends JComponent {

    public static final int HEADER_HEIGHT = 44;
    public static final int ROW_HEIGHT = TaskTablePanel.ROW_HEIGHT;

    private static final Color BAR_COLOR = new Color(0x4A90D9);
    private static final Color PROGRESS_COLOR = new Color(0x2E6DA4);
    private static final Color MILESTONE_COLOR = new Color(0xE67E22);
    private static final Color LINK_COLOR = new Color(0x555555);
    private static final Color GRID_COLOR = new Color(0xE8E8E8);
    private static final Color HEADER_COLOR = new Color(0xE0E7EF);
    private static final Color TODAY_COLOR = new Color(0xC0392B);
    private static final Color TEXT_COLOR = new Color(0x333333);

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private final TimeScale timeScale = new TimeScale();
    private List<TaskDto> allTasks = List.of();
    private List<TaskDto> visible = List.of();
    private List<TaskLinkDto> links = List.of();
    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    public void setData(List<TaskDto> allTasks, List<TaskDto> visible, List<TaskLinkDto> links) {
        this.allTasks = allTasks;
        this.visible = visible;
        this.links = links;
        updateRange();
    }

    public void setVisibleTasks(List<TaskDto> visible) {
        this.visible = visible;
        revalidate();
        repaint();
    }

    public void setDayWidth(int dayWidth) {
        timeScale.setDayWidth(dayWidth);
        revalidate();
        repaint();
    }

    public int dayWidth() {
        return timeScale.dayWidth();
    }

    @Override
    public Dimension getPreferredSize() {
        int width = timeScale.xOf(rangeEnd) + timeScale.dayWidth() + 120;
        int height = HEADER_HEIGHT + visible.size() * ROW_HEIGHT + 60;
        return new Dimension(Math.max(width, 600), height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (rangeStart == null || rangeEnd == null || allTasks.isEmpty() && visible.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        paintBackground(g2);
        paintGrid(g2);
        paintToday(g2);
        paintHeader(g2);
        paintBars(g2);
        paintLinks(g2);
        g2.dispose();
    }

    private void updateRange() {
        LocalDate today = LocalDate.now();
        LocalDate min = today;
        LocalDate max = today;
        for (TaskDto task : allTasks) {
            if (task.start().isBefore(min)) {
                min = task.start();
            }
            if (task.end().isAfter(max)) {
                max = task.end();
            }
        }
        rangeStart = min.minusDays(4);
        rangeEnd = max.plusDays(4);
        timeScale.setStartDate(rangeStart);
        revalidate();
        repaint();
    }

    private void paintBackground(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(GRID_COLOR);
        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            int x = timeScale.xOf(date);
            g.drawLine(x, HEADER_HEIGHT, x, getHeight());
        }
        g.setColor(LINK_COLOR);
        g.drawLine(0, HEADER_HEIGHT, getWidth(), HEADER_HEIGHT);
    }

    private void paintToday(Graphics2D g) {
        LocalDate today = LocalDate.now();
        if (today.isBefore(rangeStart) || today.isAfter(rangeEnd)) {
            return;
        }
        int x = timeScale.xOf(today);
        g.setColor(TODAY_COLOR);
        g.setStroke(new BasicStroke(2f));
        g.drawLine(x, HEADER_HEIGHT, x, getHeight());
        g.setStroke(new BasicStroke(1f));
    }

    private void paintHeader(Graphics2D g) {
        g.setColor(HEADER_COLOR);
        g.fillRect(0, 0, getWidth(), HEADER_HEIGHT);

        g.setFont(HEADER_FONT);
        for (LocalDate month = rangeStart.withDayOfMonth(1);
                !month.isAfter(rangeEnd); month = month.plusMonths(1)) {
            LocalDate monthEnd = month.withDayOfMonth(month.lengthOfMonth());
            int x0 = timeScale.xOf(month);
            int x1 = timeScale.xOf(monthEnd) + timeScale.dayWidth();
            g.setColor(HEADER_COLOR);
            g.fillRect(x0, 0, x1 - x0 - 1, 20);
            g.setColor(TEXT_COLOR);
            g.drawString(month.format(MONTH_FORMAT), x0 + 4, 15);
        }

        g.setColor(TEXT_COLOR);
        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            int x = timeScale.xOf(date);
            g.drawString(String.valueOf(date.getDayOfMonth()), x + 2, HEADER_HEIGHT - 7);
        }

        g.setColor(LINK_COLOR);
        g.drawLine(0, HEADER_HEIGHT - 1, getWidth(), HEADER_HEIGHT - 1);
        g.drawLine(0, 20, getWidth(), 20);
    }

    private void paintBars(Graphics2D g) {
        g.setFont(LABEL_FONT);
        for (int index = 0; index < visible.size(); index++) {
            TaskDto task = visible.get(index);
            int y = HEADER_HEIGHT + index * ROW_HEIGHT;
            int barHeight = Math.max(10, ROW_HEIGHT - 14);
            int barY = y + (ROW_HEIGHT - barHeight) / 2;

            if (task.milestone()) {
                paintMilestone(g, task, barY, barHeight);
            } else {
                paintTaskBar(g, task, barY, barHeight);
            }

            g.setColor(TEXT_COLOR);
            int labelX = timeScale.xOf(task.end()) + timeScale.dayWidth() + 6;
            g.drawString(task.name(), labelX, y + ROW_HEIGHT / 2 + 4);
        }
    }

    private void paintTaskBar(Graphics2D g, TaskDto task, int barY, int barHeight) {
        int x0 = timeScale.xOf(task.start());
        int width = timeScale.widthFor(task.start(), task.end());
        g.setColor(BAR_COLOR);
        g.fillRoundRect(x0, barY, width, barHeight, 4, 4);

        if (task.progress() > 0f) {
            int progressWidth = Math.max(1, Math.round(width * task.progress()));
            g.setColor(PROGRESS_COLOR);
            g.fillRect(x0 + 1, barY + barHeight / 2, progressWidth - 2, barHeight / 2);
        }
        g.setColor(LINK_COLOR);
        g.drawRoundRect(x0, barY, width, barHeight, 4, 4);
    }

    private void paintMilestone(Graphics2D g, TaskDto task, int barY, int barHeight) {
        int centerX = timeScale.xOf(task.start()) + timeScale.dayWidth() / 2;
        int centerY = barY + barHeight / 2;
        int size = Math.max(7, barHeight / 2);
        int[] xs = {centerX, centerX + size, centerX, centerX - size};
        int[] ys = {centerY - size, centerY, centerY + size, centerY};
        g.setColor(MILESTONE_COLOR);
        g.fillPolygon(xs, ys, 4);
        g.setColor(LINK_COLOR);
        g.drawPolygon(xs, ys, 4);
    }

    private void paintLinks(Graphics2D g) {
        Map<TaskId, Integer> rowByTask = new HashMap<>();
        for (int index = 0; index < visible.size(); index++) {
            rowByTask.put(visible.get(index).id(), index);
        }
        Map<TaskId, TaskDto> taskById = new HashMap<>();
        allTasks.forEach(task -> taskById.put(task.id(), task));

        g.setColor(LINK_COLOR);
        for (TaskLinkDto link : links) {
            Integer predRow = rowByTask.get(link.predecessorId());
            Integer succRow = rowByTask.get(link.successorId());
            if (predRow == null || succRow == null) {
                continue;
            }
            TaskDto pred = taskById.get(link.predecessorId());
            TaskDto succ = taskById.get(link.successorId());
            if (pred == null || succ == null) {
                continue;
            }
            int x1 = timeScale.xOf(pred.end()) + timeScale.dayWidth();
            int x2 = timeScale.xOf(succ.start());
            int y1 = HEADER_HEIGHT + predRow * ROW_HEIGHT + ROW_HEIGHT / 2;
            int y2 = HEADER_HEIGHT + succRow * ROW_HEIGHT + ROW_HEIGHT / 2;

            g.drawLine(x1, y1, x2, y1);
            g.drawLine(x2, y1, x2, y2);
            if (y1 != y2) {
                g.drawLine(x2 - 6, y2 - 5, x2, y2);
                g.drawLine(x2, y2, x2 - 6, y2 + 5);
            }
        }
    }
}