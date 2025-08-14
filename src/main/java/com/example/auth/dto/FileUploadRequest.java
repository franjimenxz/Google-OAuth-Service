package com.example.auth.dto;

public class FileUploadRequest {
    private String fileName;
    private String mimeType;
    private String fileContent; // base64 encoded

    public FileUploadRequest() {
    }

    public FileUploadRequest(String fileName, String mimeType, String fileContent) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }
}