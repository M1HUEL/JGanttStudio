package com.itson.jgantt.ui.model;

import java.time.LocalDate;

import com.itson.jgantt.domain.valueobject.TaskId;

public interface TaskEditListener {

    void onRename(TaskId taskId, String newName);

    void onStartChange(TaskId taskId, LocalDate newStart);

    void onEndChange(TaskId taskId, LocalDate newEnd);

    void onProgressChange(TaskId taskId, float newProgress);
}