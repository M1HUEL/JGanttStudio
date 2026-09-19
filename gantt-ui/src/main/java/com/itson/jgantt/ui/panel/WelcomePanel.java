package com.itson.jgantt.ui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.itson.jgantt.ui.util.Messages;
import com.itson.jgantt.ui.util.UiFonts;
import com.itson.jgantt.ui.util.UiIcons;

public final class WelcomePanel extends JPanel {

	private static final Color BACKGROUND_TOP = new Color(0xEAF1FB);
	private static final Color BACKGROUND_BOTTOM = new Color(0xFFFFFF);
	private static final Color CARD_BORDER = new Color(0xD8E2F0);
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
		setOpaque(true);

		newButton.putClientProperty("JButton.buttonType", "roundRect");
		newButton.setIcon(UiIcons.newProject());
		newButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		newButton.setMargin(new Insets(12, 28, 12, 28));
		newButton.setOpaque(true);
		newButton.setBackground(BUTTON_BG);
		newButton.setForeground(Color.WHITE);
		newButton.setBorderPainted(false);
		newButton.setFocusable(false);
		newButton.setIconTextGap(10);
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
		openButton.setIcon(UiIcons.openFolder());
		openButton.setForeground(BUTTON_BG);
		openButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		openButton.setMargin(new Insets(12, 28, 12, 28));
		openButton.setOpaque(true);
		openButton.setBackground(Color.WHITE);
		openButton.setBorder(BorderFactory.createLineBorder(new Color(0xCBD5E1), 2));
		openButton.setFocusable(false);
		openButton.setIconTextGap(10);
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

		JPanel card = new RoundedCard(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.gridwidth = 2;
		gc.insets = new Insets(0, 0, 6, 0);
		card.add(titleLabel, gc);
		gc.gridy = 1;
		gc.insets = new Insets(0, 0, 30, 0);
		card.add(subtitleLabel, gc);
		gc.gridy = 2;
		gc.gridwidth = 1;
		gc.insets = new Insets(0, 0, 0, 12);
		card.add(newButton, gc);
		gc.gridx = 1;
		gc.insets = new Insets(0, 12, 0, 0);
		card.add(openButton, gc);

		CardHolder holder = new CardHolder();
		holder.add(card, BorderLayout.CENTER);
		add(holder, BorderLayout.CENTER);
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

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g2 = (Graphics2D) graphics.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setPaint(new java.awt.GradientPaint(0, 0, BACKGROUND_TOP,
			0, getHeight(), BACKGROUND_BOTTOM));
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.dispose();
		super.paintComponent(graphics);
	}

	private static final class CardHolder extends JPanel {

		private CardHolder() {
			super(new BorderLayout());
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));
			setPreferredSize(new java.awt.Dimension(700, 480));
		}

	}

	private static final class RoundedCard extends JPanel {

		private RoundedCard(java.awt.LayoutManager layout) {
			super(layout);
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(46, 64, 46, 64));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Color.WHITE);
			int arc = 22;
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
			g2.dispose();
		}

		@Override
		public void paintBorder(Graphics graphics) {
			Graphics2D g2 = (Graphics2D) graphics.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(CARD_BORDER);
			int arc = 22;
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
			g2.dispose();
		}

	}

}