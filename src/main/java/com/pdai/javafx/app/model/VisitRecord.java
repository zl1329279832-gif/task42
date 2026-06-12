package com.pdai.javafx.app.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * VisitRecord model representing a recently visited web page.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VisitRecord {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String title;
    private String url;
    private String visitedAt;

    public VisitRecord() {
        // for Jackson
    }

    public VisitRecord(String title, String url) {
        this.title = title;
        this.url = url;
        this.visitedAt = LocalDateTime.now().format(FORMATTER);
    }

    @JsonCreator
    public VisitRecord(
            @JsonProperty("title") String title,
            @JsonProperty("url") String url,
            @JsonProperty("visitedAt") String visitedAt) {
        this.title = title;
        this.url = url;
        this.visitedAt = visitedAt;
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
        return title + " - " + url + " (" + visitedAt + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisitRecord that = (VisitRecord) o;
        return url != null && url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url != null ? url.hashCode() : 0;
    }
}
