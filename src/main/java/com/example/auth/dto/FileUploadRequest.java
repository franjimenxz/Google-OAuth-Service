package com.example.auth.dto;

public class FileUploadRequest {
    private String fileName;
    private String mimeType;
    private String fileContent; // base64 encoded
    private String folderId; // ID de carpeta de Drive (opcional)

    public FileUploadRequest() {
    }

    public FileUploadRequest(String fileName, String mimeType, String fileContent) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileContent = fileContent;
    }

    public FileUploadRequest(String fileName, String mimeType, String fileContent, String folderId) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileContent = fileContent;
        this.folderId = folderId;
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

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}