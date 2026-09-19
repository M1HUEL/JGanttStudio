package com.itson.jgantt.ui.panel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;

import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.app.dto.TaskLinkDto;
import com.itson.jgantt.domain.valueobject.TaskId;
import com.itson.jgantt.ui.model.TaskDragListener;
import com.itson.jgantt.ui.util.TimeScale;
import com.itson.jgantt.ui.util.UiFonts;

public final class GanttChartPanel extends JComponent {

    public static final int HEADER_HEIGHT = 44;
    public static final int ROW_HEIGHT = TaskTablePanel.ROW_HEIGHT;

    private static final Color BAR_COLOR = new Color(0x3B82F6);
    private static final Color PROGRESS_COLOR = new Color(0x1D4ED8);
    private static final Color MILESTONE_COLOR = new Color(0xF59E0B);
    private static final Color LINK_COLOR = new Color(0x64748B);
    private static final Color GRID_COLOR = new Color(0xE2E8F0);
    private static final Color HEADER_COLOR = new Color(0xEEF2F7);
    private static final Color TODAY_COLOR = new Color(0xEF4444);
    private static final Color TEXT_COLOR = new Color(0x334155);
    private static final Color WEEKEND_COLOR = new Color(0xFAFAFB);
    private static final Color SELECTION_COLOR = new Color(59, 130, 246, 38);
    private static final Color SELECTED_BAR_COLOR = new Color(0x1E3A8A);
    private static final Color ZEBRA_COLOR = new Color(0xF5F7FB);

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final Font HEADER_FONT = UiFonts.semiBold(12);
    private static final Font LABEL_FONT = UiFonts.regular(12);

    private final TimeScale timeScale = new TimeScale();
    private List<TaskDto> allTasks = List.of();
    private List<TaskDto> visible = List.of();
    private List<TaskLinkDto> links = List.of();
    private Set<TaskId> selectedIds = Set.of();
    private TaskDragListener dragListener;
    private DragMode dragMode = DragMode.NONE;
    private TaskId dragTaskId;
    private LocalDate dragAnchor;
    private LocalDate dragStartAtPress;
    private LocalDate dragEndAtPress;
    private LocalDate draftStart;
    private LocalDate draftEnd;
    private LocalDate rangeStart;
    private LocalDate rangeEnd;

    public GanttChartPanel() {
        TaskDragHandler handler = new TaskDragHandler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int row = rowAt(event.getY());
        if (row < 0) {
            return null;
        }
        TaskDto task = visible.get(row);
        if (event.getY() < HEADER_HEIGHT + ROW_HEIGHT) {
            long days = ChronoUnit.DAYS.between(task.start(), task.end()) + 1;
            String prefix = task.milestone() ? "Milestone" : "Task";
            return String.format(
                    "<html><b>%s</b> (%s)<br>%s to %s &middot; %d day%s</html>",
                    task.name(), prefix, task.start(), task.end(), days, days == 1 ? "" : "s");
        }
        return null;
    }

    public void setDragListener(TaskDragListener dragListener) {
        this.dragListener = dragListener;
    }

    public void setData(List<TaskDto> allTasks, List<TaskDto> visible, List<TaskLinkDto> links) {
        this.allTasks = allTasks;
        this.visible = visible;
        this.links = links;
        updateRange();
    }

    public void setSelectedIds(Set<TaskId> selectedIds) {
        this.selectedIds = selectedIds == null ? Set.of() : Set.copyOf(selectedIds);
        repaint();
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
        paintWeekends(g2);
        paintZebraBands(g2);
        paintGrid(g2);
        paintToday(g2);
        paintHeader(g2);
        paintSelection(g2);
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

    private void paintZebraBands(Graphics2D g) {
        g.setColor(ZEBRA_COLOR);
        for (int index = 1; index < visible.size(); index += 2) {
            int y = HEADER_HEIGHT + index * ROW_HEIGHT;
            g.fillRect(0, y, getWidth(), ROW_HEIGHT);
        }
    }

    private void paintWeekends(Graphics2D g) {
        g.setColor(WEEKEND_COLOR);
        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            DayOfWeek day = date.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                int x = timeScale.xOf(date);
                g.fillRect(x, HEADER_HEIGHT, timeScale.dayWidth(), getHeight() - HEADER_HEIGHT);
            }
        }
    }

    private void paintSelection(Graphics2D g) {
        if (selectedIds.isEmpty()) {
            return;
        }
        g.setColor(SELECTION_COLOR);
        for (int index = 0; index < visible.size(); index++) {
            if (selectedIds.contains(visible.get(index).id())) {
                int y = HEADER_HEIGHT + index * ROW_HEIGHT;
                g.fillRect(0, y, getWidth(), ROW_HEIGHT);
            }
        }
    }

    private void paintGrid(Graphics2D g) {
        g.setColor(GRID_COLOR);
        for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            int x = timeScale.xOf(date);
            g.drawLine(x, HEADER_HEIGHT, x, getHeight());
        }
        for (int index = 0; index <= visible.size(); index++) {
            int y = HEADER_HEIGHT + index * ROW_HEIGHT;
            g.drawLine(0, y, getWidth(), y);
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
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[] {4f, 4f}, 0f));
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
        if (timeScale.dayWidth() >= 8) {
            for (LocalDate date = rangeStart; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
                int x = timeScale.xOf(date);
                g.drawString(String.valueOf(date.getDayOfMonth()), x + 2, HEADER_HEIGHT - 7);
            }
        }

        g.setColor(LINK_COLOR);
        g.drawLine(0, HEADER_HEIGHT - 1, getWidth(), HEADER_HEIGHT - 1);
        g.drawLine(0, 20, getWidth(), 20);
    }

    private void paintBars(Graphics2D g) {
        g.setFont(LABEL_FONT);
        for (int index = 0; index < visible.size(); index++) {
            TaskDto task = visible.get(index);
            boolean selected = selectedIds.contains(task.id());
            LocalDate start = effectiveStart(task);
            LocalDate end = effectiveEnd(task);
            int y = HEADER_HEIGHT + index * ROW_HEIGHT;
            int barHeight = Math.max(10, ROW_HEIGHT - 14);
            int barY = y + (ROW_HEIGHT - barHeight) / 2;

            if (task.milestone()) {
                paintMilestone(g, task, start, barY, barHeight, selected);
            } else {
                paintTaskBar(g, task, start, end, barY, barHeight, selected);
            }

            g.setColor(TEXT_COLOR);
            int labelX = timeScale.xOf(end) + timeScale.dayWidth() + 6;
            g.drawString(task.name(), labelX, y + ROW_HEIGHT / 2 + 4);
        }
    }

    private LocalDate effectiveStart(TaskDto task) {
        return dragMode != DragMode.NONE && dragTaskId != null && dragTaskId.equals(task.id())
                ? draftStart : task.start();
    }

    private LocalDate effectiveEnd(TaskDto task) {
        return dragMode != DragMode.NONE && dragTaskId != null && dragTaskId.equals(task.id())
                ? draftEnd : task.end();
    }

    private void paintTaskBar(Graphics2D g, TaskDto task, LocalDate start, LocalDate end,
            int barY, int barHeight, boolean selected) {
        int x0 = timeScale.xOf(start);
        int width = timeScale.widthFor(start, end);
        g.setColor(BAR_COLOR);
        g.fillRoundRect(x0, barY, width, barHeight, 4, 4);

        if (task.progress() > 0f) {
            int progressWidth = Math.max(1, Math.round(width * task.progress()));
            g.setColor(PROGRESS_COLOR);
            g.fillRect(x0 + 1, barY + barHeight / 2, progressWidth - 2, barHeight / 2);
        }
        if (selected) {
            g.setColor(SELECTED_BAR_COLOR);
            g.setStroke(new BasicStroke(2f));
        } else {
            g.setColor(LINK_COLOR);
        }
        g.drawRoundRect(x0, barY, width, barHeight, 4, 4);
        g.setStroke(new BasicStroke(1f));
    }

    private void paintMilestone(Graphics2D g, TaskDto task, LocalDate date, int barY, int barHeight,
            boolean selected) {
        int centerX = timeScale.xOf(date) + timeScale.dayWidth() / 2;
        int centerY = barY + barHeight / 2;
        int size = Math.max(8, barHeight);
        int[] xs = {centerX, centerX + size, centerX, centerX - size};
        int[] ys = {centerY - size, centerY, centerY + size, centerY};
        g.setColor(MILESTONE_COLOR);
        g.fillPolygon(xs, ys, 4);
        if (selected) {
            g.setColor(SELECTED_BAR_COLOR);
            g.setStroke(new BasicStroke(2f));
        } else {
            g.setColor(LINK_COLOR);
        }
        g.drawPolygon(xs, ys, 4);
        g.setStroke(new BasicStroke(1f));
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

            drawLinkPath(g, x1, y1, x2, y2);
        }
    }

    private void drawLinkPath(Graphics2D g, int x1, int y1, int x2, int y2) {
        int startX = x1 + 8;
        int endX = x2 - 6;
        if (endX > startX) {
            g.drawLine(startX, y1, endX, y1);
            if (y1 != y2) {
                g.drawLine(endX, y1, endX, y2);
            }
        } else {
            int step = y1 == y2 ? -8 : (y1 < y2 ? 6 : -6);
            int midY = y1 + step;
            g.drawLine(startX, y1, startX, midY);
            g.drawLine(startX, midY, endX, midY);
            if (y1 != y2) {
                g.drawLine(endX, midY, endX, y2);
            }
        }
        g.fillPolygon(arrowHead(x2, y2));
    }

    private Polygon arrowHead(int x, int y) {
        Polygon arrow = new Polygon();
        arrow.addPoint(x, y);
        arrow.addPoint(x - 7, y - 4);
        arrow.addPoint(x - 7, y + 4);
        return arrow;
    }

    private int rowAt(int y) {
        int index = (y - HEADER_HEIGHT) / ROW_HEIGHT;
        return index >= 0 && index < visible.size() ? index : -1;
    }

    private DragMode hitTest(TaskDto task, int x) {
        if (task.milestone()) {
            int centerX = timeScale.xOf(task.start()) + timeScale.dayWidth() / 2;
            int radius = Math.max(8, Math.max(10, ROW_HEIGHT - 14)) + 8;
            int dx = x - centerX;
            if (dx >= -radius && dx <= radius) {
                return DragMode.MOVE;
            }
            return DragMode.NONE;
        }
        int x0 = timeScale.xOf(task.start());
        int x1 = x0 + timeScale.widthFor(task.start(), task.end());
        if (x < x0 - 4 || x > x1 + 4) {
            return DragMode.NONE;
        }
        int edge = 4;
        if (x <= x0 + edge) {
            return DragMode.RESIZE_START;
        }
        if (x >= x1 - edge) {
            return DragMode.RESIZE_END;
        }
        return DragMode.MOVE;
    }

    private void beginDrag(TaskDto task, DragMode mode, int x) {
        dragTaskId = task.id();
        dragMode = mode;
        dragAnchor = timeScale.dateOf(x);
        dragStartAtPress = task.start();
        dragEndAtPress = task.end();
        draftStart = task.start();
        draftEnd = task.end();
        repaint();
    }

    private void updateDrag(int x) {
        if (dragMode == DragMode.NONE || dragTaskId == null) {
            return;
        }
        long delta = ChronoUnit.DAYS.between(dragAnchor, timeScale.dateOf(x));
        LocalDate start = dragStartAtPress.plusDays(delta);
        LocalDate end = dragEndAtPress.plusDays(delta);
        switch (dragMode) {
            case MOVE -> {
                draftStart = start;
                draftEnd = end;
            }
            case RESIZE_START -> {
                if (!start.isAfter(dragEndAtPress)) {
                    draftStart = start;
                    draftEnd = dragEndAtPress;
                }
            }
            case RESIZE_END -> {
                if (!end.isBefore(dragStartAtPress)) {
                    draftStart = dragStartAtPress;
                    draftEnd = end;
                }
            }
            default -> { /* nothing */ }
        }
        repaint();
    }

    private void finishDrag() {
        if (dragMode == DragMode.NONE || dragTaskId == null) {
            return;
        }
        DragMode finishedMode = dragMode;
        TaskId finishedId = dragTaskId;
        LocalDate start = draftStart;
        LocalDate end = draftEnd;
        LocalDate originalStart = dragStartAtPress;
        LocalDate originalEnd = dragEndAtPress;
        dragMode = DragMode.NONE;
        dragTaskId = null;
        repaint();
        if (dragListener != null
                && (!start.equals(originalStart) || !end.equals(originalEnd))) {
            dragListener.onDatesChange(finishedId, start, end);
        }
    }

    private enum DragMode {
        NONE,
        MOVE,
        RESIZE_START,
        RESIZE_END
    }

    private final class TaskDragHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1 || e.getY() < HEADER_HEIGHT) {
                return;
            }
            int row = rowAt(e.getY());
            if (row < 0) {
                return;
            }
            TaskDto task = visible.get(row);
            DragMode mode = hitTest(task, e.getX());
            if (mode != DragMode.NONE) {
                beginDrag(task, mode, e.getX());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updateDrag(e.getX());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            finishDrag();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int row = e.getY() < HEADER_HEIGHT ? -1 : rowAt(e.getY());
            int cursor;
            if (row < 0) {
                cursor = Cursor.DEFAULT_CURSOR;
            } else {
                cursor = switch (hitTest(visible.get(row), e.getX())) {
                    case RESIZE_START, RESIZE_END -> Cursor.E_RESIZE_CURSOR;
                    case MOVE -> Cursor.MOVE_CURSOR;
                    default -> Cursor.DEFAULT_CURSOR;
                };
            }
            if (getCursor().getType() != cursor) {
                setCursor(Cursor.getPredefinedCursor(cursor));
            }
        }
    }
}