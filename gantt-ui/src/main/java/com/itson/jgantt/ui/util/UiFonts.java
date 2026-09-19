package com.itson.jgantt.ui.util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;

public final class UiFonts {

    public static final String FAMILY = "Poppins";

    private static Font regular;
    private static Font medium;
    private static Font semiBold;
    private static Font bold;
    private static boolean loaded;

    private UiFonts() {
    }

    public static void register() {
        if (loaded) {
            return;
        }
        loaded = true;
        regular = load("font/Poppins-Regular.ttf");
        medium = load("font/Poppins-Medium.ttf");
        semiBold = load("font/Poppins-SemiBold.ttf");
        bold = load("font/Poppins-Bold.ttf");
    }

    public static Font regular(int size) {
        return sized(regular, Font.PLAIN, size);
    }

    public static Font medium(int size) {
        return sized(medium, Font.PLAIN, size);
    }

    public static Font semiBold(int size) {
        return sized(semiBold, Font.BOLD, size);
    }

    public static Font bold(int size) {
        return sized(bold, Font.BOLD, size);
    }

    private static Font load(String resource) {
        try (InputStream in = UiFonts.class.getResourceAsStream("/" + resource)) {
            if (in == null) {
                return null;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (IOException | FontFormatException ex) {
            return null;
        }
    }

    private static Font sized(Font face, int style, int size) {
        if (face == null) {
            return new Font(Font.SANS_SERIF, style, size);
        }
        return face.deriveFont(style, size);
    }
}