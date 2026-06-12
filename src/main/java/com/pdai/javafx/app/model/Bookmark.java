package com.pdai.javafx.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

public class Bookmark {

    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("createdAt")
    private String createdAt;

    public Bookmark() {
    }

    public Bookmark(String title, String url) {
        this.id = UUID.randomUUID().toString();
        this.title = title != null ? title : "";
        this.url = url;
        this.createdAt = LocalDateTime.now().toString();
    }

    public Bookmark(String id, String title, String url, String createdAt) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.createdAt = createdAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return url != null && bookmark.url != null && url.equalsIgnoreCase(bookmark.url);
    }

    @Override
    public int hashCode() {
        return url != null ? url.toLowerCase().hashCode() : 0;
    }

    @Override
    public String toString() {
        return title + " | " + url;
    }
}
