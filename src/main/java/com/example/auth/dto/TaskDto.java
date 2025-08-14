package com.example.auth.dto;

public class TaskDto {
    private String id;
    private String title;
    private String status;
    private String dueDate;
    private String notes;
    private String type; // "task"

    public TaskDto(String id, String title, String status, String dueDate, String notes, String type) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
        this.notes = notes;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getNotes() {
        return notes;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}