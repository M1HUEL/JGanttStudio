package com.itson.jgantt.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

import org.junit.jupiter.api.Test;

class UiIconsTest {

	@Test
	void allIconsPaintWithoutErrors() {
		BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = image.createGraphics();
		try {
			for (Icon icon : new Icon[]{
				UiIcons.newProject(),
				UiIcons.openFolder(),
				UiIcons.save(),
				UiIcons.addTask(),
				UiIcons.addSubtask(),
				UiIcons.addMilestone(),
				UiIcons.delete(),
				UiIcons.link(),
				UiIcons.unlink(),
				UiIcons.zoomIn(),
				UiIcons.zoomOut(),
				UiIcons.rename(),
				UiIcons.table(),
				UiIcons.calendar()}) {
				assertNotNull(icon);
				assertEquals(16, icon.getIconWidth());
				assertEquals(16, icon.getIconHeight());
				icon.paintIcon(null, g2, 0, 0);
			}
		} finally {
			g2.dispose();
		}
	}

}