package com.itson.jgantt.ui.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public final class DatePicker extends JPanel {

	private static final Color HEADER_BG = new Color(0x1E3A8A);
	private static final Color HEADER_FG = Color.WHITE;
	private static final Color SELECTED_BG = new Color(0x3B82F6);
	private static final Color SELECTED_FG = Color.WHITE;
	private static final Color TODAY_BORDER = new Color(0x3B82F6);
	private static final Color DAY_FG = new Color(0x334155);
	private static final Color ADJACENT_FG = new Color(0xB0B8C4);
	private static final Color WEEKDAY_FG = new Color(0x64748B);

	private YearMonth visible;
	private final LocalDate selected;
	private final Locale locale = Messages.locale();
	private final Consumer<LocalDate> onSelect;
	private final Runnable onCancel;
	private JLabel titleLabel;
	private JPanel grid;

	private DatePicker(LocalDate selected, Consumer<LocalDate> onSelect, Runnable onCancel) {
		super(new BorderLayout());
		this.visible = YearMonth.from(selected);
		this.selected = selected;
		this.onSelect = onSelect;
		this.onCancel = onCancel;
		setOpaque(false);
		setBackground(new Color(0xF8FAFC));
		setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1)));
		add(buildHeader(), BorderLayout.NORTH);
		buildGrid();
	}

	public static void showPopup(Component invoker, int x, int y, LocalDate initial,
		Consumer<LocalDate> onSelect) {
		if (openWindow != null) {
			openWindow.setVisible(false);
		}
		LocalDate base = initial != null ? initial : LocalDate.now();
		Window parent = SwingUtilities.getWindowAncestor(invoker);
		openWindow = new JWindow(parent);
		openWindow.setFocusableWindowState(false);
		openWindow.setLayout(new BorderLayout());
		DatePicker picker = new DatePicker(base,
			date -> {
				openWindow.setVisible(false);
				openWindow = null;
				onSelect.accept(date);
			},
			() -> {
				openWindow.setVisible(false);
				openWindow = null;
			});
		openWindow.add(picker, BorderLayout.CENTER);
		openWindow.pack();
		Point origin = SwingUtilities.convertPoint(invoker, new Point(x, y), parent);
		openWindow.setLocation(origin.x, origin.y + 2);
		openWindow.setVisible(true);
	}

	private static JWindow openWindow;

	private JPanel buildHeader() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(HEADER_BG);
		header.setPreferredSize(new Dimension(250, 32));

		JLabel prev = headerButton("\u2039");
		JLabel next = headerButton("\u203A");
		JLabel close = headerButton("\u00D7");
		prev.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigate(-1);
			}
		});
		next.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				navigate(1);
			}
		});
		close.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				onCancel.run();
			}
		});

		JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		nav.setOpaque(false);
		nav.add(prev);
		nav.add(close);
		nav.add(next);

		titleLabel = new JLabel(monthTitle(), SwingConstants.CENTER);
		titleLabel.setForeground(HEADER_FG);
		titleLabel.setFont(UiFonts.semiBold(13));
		header.add(titleLabel, BorderLayout.CENTER);
		header.add(nav, BorderLayout.EAST);
		return header;
	}

	private JLabel headerButton(String symbol) {
		JLabel label = new JLabel(symbol, SwingConstants.CENTER);
		label.setForeground(HEADER_FG);
		label.setFont(UiFonts.regular(16));
		label.setPreferredSize(new Dimension(26, 26));
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		return label;
	}

	private void navigate(int delta) {
		visible = visible.plusMonths(delta);
		titleLabel.setText(monthTitle());
		buildGrid();
		revalidate();
		repaint();
	}

	private String monthTitle() {
		return visible.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale));
	}

	private void buildGrid() {
		if (grid != null) {
			remove(grid);
		}
		grid = new JPanel(new GridLayout(6, 7, 1, 1));
		grid.setOpaque(false);
		grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		for (String letter : weekdayLetters()) {
			grid.add(weekdayLabel(letter));
		}

		int firstDayOfWeek = WeekFields.of(locale).getFirstDayOfWeek().getValue();
		int offset = (visible.atDay(1).getDayOfWeek().getValue() - firstDayOfWeek + 7) % 7;

		YearMonth previous = visible.minusMonths(1);
		for (int i = 0; i < offset; i++) {
			grid.add(dayLabel(previous.atEndOfMonth().minusDays(offset - 1L - i), ADJACENT_FG));
		}
		for (int day = 1; day <= visible.lengthOfMonth(); day++) {
			LocalDate date = visible.atDay(day);
			grid.add(dayLabel(date, DAY_FG));
		}
		LocalDate nextDay = visible.atEndOfMonth().plusDays(1);
		for (int i = offset + visible.lengthOfMonth(); i < 42; i++) {
			grid.add(dayLabel(nextDay.plusDays(i - offset - visible.lengthOfMonth()), ADJACENT_FG));
		}

		add(grid, BorderLayout.CENTER);
	}

	private JLabel weekdayLabel(String letter) {
		JLabel label = new JLabel(letter, SwingConstants.CENTER);
		label.setFont(UiFonts.semiBold(11));
		label.setForeground(WEEKDAY_FG);
		return label;
	}

	private JLabel dayLabel(LocalDate date, Color foreground) {
		JLabel label = new JLabel(String.valueOf(date.getDayOfMonth()), SwingConstants.CENTER);
		label.setFont(UiFonts.regular(12));
		label.setForeground(foreground);
		label.setOpaque(true);
		label.setBackground(new Color(0xF1F5F9));
		if (date.equals(selected)) {
			label.setForeground(SELECTED_FG);
			label.setBackground(SELECTED_BG);
			return label;
		}
		if (date.equals(LocalDate.now())) {
			label.setBorder(BorderFactory.createLineBorder(TODAY_BORDER));
		} else {
			label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		}
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				onSelect.accept(date);
			}
		});
		return label;
	}

	private static String[] weekdayLetters() {
		return Messages.locale().getLanguage().startsWith("es")
			? new String[]{"L", "M", "X", "J", "V", "S", "D"}
			: new String[]{"M", "T", "W", "T", "F", "S", "S"};
	}

	public static void closePopup() {
		if (openWindow != null) {
			openWindow.setVisible(false);
			openWindow = null;
		}
	}

}
