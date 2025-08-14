package com.example.auth.dto;

public class CalendarEventDto {
    private String title;
    private String description;
    private String dateTime;

    public CalendarEventDto(String title, String description, String dateTime) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDateTime() {
        return dateTime;
    }
}