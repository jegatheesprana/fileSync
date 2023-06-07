package com.example.filesynchttp;

import java.io.File;
import java.util.Date;

public class FileDetail {
    public String fileName;
    public String path;
    public String relativePath;
    public Date lastModified;

    public FileDetail(String fileName, String path, String relativePath, Date lastModified) {
        this.fileName = fileName;
        this.path = path;
        this.relativePath = relativePath;
        this.lastModified = lastModified;
    }
}
