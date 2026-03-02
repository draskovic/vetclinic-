package com.softart.vetclinic.entity;

public interface FileAttachable {
    String getStoragePath();
    void setStoragePath(String path);
    String getFileName();
    void setFileName(String name);
    String getMimeType();
    void setMimeType(String type);
    Long getFileSizeBytes();
    void setFileSizeBytes(Long size);
}
