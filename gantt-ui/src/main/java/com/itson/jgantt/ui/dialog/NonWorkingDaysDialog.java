package com.itson.jgantt.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.border.EmptyBorder;

import com.itson.jgantt.ui.util.Messages;

public final class NonWorkingDaysDialog {

	private static final Color BUTTON_BG = new Color(0xFFFFFF);
	private static final Color BUTTON_HOVER_BG = new Color(0xE8EEF7);

	private final JDialog dialog;
	private final DefaultListModel<LocalDate> listModel = new DefaultListModel<>();
	private final JList<LocalDate> list;
	private final JSpinner dateSpinner;
	private boolean accepted;
	private Set<LocalDate> result = Set.of();

	private NonWorkingDaysDialog(Window owner, List<LocalDate> currentDays) {
		dialog = new JDialog(owner, Messages.get("dialog.nonWorkingDays.title"),
			Dialog.ModalityType.APPLICATION_MODAL);
		currentDays.forEach(listModel::addElement);

		list = new JList<>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(8);
		list.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus);
				if (value instanceof LocalDate date) {
					DateTimeFormatter formatter = DateTimeFormatter
						.ofLocalizedDate(FormatStyle.MEDIUM)
						.withLocale(Messages.locale());
					label.setText(formatter.format(date));
				}
				return label;
			}
		});

		dateSpinner = new JSpinner(new SpinnerDateModel());
		dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, datePattern()));

		JButton addButton = styleButton(Messages.get("dialog.nonWorkingDays.add"));
		addButton.addActionListener(e -> addSpinnerDate());

		JButton removeButton = styleButton(Messages.get("dialog.nonWorkingDays.remove"));
		removeButton.setEnabled(false);
		removeButton.addActionListener(e -> {
			LocalDate selected = list.getSelectedValue();
			if (selected != null) {
				listModel.removeElement(selected);
			}
		});
		list.addListSelectionListener(e -> removeButton.setEnabled(list.getSelectedIndex() >= 0));

		JButton okButton = styleButton(Messages.get("dialog.ok"));
		okButton.addActionListener(e -> {
			result = new TreeSet<>();
			for (int i = 0; i < listModel.size(); i++) {
				result.add(listModel.get(i));
			}
			accepted = true;
			dialog.dispose();
		});

		JButton cancelButton = styleButton(Messages.get("dialog.cancel"));
		cancelButton.addActionListener(e -> dialog.dispose());

		JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
		inputPanel.add(new JLabel(Messages.get("dialog.nonWorkingDays.date") + " "));
		inputPanel.add(dateSpinner);
		inputPanel.add(addButton);

		JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setBorder(new EmptyBorder(0, 8, 0, 8));
		listPanel.add(new JScrollPane(list), BorderLayout.CENTER);

		JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
		listButtons.add(removeButton);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(listButtons, BorderLayout.CENTER);
		JPanel okCancel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
		okCancel.add(cancelButton);
		okCancel.add(okButton);
		bottom.add(okCancel, BorderLayout.EAST);

		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		content.add(inputPanel, BorderLayout.NORTH);
		content.add(listPanel, BorderLayout.CENTER);
		content.add(bottom, BorderLayout.SOUTH);

		dialog.setContentPane(content);
		dialog.pack();
		dialog.setLocationRelativeTo(owner);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}

	public static Optional<Set<LocalDate>> show(Window owner, List<LocalDate> currentDays) {
		NonWorkingDaysDialog dialog = new NonWorkingDaysDialog(owner, currentDays);
		dialog.dialog.setVisible(true);
		return dialog.accepted ? Optional.of(dialog.result) : Optional.empty();
	}

	private void addSpinnerDate() {
		Date date = (Date) dateSpinner.getValue();
		LocalDate day = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (!listModel.contains(day)) {
			listModel.addElement(day);
		}
	}

	private static JButton styleButton(String text) {
		JButton button = new JButton(text);
		button.putClientProperty("JButton.buttonType", "square");
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.setFocusable(false);
		button.setMargin(new Insets(5, 14, 5, 14));
		button.setBackground(BUTTON_BG);
		button.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(0xD0D7E2), 1), new EmptyBorder(0, 0, 0, 0)));
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

	private static String datePattern() {
		return Messages.locale().getLanguage().startsWith("es") ? "dd/MM/yyyy" : "MM/dd/yyyy";
	}

}