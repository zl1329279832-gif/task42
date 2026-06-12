package com.pdai.javafx.app.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.BookmarkData;
import com.pdai.javafx.app.model.VisitRecord;

/**
 * Unit tests for BookmarkService. No JavaFX window or Spring context needed.
 */
public class BookmarkServiceTest {

    private BookmarkService service;
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("bookmark-test-", ".json");
        tempFile.deleteOnExit();
        // Delete so service starts with empty state
        tempFile.delete();
        service = new BookmarkService(new ObjectMapper(), tempFile);
        // Manually call init since we're not using Spring
        service.init();
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
        // Clean up corrupted backup if exists
        File backup = new File(tempFile.getAbsolutePath() + ".corrupted");
        if (backup.exists()) {
            backup.delete();
        }
    }

    // ==================== Bookmark Tests ====================

    @Test
    public void testAddBookmark() {
        Bookmark b = service.addBookmark("Google", "https://www.google.com");
        assertNotNull(b);
        assertEquals("Google", b.getTitle());
        assertEquals("https://www.google.com", b.getUrl());
        assertNotNull(b.getCreatedAt());

        List<Bookmark> all = service.getAllBookmarks();
        assertEquals(1, all.size());
        assertEquals("https://www.google.com", all.get(0).getUrl());
    }

    @Test
    public void testAddBookmark_nullTitle_usesUrlAsTitle() {
        Bookmark b = service.addBookmark(null, "https://www.google.com");
        assertEquals("https://www.google.com", b.getTitle());
    }

    @Test
    public void testAddBookmark_emptyTitle_usesUrlAsTitle() {
        Bookmark b = service.addBookmark("  ", "https://www.google.com");
        assertEquals("https://www.google.com", b.getTitle());
    }

    @Test
    public void testAddBookmark_duplicate_throwsException() {
        service.addBookmark("Google", "https://www.google.com");
        try {
            service.addBookmark("Google Again", "https://www.google.com");
            fail("Should have thrown IllegalArgumentException for duplicate URL");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void testAddBookmark_invalidUrl_throwsException() {
        try {
            service.addBookmark("Bad URL", "not-a-valid-url");
            fail("Should have thrown IllegalArgumentException for invalid URL");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid URL"));
        }
    }

    @Test
    public void testAddBookmark_nullUrl_throwsException() {
        try {
            service.addBookmark("Null URL", null);
            fail("Should have thrown IllegalArgumentException for null URL");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null or empty"));
        }
    }

    @Test
    public void testAddBookmark_emptyUrl_throwsException() {
        try {
            service.addBookmark("Empty URL", "");
            fail("Should have thrown IllegalArgumentException for empty URL");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("null or empty"));
        }
    }

    @Test
    public void testDeleteBookmark() {
        service.addBookmark("Google", "https://www.google.com");
        service.addBookmark("Bing", "https://www.bing.com");

        boolean result = service.deleteBookmark("https://www.google.com");
        assertTrue(result);

        List<Bookmark> all = service.getAllBookmarks();
        assertEquals(1, all.size());
        assertEquals("https://www.bing.com", all.get(0).getUrl());
    }

    @Test
    public void testDeleteBookmark_nonExistent_returnsFalse() {
        boolean result = service.deleteBookmark("https://www.nonexistent.com");
        assertFalse(result);
    }

    @Test
    public void testDeleteBookmark_invalidUrl_throwsException() {
        try {
            service.deleteBookmark("bad-url");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testSearchBookmarks_byTitle() {
        service.addBookmark("Google Search", "https://www.google.com");
        service.addBookmark("Bing Search", "https://www.bing.com");
        service.addBookmark("GitHub", "https://github.com");

        List<Bookmark> results = service.searchBookmarks("Search");
        assertEquals(2, results.size());
    }

    @Test
    public void testSearchBookmarks_byUrl() {
        service.addBookmark("Google", "https://www.google.com");
        service.addBookmark("Bing", "https://www.bing.com");

        List<Bookmark> results = service.searchBookmarks("google");
        assertEquals(1, results.size());
        assertEquals("Google", results.get(0).getTitle());
    }

    @Test
    public void testSearchBookmarks_caseInsensitive() {
        service.addBookmark("Google", "https://www.google.com");

        List<Bookmark> results = service.searchBookmarks("GOOGLE");
        assertEquals(1, results.size());
    }

    @Test
    public void testSearchBookmarks_emptyKeyword_returnsAll() {
        service.addBookmark("Google", "https://www.google.com");
        service.addBookmark("Bing", "https://www.bing.com");

        List<Bookmark> results = service.searchBookmarks("");
        assertEquals(2, results.size());
    }

    @Test
    public void testSearchBookmarks_nullKeyword_returnsAll() {
        service.addBookmark("Google", "https://www.google.com");

        List<Bookmark> results = service.searchBookmarks(null);
        assertEquals(1, results.size());
    }

    @Test
    public void testSearchBookmarks_noMatch_returnsEmpty() {
        service.addBookmark("Google", "https://www.google.com");

        List<Bookmark> results = service.searchBookmarks("xyz123");
        assertTrue(results.isEmpty());
    }

    // ==================== Visit Record Tests ====================

    @Test
    public void testRecordVisit() {
        VisitRecord r = service.recordVisit("Google", "https://www.google.com");
        assertNotNull(r);
        assertEquals("Google", r.getTitle());
        assertEquals("https://www.google.com", r.getUrl());
        assertNotNull(r.getVisitedAt());

        List<VisitRecord> all = service.getVisitRecords();
        assertEquals(1, all.size());
    }

    @Test
    public void testRecordVisit_mostRecentFirst() {
        service.recordVisit("First", "https://first.com");
        service.recordVisit("Second", "https://second.com");

        List<VisitRecord> all = service.getVisitRecords();
        assertEquals(2, all.size());
        assertEquals("https://second.com", all.get(0).getUrl());
        assertEquals("https://first.com", all.get(1).getUrl());
    }

    @Test
    public void testRecordVisit_sameUrlAllowed() {
        // Unlike bookmarks, visiting the same URL multiple times should be allowed
        service.recordVisit("Google", "https://www.google.com");
        service.recordVisit("Google", "https://www.google.com");

        List<VisitRecord> all = service.getVisitRecords();
        assertEquals(2, all.size());
    }

    @Test
    public void testRecordVisit_nullTitle_usesUrl() {
        VisitRecord r = service.recordVisit(null, "https://www.google.com");
        assertEquals("https://www.google.com", r.getTitle());
    }

    @Test
    public void testRecordVisit_invalidUrl_throwsException() {
        try {
            service.recordVisit("Bad", "not-a-url");
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testRecordVisit_maxLimit() {
        // Fill beyond the max limit
        for (int i = 0; i < service.getMaxVisitRecords() + 50; i++) {
            service.recordVisit("Page " + i, "https://page" + i + ".com");
        }

        List<VisitRecord> all = service.getVisitRecords();
        assertEquals(service.getMaxVisitRecords(), all.size());
        // Most recent should be first
        assertEquals("https://page" + (service.getMaxVisitRecords() + 49) + ".com", all.get(0).getUrl());
    }

    @Test
    public void testClearVisitRecords() {
        service.recordVisit("Google", "https://www.google.com");
        service.recordVisit("Bing", "https://www.bing.com");

        service.clearVisitRecords();

        List<VisitRecord> all = service.getVisitRecords();
        assertTrue(all.isEmpty());
    }

    @Test
    public void testClearAll() {
        service.addBookmark("Google", "https://www.google.com");
        service.recordVisit("Bing", "https://www.bing.com");

        service.clearAll();

        assertTrue(service.getAllBookmarks().isEmpty());
        assertTrue(service.getVisitRecords().isEmpty());
    }

    // ==================== Persistence Tests ====================

    @Test
    public void testPersistence_saveAndLoad() {
        service.addBookmark("Google", "https://www.google.com");
        service.recordVisit("Bing", "https://www.bing.com");

        // Create a new service pointing to the same file
        BookmarkService service2 = new BookmarkService(new ObjectMapper(), tempFile);
        service2.init();

        List<Bookmark> bookmarks = service2.getAllBookmarks();
        assertEquals(1, bookmarks.size());
        assertEquals("Google", bookmarks.get(0).getTitle());

        List<VisitRecord> visits = service2.getVisitRecords();
        assertEquals(1, visits.size());
        assertEquals("Bing", visits.get(0).getTitle());
    }

    @Test
    public void testLoad_noFile_startsWithEmptyData() {
        // tempFile was already deleted in setUp, so init should succeed with empty data
        assertTrue(service.getAllBookmarks().isEmpty());
        assertTrue(service.getVisitRecords().isEmpty());
    }

    @Test
    public void testLoad_corruptedFile_resetsAndBacksUp() throws IOException {
        // Write garbage to the file
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{ this is not valid json !!!");
        }

        service.load();

        // Should have empty data
        assertTrue(service.getAllBookmarks().isEmpty());
        assertTrue(service.getVisitRecords().isEmpty());

        // Corrupted file should have been backed up
        File backup = new File(tempFile.getAbsolutePath() + ".corrupted");
        assertTrue("Corrupted file should be backed up", backup.exists());
    }

    @Test
    public void testLoad_emptyJsonFile_handledGracefully() throws IOException {
        // Write an empty JSON object
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{}");
        }

        service.load();

        assertTrue(service.getAllBookmarks().isEmpty());
        assertTrue(service.getVisitRecords().isEmpty());
    }

    @Test
    public void testLoad_partialData_handledGracefully() throws IOException {
        // Write JSON with only bookmarks, no visitRecords field
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\"bookmarks\":[{\"title\":\"Test\",\"url\":\"https://test.com\",\"createdAt\":\"2024-01-01 00:00:00\"}]}");
        }

        service.load();

        assertEquals(1, service.getAllBookmarks().size());
        assertTrue(service.getVisitRecords().isEmpty());
    }

    @Test
    public void testSave_createsParentDirectory() throws IOException {
        // Use a file in a nested directory that doesn't exist
        File nestedFile = new File(tempFile.getParentFile(), "subdir/nested/bookmarks.json");
        nestedFile.getParentFile().deleteOnExit();
        nestedFile.deleteOnExit();

        BookmarkService nestedService = new BookmarkService(new ObjectMapper(), nestedFile);
        nestedService.addBookmark("Test", "https://test.com");

        assertTrue(nestedFile.exists());

        // Clean up
        nestedFile.delete();
        nestedFile.getParentFile().delete();
        nestedFile.getParentFile().getParentFile().delete();
    }

    // ==================== Concurrency Tests ====================

    @Test
    public void testConcurrentBookmarkAdds() throws InterruptedException {
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                service.addBookmark("Site " + idx, "https://site" + idx + ".com");
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        List<Bookmark> all = service.getAllBookmarks();
        assertEquals(10, all.size());
    }

    @Test
    public void testConcurrentVisitRecords() throws InterruptedException {
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                service.recordVisit("Page " + idx, "https://page" + idx + ".com");
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        List<VisitRecord> all = service.getVisitRecords();
        assertEquals(20, all.size());
    }

    @Test
    public void testConcurrentAddAndDelete() throws InterruptedException {
        // Pre-add some bookmarks
        for (int i = 0; i < 10; i++) {
            service.addBookmark("Site " + i, "https://site" + i + ".com");
        }

        Thread[] threads = new Thread[10];
        // Delete even ones, add new ones
        for (int i = 0; i < threads.length; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                if (idx % 2 == 0) {
                    service.deleteBookmark("https://site" + idx + ".com");
                } else {
                    service.addBookmark("New " + idx, "https://new" + idx + ".com");
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        List<Bookmark> all = service.getAllBookmarks();
        // 5 odd originals kept + 5 new odd ones added = 10
        assertEquals(10, all.size());
    }

    // ==================== getAllBookmarks Returns Copy Test ====================

    @Test
    public void testGetAllBookmarks_returnsUnmodifiableCopy() {
        service.addBookmark("Google", "https://www.google.com");

        List<Bookmark> all = service.getAllBookmarks();
        try {
            all.add(new Bookmark("Hack", "https://hack.com"));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected - list is unmodifiable
        }
    }

    @Test
    public void testGetVisitRecords_returnsUnmodifiableCopy() {
        service.recordVisit("Google", "https://www.google.com");

        List<VisitRecord> all = service.getVisitRecords();
        try {
            all.add(new VisitRecord("Hack", "https://hack.com"));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected - list is unmodifiable
        }
    }
}
