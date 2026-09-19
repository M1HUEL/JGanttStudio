package com.itson.jgantt.ui.model;

import java.time.LocalDate;

import com.itson.jgantt.domain.valueobject.TaskId;

public interface TaskDragListener {

    void onDatesChange(TaskId taskId, LocalDate newStart, LocalDate newEnd);
}