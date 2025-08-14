package com.example.auth.dto;

import java.util.List;

public class DriveFileDto {
    private String id;
    private String name;
    private String mimeType;
    private String modifiedTime;
    private String size;
    private String type; // "file" o "folder"
    private List<DriveFileDto> children; // Solo para carpetas

    public DriveFileDto(String id, String name, String mimeType, String modifiedTime, String size, String type) {
        this.id = id;
        this.name = name;
        this.mimeType = mimeType;
        this.modifiedTime = modifiedTime;
        this.size = size;
        this.type = type;
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

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public List<DriveFileDto> getChildren() {
        return children;
    }

    public void setChildren(List<DriveFileDto> children) {
        this.children = children;
    }
}