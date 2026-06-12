package com.pdai.javafx.app.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wrapper for serializing bookmarks and visit records to a single JSON file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookmarkData {

    private List<Bookmark> bookmarks = new ArrayList<>();
    private List<VisitRecord> visitRecords = new ArrayList<>();

    public BookmarkData() {
    }

    public BookmarkData(List<Bookmark> bookmarks, List<VisitRecord> visitRecords) {
        this.bookmarks = bookmarks != null ? bookmarks : new ArrayList<>();
        this.visitRecords = visitRecords != null ? visitRecords : new ArrayList<>();
    }

    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        this.bookmarks = bookmarks != null ? bookmarks : new ArrayList<>();
    }

    public List<VisitRecord> getVisitRecords() {
        return visitRecords;
    }

    public void setVisitRecords(List<VisitRecord> visitRecords) {
        this.visitRecords = visitRecords != null ? visitRecords : new ArrayList<>();
    }
}
