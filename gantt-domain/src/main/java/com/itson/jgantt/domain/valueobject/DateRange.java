package com.itson.jgantt.domain.valueobject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.itson.jgantt.domain.exception.GanttDomainException;

public record DateRange(LocalDate start, LocalDate end) {

	public DateRange {
		if (start == null || end == null) {
			throw new GanttDomainException("DateRange bounds must not be null");
		}
		if (start.isAfter(end)) {
			throw new GanttDomainException("start (" + start + ") must not be after end (" + end + ")");
		}
	}

	public long lengthInDays() {
		return ChronoUnit.DAYS.between(start, end) + 1;
	}

	public boolean includes(LocalDate date) {
		return !date.isBefore(start) && !date.isAfter(end);
	}

	public boolean overlaps(DateRange other) {
		return !end.isBefore(other.start) && !other.end.isBefore(start);
	}

	public DateRange withStart(LocalDate newStart) {
		return new DateRange(newStart, end);
	}

	public DateRange withEnd(LocalDate newEnd) {
		return new DateRange(start, newEnd);
	}
}
