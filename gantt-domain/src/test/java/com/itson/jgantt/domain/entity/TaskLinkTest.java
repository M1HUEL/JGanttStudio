package com.itson.jgantt.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.itson.jgantt.domain.exception.GanttDomainException;
import com.itson.jgantt.domain.valueobject.DependencyType;
import com.itson.jgantt.domain.valueobject.Lag;
import com.itson.jgantt.domain.valueobject.TaskId;

class TaskLinkTest {

	@Test
	void finishToStartFactoryBuildsPlainLink() {
		TaskId pred = TaskId.random();
		TaskId succ = TaskId.random();

		TaskLink link = TaskLink.finishToStart(pred, succ);

		assertEquals(pred, link.predecessorId());
		assertEquals(succ, link.successorId());
		assertEquals(DependencyType.FINISH_TO_START, link.type());
		assertEquals(Lag.ZERO, link.lag());
	}

	@Test
	void selfLinkIsRejected() {
		TaskId id = TaskId.random();
		assertThrows(GanttDomainException.class, () -> TaskLink.finishToStart(id, id));
	}

}
