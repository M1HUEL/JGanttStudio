package com.itson.jgantt.ui.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Vector line icon set. All icons share the same stroke weight, padding and
 * rounded-corner language so they read as a single family.
 */
public final class UiIcons {

	public static final Color ICON_COLOR = new Color(0x44506A);

	private static final int SIZE = 16;
	private static final float STROKE = 1.6f;

	private UiIcons() {
		// ...
	}

	public static Icon newProject() {
		return shape(g -> {
			g.setStroke(stroke());
			Path2D doc = new Path2D.Double();
			doc.moveTo(2.5, 2.5);
			doc.lineTo(9.5, 2.5);
			doc.lineTo(13.5, 6.5);
			doc.lineTo(13.5, 13.5);
			doc.lineTo(2.5, 13.5);
			doc.closePath();
			g.draw(doc);
			g.draw(new Line2D.Double(9.5, 2.5, 9.5, 6.5));
			g.draw(new Line2D.Double(9.5, 6.5, 13.5, 6.5));
		});
	}

	public static Icon openFolder() {
		return shape(g -> {
			g.setStroke(stroke());
			Path2D folder = new Path2D.Double();
			folder.moveTo(1.8, 5.2);
			folder.lineTo(6.0, 5.2);
			folder.lineTo(7.0, 7.0);
			folder.lineTo(14.2, 7.0);
			folder.lineTo(14.2, 13.0);
			folder.lineTo(1.8, 13.0);
			folder.closePath();
			g.draw(folder);
		});
	}

	public static Icon save() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(new RoundRectangle2D.Double(2.0, 1.5, 12.0, 13.0, 1.5, 1.5));
			g.draw(new RoundRectangle2D.Double(4.5, 1.5, 7.0, 4.0, 0.5, 0.5));
			g.draw(new RoundRectangle2D.Double(4.5, 9.5, 7.0, 5.0, 0.5, 0.5));
		});
	}

	public static Icon addTask() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(tile());
			plus(g);
		});
	}

	public static Icon addSubtask() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(tile());
			g.draw(new Line2D.Double(0.5, 8.0, 5.75, 8.0));
			g.draw(new Line2D.Double(6.0, 8.0, 10.25, 8.0));
			g.draw(new Line2D.Double(8.0, 5.75, 8.0, 10.25));
		});
	}

	public static Icon addMilestone() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(tile());
			Path2D diamond = new Path2D.Double();
			diamond.moveTo(8.0, 5.5);
			diamond.lineTo(10.5, 8.0);
			diamond.lineTo(8.0, 10.5);
			diamond.lineTo(5.5, 8.0);
			diamond.closePath();
			g.draw(diamond);
		});
	}

	public static Icon delete() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(new Line2D.Double(3.75, 3.5, 12.25, 3.5));
			g.draw(new Line2D.Double(6.0, 3.5, 6.0, 2.4));
			g.draw(new Line2D.Double(10.0, 3.5, 10.0, 2.4));
			g.draw(new Line2D.Double(6.0, 2.4, 10.0, 2.4));
			Path2D bin = new Path2D.Double();
			bin.moveTo(4.6, 4.2);
			bin.lineTo(11.4, 4.2);
			bin.lineTo(10.7, 12.8);
			bin.lineTo(5.3, 12.8);
			bin.closePath();
			g.draw(bin);
			g.draw(new Line2D.Double(7.1, 6.5, 6.9, 10.5));
			g.draw(new Line2D.Double(9.1, 6.5, 8.9, 10.5));
		});
	}

	public static Icon link() {
		return shape(g -> {
			g.setStroke(stroke());
			drawLink(g, 45.0);
			drawLink(g, -45.0);
		});
	}

	public static Icon unlink() {
		return shape(g -> {
			g.setStroke(stroke());
			Graphics2D left = (Graphics2D) g.create();
			left.rotate(Math.toRadians(45.0), 8.0, 8.0);
			left.draw(new RoundRectangle2D.Double(5.1, 1.8, 5.8, 12.4, 2.9, 2.9));
			left.dispose();
			Graphics2D right = (Graphics2D) g.create();
			right.rotate(Math.toRadians(-45.0), 8.0, 8.0);
			right.draw(new RoundRectangle2D.Double(5.1, 1.8, 5.8, 12.4, 2.9, 2.9));
			right.dispose();
			g.translate(8.0, 8.0);
			g.rotate(Math.toRadians(45.0));
			g.draw(new Line2D.Double(-4.2, 0.0, 4.2, 0.0));
		});
	}

	private static void drawLink(Graphics2D g, double angle) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.rotate(Math.toRadians(angle), 8.0, 8.0);
		g2.draw(new RoundRectangle2D.Double(5.1, 1.8, 5.8, 12.4, 2.9, 2.9));
		g2.dispose();
	}

	public static Icon zoomIn() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(new Ellipse2D.Double(2.5, 2.5, 7.5, 7.5));
			g.draw(new Line2D.Double(8.9, 8.9, 13.4, 13.4));
			g.draw(new Line2D.Double(4.4, 6.2, 8.2, 6.2));
			g.draw(new Line2D.Double(6.3, 4.2, 6.3, 8.2));
		});
	}

	public static Icon zoomOut() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(new Ellipse2D.Double(2.5, 2.5, 7.5, 7.5));
			g.draw(new Line2D.Double(8.9, 8.9, 13.4, 13.4));
			g.draw(new Line2D.Double(4.4, 6.2, 8.2, 6.2));
		});
	}

	public static Icon rename() {
		return shape(g -> {
			g.setStroke(stroke());
			Path2D pencil = new Path2D.Double();
			pencil.moveTo(2.5, 13.5);
			pencil.lineTo(3.2, 10.4);
			pencil.lineTo(10.6, 3.0);
			pencil.lineTo(13.0, 5.4);
			pencil.lineTo(5.6, 12.8);
			pencil.closePath();
			g.draw(pencil);
			g.draw(new Line2D.Double(3.2, 10.4, 5.6, 12.8));
			g.draw(new Line2D.Double(9.0, 4.6, 11.4, 7.0));
		});
	}

	public static Icon table() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(new RoundRectangle2D.Double(1.75, 2.5, 12.5, 11.0, 2.2, 2.2));
			g.draw(new Line2D.Double(1.75, 6.0, 14.25, 6.0));
			g.draw(new Line2D.Double(1.75, 9.5, 14.25, 9.5));
			g.draw(new Line2D.Double(6.4, 6.0, 6.4, 13.5));
		});
	}

	public static Icon calendar() {
		return shape(g -> {
			g.setStroke(stroke());
			g.draw(new RoundRectangle2D.Double(1.75, 3.0, 12.5, 11.0, 2.4, 2.4));
			g.draw(new Line2D.Double(1.75, 6.4, 14.25, 6.4));
			g.draw(new Line2D.Double(4.6, 1.4, 4.6, 4.4));
			g.draw(new Line2D.Double(11.4, 1.4, 11.4, 4.4));
			dot(g, 4.3, 9.2);
			dot(g, 8.0, 9.2);
			dot(g, 11.7, 9.2);
			dot(g, 4.3, 12.0);
			dot(g, 8.0, 12.0);
			dot(g, 11.7, 12.0);
		});
	}

	private static void dot(Graphics2D g, double x, double y) {
		g.fill(new Ellipse2D.Double(x - 0.85, y - 0.85, 1.7, 1.7));
	}

	private static RoundRectangle2D tile() {
		return new RoundRectangle2D.Double(1.75, 1.75, 12.5, 12.5, 3.5, 3.5);
	}

	private static void plus(Graphics2D g) {
		g.draw(new Line2D.Double(5.75, 8.0, 10.25, 8.0));
		g.draw(new Line2D.Double(8.0, 5.75, 8.0, 10.25));
	}

	public static Icon scaled(final Icon icon, final int size) {
		if (icon == null) {
			return null;
		}
		return new Icon() {

			@Override
			public int getIconWidth() {
				return size;
			}

			@Override
			public int getIconHeight() {
				return size;
			}

			@Override
			public void paintIcon(Component component, Graphics graphics, int x, int y) {
				Graphics2D g2 = (Graphics2D) graphics.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.translate(x, y);
				double scale = size / (double) SIZE;
				g2.scale(scale, scale);
				icon.paintIcon(component, g2, 0, 0);
				g2.dispose();
			}
		};
	}

	private static Icon shape(final IconPainter painter) {
		return new Icon() {

			@Override
			public int getIconWidth() {
				return SIZE;
			}

			@Override
			public int getIconHeight() {
				return SIZE;
			}

			@Override
			public void paintIcon(Component component, Graphics graphics, int x, int y) {
				Graphics2D g2 = (Graphics2D) graphics.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.setColor(ICON_COLOR);
				g2.translate(x, y);
				painter.paint(g2);
				g2.dispose();
			}
		};
	}

	private static BasicStroke stroke() {
		return new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	}

	private interface IconPainter {

		void paint(Graphics2D g);
	}

}