package com.example.auth.dto;

import java.util.List;

public class TaskListDto {
    private String id;
    private String title;
    private String type; // "tasklist"
    private List<TaskDto> tasks;

    public TaskListDto(String id, String title, String type) {
        this.id = id;
        this.title = title;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public List<TaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskDto> tasks) {
        this.tasks = tasks;
    }
}