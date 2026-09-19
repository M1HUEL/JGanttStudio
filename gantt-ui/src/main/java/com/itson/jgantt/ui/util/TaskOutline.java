package com.itson.jgantt.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.itson.jgantt.app.dto.TaskDto;
import com.itson.jgantt.domain.valueobject.TaskId;

public final class TaskOutline {

    private TaskOutline() {
    }

    public static List<TaskDto> visible(List<TaskDto> tasks, Set<TaskId> expanded) {
        List<TaskDto> result = new ArrayList<>();
        for (TaskDto task : tasks) {
            if (task.parentId() == null) {
                appendVisible(task, tasks, expanded, result);
            }
        }
        return result;
    }

    public static boolean hasChildren(List<TaskDto> tasks, TaskId taskId) {
        for (TaskDto task : tasks) {
            if (taskId.equals(task.parentId())) {
                return true;
            }
        }
        return false;
    }

    private static void appendVisible(TaskDto task, List<TaskDto> all, Set<TaskId> expanded, List<TaskDto> out) {
        out.add(task);
        if (!expanded.contains(task.id())) {
            return;
        }
        for (TaskDto child : all) {
            if (task.id().equals(child.parentId())) {
                appendVisible(child, all, expanded, out);
            }
        }
    }
}