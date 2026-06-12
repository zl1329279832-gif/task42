package com.pdai.javafx.app.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Bookmark model representing a saved web page.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bookmark {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String title;
    private String url;
    private String createdAt;

    public Bookmark() {
        // for Jackson
    }

    public Bookmark(String title, String url) {
        this.title = title;
        this.url = url;
        this.createdAt = LocalDateTime.now().format(FORMATTER);
    }

    @JsonCreator
    public Bookmark(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("createdAt") String createdAt) {
        this.title = title;
        this.url = url;
        this.createdAt = createdAt;
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
    public String toString() {
        return title + " - " + url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bookmark bookmark = (Bookmark) o;
        return url != null && url.equalsIgnoreCase(bookmark.url);
    }

    @Override
    public int hashCode() {
        return url != null ? url.toLowerCase().hashCode() : 0;
    }
}
