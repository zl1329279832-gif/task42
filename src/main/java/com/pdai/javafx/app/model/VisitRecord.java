package com.pdai.javafx.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class VisitRecord {

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("visitedAt")
    private String visitedAt;

    public VisitRecord() {
    }

    public VisitRecord(String title, String url) {
        this.id = UUID.randomUUID().toString();
        this.title = title != null ? title : "";
        this.url = url;
        this.visitedAt = LocalDateTime.now().toString();
    }

    public VisitRecord(String id, String title, String url, String visitedAt) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.visitedAt = visitedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(String visitedAt) {
        this.visitedAt = visitedAt;
    }

    @Override
    public String toString() {
        return title + " | " + url;
    }
}
