package com.itson.jgantt.domain.valueobject;

import com.itson.jgantt.domain.exception.GanttDomainException;

public record Lag(int days) {

	public static final Lag ZERO = new Lag(0);

	public Lag {
		if (days < -3650 || days > 3650) {
			throw new GanttDomainException("Lag out of range (days must be between -3650 and 3650): " + days);
		}
	}

	public Lag plus(int daysToAdd) {
		return new Lag(days + daysToAdd);
	}
}
