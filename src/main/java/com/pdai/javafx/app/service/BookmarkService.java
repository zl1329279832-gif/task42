package com.pdai.javafx.app.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.BookmarkData;
import com.pdai.javafx.app.model.VisitRecord;

import javax.annotation.PostConstruct;

/**
 * BookmarkService provides bookmark management and visit history tracking.
 * Data is persisted to a local JSON file.
 *
 * Features:
 * - Add/delete/search bookmarks
 * - Auto-record visit history
 * - Clean old visit records
 * - Load history on startup
 * - Handle corrupted config files gracefully
 * - Thread-safe concurrent save operations
 */
@Service
public class BookmarkService {

    private static final Logger log = LoggerFactory.getLogger(BookmarkService.class);

    private static final int MAX_VISIT_RECORDS = 200;

    private final ObjectMapper objectMapper;
    private final File storageFile;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private List<Bookmark> bookmarks;
    private List<VisitRecord> visitRecords;

    /**
     * Default constructor: uses default storage path and a new ObjectMapper.
     */
    public BookmarkService() {
        this(new ObjectMapper(), getDefaultStorageFile());
    }

    /**
     * Constructor with custom ObjectMapper and storage file (for testing).
     */
    public BookmarkService(ObjectMapper objectMapper, File storageFile) {
        this.objectMapper = objectMapper;
        this.storageFile = storageFile;
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.bookmarks = new ArrayList<>();
        this.visitRecords = new ArrayList<>();
    }

    /**
     * Load persisted data on startup.
     */
    @PostConstruct
    public void init() {
        load();
    }

    // ==================== Bookmark Operations ====================

    /**
     * Add a bookmark. Validates URL and checks for duplicates.
     *
     * @param title the page title
     * @param url   the page URL
     * @return the created Bookmark
     * @throws IllegalArgumentException if URL is invalid or bookmark already exists
     */
    public Bookmark addBookmark(String title, String url) {
        validateUrl(url);

        if (title == null || title.trim().isEmpty()) {
            title = url;
        }
        title = title.trim();

        Bookmark newBookmark = new Bookmark(title, url.trim());

        lock.writeLock().lock();
        try {
            // Check for duplicate (case-insensitive URL comparison)
            String normalizedUrl = url.trim().toLowerCase();
            for (Bookmark b : bookmarks) {
                if (b.getUrl() != null && b.getUrl().toLowerCase().equals(normalizedUrl)) {
                    throw new IllegalArgumentException("Bookmark already exists: " + url);
                }
            }
            bookmarks.add(newBookmark);
            save();
        } finally {
            lock.writeLock().unlock();
        }

        log.info("Bookmark added: {} -> {}", title, url);
        return newBookmark;
    }

    /**
     * Delete a bookmark by URL.
     *
     * @param url the bookmark URL to delete
     * @return true if a bookmark was removed
     * @throws IllegalArgumentException if URL is invalid
     */
    public boolean deleteBookmark(String url) {
        validateUrl(url);

        lock.writeLock().lock();
        try {
            String normalizedUrl = url.trim().toLowerCase();
            boolean removed = bookmarks.removeIf(b -> b.getUrl() != null
                    && b.getUrl().toLowerCase().equals(normalizedUrl));
            if (removed) {
                save();
                log.info("Bookmark deleted: {}", url);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Search bookmarks by title or URL (case-insensitive, partial match).
     *
     * @param keyword the search keyword
     * @return list of matching bookmarks
     */
    public List<Bookmark> searchBookmarks(String keyword) {
        lock.readLock().lock();
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return Collections.unmodifiableList(new ArrayList<>(bookmarks));
            }
            String lowerKeyword = keyword.trim().toLowerCase();
            return bookmarks.stream()
                    .filter(b -> (b.getTitle() != null && b.getTitle().toLowerCase().contains(lowerKeyword))
                            || (b.getUrl() != null && b.getUrl().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get all bookmarks.
     *
     * @return unmodifiable list of all bookmarks
     */
    public List<Bookmark> getAllBookmarks() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(bookmarks));
        } finally {
            lock.readLock().unlock();
        }
    }

    // ==================== Visit Record Operations ====================

    /**
     * Record a page visit. Automatically limits history size.
     *
     * @param title the page title
     * @param url   the page URL
     * @return the created VisitRecord
     * @throws IllegalArgumentException if URL is invalid
     */
    public VisitRecord recordVisit(String title, String url) {
        validateUrl(url);

        if (title == null || title.trim().isEmpty()) {
            title = url;
        }
        title = title.trim();

        VisitRecord record = new VisitRecord(title, url.trim());

        lock.writeLock().lock();
        try {
            // Deduplicate: if same URL already in history, remove old entry
            // (prevents repeated refreshes of the same page from filling up the limit)
            String normalizedUrl = url.trim().toLowerCase();
            visitRecords.removeIf(r -> r.getUrl() != null
                    && r.getUrl().toLowerCase().equals(normalizedUrl));

            // Add to the front (most recent first)
            visitRecords.add(0, record);

            // Trim to max size
            while (visitRecords.size() > MAX_VISIT_RECORDS) {
                visitRecords.remove(visitRecords.size() - 1);
            }

            save();
        } finally {
            lock.writeLock().unlock();
        }

        log.info("Visit recorded: {} -> {}", title, url);
        return record;
    }

    /**
     * Get all visit records (most recent first).
     *
     * @return unmodifiable list of visit records
     */
    public List<VisitRecord> getVisitRecords() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(visitRecords));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clear all visit records.
     */
    public void clearVisitRecords() {
        lock.writeLock().lock();
        try {
            visitRecords.clear();
            save();
        } finally {
            lock.writeLock().unlock();
        }
        log.info("Visit records cleared");
    }

    /**
     * Clear all bookmarks and visit records.
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            bookmarks.clear();
            visitRecords.clear();
            save();
        } finally {
            lock.writeLock().unlock();
        }
        log.info("All data cleared");
    }

    // ==================== Persistence ====================

    /**
     * Load data from JSON file. Handles corrupted files gracefully by
     * resetting to empty state and backing up the corrupted file.
     */
    public void load() {
        if (!storageFile.exists()) {
            log.info("No storage file found at {}, starting with empty data", storageFile.getAbsolutePath());
            return;
        }

        lock.writeLock().lock();
        try {
            BookmarkData data = objectMapper.readValue(storageFile, BookmarkData.class);
            this.bookmarks = data.getBookmarks() != null ? data.getBookmarks() : new ArrayList<>();
            this.visitRecords = data.getVisitRecords() != null ? data.getVisitRecords() : new ArrayList<>();
            log.info("Loaded {} bookmarks and {} visit records", bookmarks.size(), visitRecords.size());
        } catch (IOException e) {
            log.error("Failed to load bookmark data from {}. Backing up corrupted file and starting fresh.",
                    storageFile.getAbsolutePath(), e);
            backupCorruptedFile();
            this.bookmarks = new ArrayList<>();
            this.visitRecords = new ArrayList<>();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Save current data to JSON file. Uses atomic write (temp file + rename)
     * to prevent data loss if the process is interrupted mid-write.
     * Note: caller must already hold writeLock.
     */
    public void save() {
        // Note: caller must already hold writeLock
        try {
            File parentDir = storageFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.error("Failed to create storage directory: {}", parentDir.getAbsolutePath());
                    return;
                }
            }

            // Write to a temp file first, then atomically move to prevent
            // data loss if the JVM exits or the write is interrupted mid-flush.
            File tempFile = new File(storageFile.getAbsolutePath() + ".tmp");
            BookmarkData data = new BookmarkData(bookmarks, visitRecords);
            objectMapper.writeValue(tempFile, data);

            // Use Files.move with REPLACE_EXISTING (works on all platforms;
            // File.renameTo fails on Windows when the target already exists).
            Files.move(tempFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save bookmark data to {}", storageFile.getAbsolutePath(), e);
        }
    }

    // ==================== Internals ====================

    /**
     * Validate that a URL string is well-formed.
     */
    void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        try {
            new URL(url.trim());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private void backupCorruptedFile() {
        try {
            File backup = new File(storageFile.getAbsolutePath() + ".corrupted");
            if (storageFile.renameTo(backup)) {
                log.info("Corrupted file backed up to: {}", backup.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to backup corrupted file", e);
        }
    }

    private static File getDefaultStorageFile() {
        String userHome = System.getProperty("user.home");
        return new File(userHome, ".bookmark-app" + File.separator + "bookmarks.json");
    }

    // Visible for testing
    File getStorageFile() {
        return storageFile;
    }

    int getMaxVisitRecords() {
        return MAX_VISIT_RECORDS;
    }
}
