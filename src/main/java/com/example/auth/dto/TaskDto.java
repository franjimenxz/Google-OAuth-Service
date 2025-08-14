package com.example.auth.dto;

public class TaskDto {
    private String title;
    private String status;
    private String dueDate;
    private String notes;

    public TaskDto(String title, String status, String dueDate, String notes) {
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
        this.notes = notes;
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
}