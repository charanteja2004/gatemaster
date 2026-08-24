package com.example.gatemaster.models;

import java.io.Serializable;

public class Topic implements Serializable {
    private String id;
    private String title;
    private String contentPath;
    private String pdfPath;

    public Topic(String id, String title, String contentPath, String pdfPath) {
        this.id = id;
        this.title = title;
        this.contentPath = contentPath;
        this.pdfPath = pdfPath;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContentPath() {
        return contentPath;
    }

    public String getPdfPath() {
        return pdfPath;
    }
}
