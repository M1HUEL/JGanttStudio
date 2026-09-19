package com.itson.jgantt.ui.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class TimeScale {

	private LocalDate startDate;
	private int dayWidth;

	public TimeScale() {
		this(LocalDate.now(), 18);
	}

	public TimeScale(LocalDate startDate, int dayWidth) {
		this.startDate = startDate;
		setDayWidth(dayWidth);
	}

	public int xOf(LocalDate date) {
		return (int) ChronoUnit.DAYS.between(startDate, date) * dayWidth;
	}

	public LocalDate dateOf(int x) {
		return startDate.plusDays(Math.floorDiv(x, dayWidth));
	}

	public int widthFor(LocalDate start, LocalDate end) {
		return (int) (ChronoUnit.DAYS.between(start, end) + 1) * dayWidth;
	}

	public int dayWidth() {
		return dayWidth;
	}

	public void setDayWidth(int dayWidth) {
		this.dayWidth = Math.max(4, dayWidth);
	}

	public LocalDate startDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

}
