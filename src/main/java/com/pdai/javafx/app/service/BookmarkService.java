package com.pdai.javafx.app.service;

import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.WebViewData;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class BookmarkService {

    private final JsonPersistenceService persistenceService;

    public BookmarkService(JsonPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public synchronized Bookmark addBookmark(String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        String trimmedUrl = url.trim();
        if (!isValidUrl(trimmedUrl)) {
            throw new IllegalArgumentException("Invalid URL format: " + trimmedUrl);
        }

        WebViewData data = persistenceService.getData();
        for (Bookmark existing : data.getBookmarks()) {
            if (existing.getUrl().equalsIgnoreCase(trimmedUrl)) {
                throw new IllegalStateException("Bookmark already exists for URL: " + trimmedUrl);
            }
        }

        Bookmark bookmark = new Bookmark(title, trimmedUrl);
        data.getBookmarks().add(bookmark);
        persistenceService.saveData();
        return bookmark;
    }

    public synchronized void deleteBookmark(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Bookmark ID cannot be null or empty");
        }

        WebViewData data = persistenceService.getData();
        boolean removed = data.getBookmarks().removeIf(b -> id.equals(b.getId()));
        if (!removed) {
            throw new IllegalArgumentException("Bookmark not found with ID: " + id);
        }
        persistenceService.saveData();
    }

    public synchronized List<Bookmark> getAllBookmarks() {
        WebViewData data = persistenceService.getData();
        List<Bookmark> result = new ArrayList<>(data.getBookmarks());
        Collections.sort(result, new Comparator<Bookmark>() {
            @Override
            public int compare(Bookmark a, Bookmark b) {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });
        return result;
    }

    public synchronized List<Bookmark> searchBookmarks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String lowerQuery = query.trim().toLowerCase();
        WebViewData data = persistenceService.getData();
        List<Bookmark> result = new ArrayList<>();
        for (Bookmark b : data.getBookmarks()) {
            String title = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            String url = b.getUrl() != null ? b.getUrl().toLowerCase() : "";
            if (title.contains(lowerQuery) || url.contains(lowerQuery)) {
                result.add(b);
            }
        }

        Collections.sort(result, new Comparator<Bookmark>() {
            @Override
            public int compare(Bookmark a, Bookmark b) {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
        });
        return result;
    }

    public synchronized boolean isBookmarked(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        String trimmedUrl = url.trim();
        WebViewData data = persistenceService.getData();
        for (Bookmark b : data.getBookmarks()) {
            if (b.getUrl().equalsIgnoreCase(trimmedUrl)) {
                return true;
            }
        }
        return false;
    }

    public synchronized Bookmark findByUrl(String url) {
        if (url == null) return null;
        WebViewData data = persistenceService.getData();
        for (Bookmark b : data.getBookmarks()) {
            if (b.getUrl().equalsIgnoreCase(url.trim())) {
                return b;
            }
        }
        return null;
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
