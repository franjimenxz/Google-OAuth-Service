package com.example.auth.dto;

public class DriveFileDto {
    private String id;
    private String name;
    private String mimeType;
    private String modifiedTime;
    private String size;

    public DriveFileDto(String id, String name, String mimeType, String modifiedTime, String size) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.modifiedTime = modifiedTime;
        this.size = size;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public String getSize() {
        return size;
    }
}