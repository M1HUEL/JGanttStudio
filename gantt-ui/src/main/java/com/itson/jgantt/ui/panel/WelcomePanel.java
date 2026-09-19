package com.itson.jgantt.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.itson.jgantt.ui.util.Messages;
import com.itson.jgantt.ui.util.UiFonts;

public final class WelcomePanel extends JPanel {

	private static final Color TITLE_COLOR = new Color(0x1E293B);
	private static final Color SUBTITLE_COLOR = new Color(0x64748B);
	private static final Color BUTTON_BG = new Color(0x3B82F6);
	private static final Color BUTTON_HOVER_BG = new Color(0x2563EB);

	private final Runnable onNewProject;
	private final Runnable onOpenProject;
	private final JLabel titleLabel = new JLabel(" ", JLabel.CENTER);
	private final JLabel subtitleLabel = new JLabel(" ", JLabel.CENTER);
	private final JButton newButton = new JButton();
	private final JButton openButton = new JButton();

	public WelcomePanel(Runnable onNewProject, Runnable onOpenProject) {
		super(new BorderLayout());
		this.onNewProject = onNewProject;
		this.onOpenProject = onOpenProject;

		newButton.putClientProperty("JButton.buttonType", "roundRect");
		newButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		newButton.setMargin(new Insets(14, 36, 14, 36));
		newButton.setOpaque(true);
		newButton.setBackground(BUTTON_BG);
		newButton.setForeground(Color.WHITE);
		newButton.setBorderPainted(false);
		newButton.setFocusable(false);
		newButton.addActionListener(e -> onNewProject.run());
		newButton.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				newButton.setBackground(BUTTON_HOVER_BG);
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				newButton.setBackground(BUTTON_BG);
			}
		});

		openButton.putClientProperty("JButton.buttonType", "roundRect");
		openButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		openButton.setMargin(new Insets(14, 36, 14, 36));
		openButton.setOpaque(true);
		openButton.setBackground(Color.WHITE);
		openButton.setForeground(BUTTON_BG);
		openButton.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 2));
		openButton.setFocusable(false);
		openButton.addActionListener(e -> onOpenProject.run());
		openButton.addMouseListener(new java.awt.event.MouseAdapter() {

			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				openButton.setBackground(new Color(0xF1F5F9));
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				openButton.setBackground(Color.WHITE);
			}
		});

		JPanel card = new JPanel(new GridBagLayout());
		card.setBackground(Color.WHITE);
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;
		gc.insets = new Insets(0, 0, 18, 0);
		card.add(titleLabel, gc);
		gc.gridy = 1;
		gc.insets = new Insets(0, 0, 44, 0);
		card.add(subtitleLabel, gc);
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.insets = new Insets(0, 0, 0, 12);
		card.add(newButton, gc);
		gc.gridx = 1;
		gc.insets = new Insets(0, 12, 0, 0);
		card.add(openButton, gc);

		add(card, BorderLayout.CENTER);
		refreshTexts();
	}

	public void refreshTexts() {
		titleLabel.setText(Messages.get("welcome.title"));
		subtitleLabel.setText(Messages.get("welcome.subtitle"));
		newButton.setText(Messages.get("welcome.new"));
		openButton.setText(Messages.get("welcome.open"));
		titleLabel.setFont(UiFonts.bold(26));
		subtitleLabel.setFont(UiFonts.regular(14));
		newButton.setFont(UiFonts.semiBold(15));
		openButton.setFont(UiFonts.semiBold(15));
		titleLabel.setForeground(TITLE_COLOR);
		subtitleLabel.setForeground(SUBTITLE_COLOR);
	}

}
