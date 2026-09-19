package com.itson.jgantt.ui.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {

	private static final String BASE_NAME = "com.itson.jgantt.ui.i18n.messages";
	private static Locale locale = Locale.getDefault();
	private static ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);

	private Messages() {
		// ...
	}

	public static Locale locale() {
		return locale;
	}

	public static void setLocale(Locale newLocale) {
		locale = newLocale;
		bundle = ResourceBundle.getBundle(BASE_NAME, locale);
	}

	public static String get(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException ex) {
			return key;
		}
	}

	public static String formatDate(LocalDate date) {
		return dateFormatter().format(date);
	}

	public static LocalDate parseDate(String text) {
		return LocalDate.parse(text.trim(), dateFormatter());
	}

	public static DateTimeFormatter dateFormatter() {
		return DateTimeFormatter.ofPattern(datePattern()).withLocale(locale);
	}

	public static String datePattern() {
		return locale.getLanguage().startsWith("es") ? "dd/MM/yyyy" : "MM/dd/yyyy";
	}

}
