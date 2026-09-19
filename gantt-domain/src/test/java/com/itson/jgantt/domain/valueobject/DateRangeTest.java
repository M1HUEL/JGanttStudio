package com.itson.jgantt.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.domain.exception.GanttDomainException;

class DateRangeTest {

	private static final LocalDate DAY = LocalDate.of(2026, 1, 1);

	@Test
	void singleDayHasLengthOne() {
		DateRange range = new DateRange(DAY, DAY);
		assertEquals(1, range.lengthInDays());
	}

	@Test
	void lengthIsInclusive() {
		DateRange range = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));
		assertEquals(3, range.lengthInDays());
	}

	@Test
	void reversedRangeIsRejected() {
		assertThrows(GanttDomainException.class,
			() -> new DateRange(LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 1)));
	}

	@Test
	void nullBoundsAreRejected() {
		assertThrows(GanttDomainException.class, () -> new DateRange(null, DAY));
		assertThrows(GanttDomainException.class, () -> new DateRange(DAY, null));
	}

	@Test
	void includesBoundaryDates() {
		DateRange range = new DateRange(DAY, DAY.plusDays(4));
		assertTrue(range.includes(DAY));
		assertTrue(range.includes(DAY.plusDays(4)));
		assertFalse(range.includes(DAY.minusDays(1)));
		assertFalse(range.includes(DAY.plusDays(5)));
	}

	@Test
	void overlapsDetectsOverlap() {
		DateRange a = new DateRange(DAY, DAY.plusDays(4));
		DateRange b = new DateRange(DAY.plusDays(3), DAY.plusDays(9));
		assertTrue(a.overlaps(b));
	}

	@Test
	void disjointRangesDoNotOverlap() {
		DateRange a = new DateRange(DAY, DAY.plusDays(4));
		DateRange b = new DateRange(DAY.plusDays(5), DAY.plusDays(9));
		assertFalse(a.overlaps(b));
	}

	@Test
	void withStartKeepsEnd() {
		DateRange range = new DateRange(DAY, DAY.plusDays(4));
		DateRange moved = range.withStart(DAY.plusDays(1));
		assertEquals(DAY.plusDays(4), moved.end());
		assertEquals(DAY.plusDays(1), moved.start());
	}

	@Test
	void withStartAfterEndIsRejected() {
		DateRange range = new DateRange(DAY, DAY.plusDays(4));
		assertThrows(GanttDomainException.class, () -> range.withStart(DAY.plusDays(9)));
	}

}
