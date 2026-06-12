package com.pdai.javafx.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class WebViewData {

    @JsonProperty("bookmarks")
    private List<Bookmark> bookmarks = new ArrayList<>();

    @JsonProperty("history")
    private List<VisitRecord> history = new ArrayList<>();

    public WebViewData() {
    }

    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        this.bookmarks = bookmarks;
    }

    public List<VisitRecord> getHistory() {
        return history;
    }

    public void setHistory(List<VisitRecord> history) {
        this.history = history;
    }
}
