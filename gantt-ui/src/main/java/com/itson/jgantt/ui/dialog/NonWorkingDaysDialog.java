package com.itson.jgantt.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.itson.jgantt.ui.util.Messages;

public final class NonWorkingDaysDialog {

	private static final java.awt.Color BUTTON_BG = new java.awt.Color(0xFFFFFF);
	private static final java.awt.Color BUTTON_HOVER_BG = new java.awt.Color(0xE8EEF7);

	private final JDialog dialog;
	private final List<JCheckBox> dayBoxes = new ArrayList<>();
	private boolean accepted;
	private Set<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);

	private NonWorkingDaysDialog(Window owner, List<DayOfWeek> currentDays) {
		dialog = new JDialog(owner, Messages.get("dialog.nonWorkingDays.title"),
			Dialog.ModalityType.APPLICATION_MODAL);
		Set<DayOfWeek> selected = currentDays == null || currentDays.isEmpty()
			? EnumSet.noneOf(DayOfWeek.class)
			: EnumSet.copyOf(currentDays);

		JPanel daysPanel = new JPanel(new GridLayout(7, 1, 0, 2));
		daysPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
		for (DayOfWeek day : DayOfWeek.values()) {
			JCheckBox box = new JCheckBox(dayName(day), selected.contains(day));
			box.setFocusable(false);
			box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			dayBoxes.add(box);
			daysPanel.add(box);
		}

		JButton okButton = styleButton(Messages.get("dialog.ok"));
		okButton.addActionListener(e -> {
			result = EnumSet.noneOf(DayOfWeek.class);
			for (int i = 0; i < dayBoxes.size(); i++) {
				if (dayBoxes.get(i).isSelected()) {
					result.add(DayOfWeek.values()[i]);
				}
			}
			accepted = true;
			dialog.dispose();
		});

		JButton cancelButton = styleButton(Messages.get("dialog.cancel"));
		cancelButton.addActionListener(e -> dialog.dispose());

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		bottom.add(cancelButton);
		bottom.add(okButton);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		content.add(new JLabel(Messages.get("dialog.nonWorkingDays.hint"),
			JLabel.CENTER), BorderLayout.NORTH);
		content.add(daysPanel, BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);

		dialog.setContentPane(content);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	public static Optional<Set<DayOfWeek>> show(Window owner, List<DayOfWeek> currentDays) {
		NonWorkingDaysDialog dialog = new NonWorkingDaysDialog(owner, currentDays);
		dialog.dialog.setVisible(true);
		return dialog.accepted ? Optional.of(dialog.result) : Optional.empty();
	}

	private static String dayName(DayOfWeek day) {
		String name = day.getDisplayName(TextStyle.FULL, Messages.locale());
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static JButton styleButton(String text) {
		JButton button = new JButton(text);
		button.putClientProperty("JButton.buttonType", "square");
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setFocusable(false);
		button.setMargin(new Insets(5, 14, 5, 14));
		button.setBackground(BUTTON_BG);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new java.awt.Color(0xD0D7E2), 1), new EmptyBorder(0, 0, 0, 0)));
		button.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				button.setBackground(BUTTON_HOVER_BG);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				button.setBackground(BUTTON_BG);
			}
		});
		return button;
	}

}