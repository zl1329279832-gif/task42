package com.pdai.javafx.app.service;

import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.WebViewData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BookmarkServiceTest {

    @Mock
    private JsonPersistenceService persistenceService;

    private BookmarkService bookmarkService;
    private WebViewData webViewData;

    @Before
    public void setUp() {
        webViewData = new WebViewData();
        when(persistenceService.getData()).thenReturn(webViewData);
        bookmarkService = new BookmarkService(persistenceService);
    }

    @Test
    public void addBookmark_validInput_createsBookmark() {
        Bookmark result = bookmarkService.addBookmark("Bing", "https://www.bing.com");

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Bing", result.getTitle());
        assertEquals("https://www.bing.com", result.getUrl());
        assertNotNull(result.getCreatedAt());
        assertEquals(1, webViewData.getBookmarks().size());
        verify(persistenceService).saveData();
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBookmark_nullUrl_throwsIllegalArgument() {
        bookmarkService.addBookmark("Test", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBookmark_emptyUrl_throwsIllegalArgument() {
        bookmarkService.addBookmark("Test", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBookmark_blankUrl_throwsIllegalArgument() {
        bookmarkService.addBookmark("Test", "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addBookmark_invalidUrl_throwsIllegalArgument() {
        bookmarkService.addBookmark("Test", "not-a-valid-url");
    }

    @Test(expected = IllegalStateException.class)
    public void addBookmark_duplicateUrl_throwsIllegalState() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");
        bookmarkService.addBookmark("Bing Again", "https://www.bing.com");
    }

    @Test(expected = IllegalStateException.class)
    public void addBookmark_duplicateUrlDifferentCase_throwsIllegalState() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");
        bookmarkService.addBookmark("Bing", "HTTPS://WWW.BING.COM");
    }

    @Test
    public void addBookmark_nullTitle_usesEmptyString() {
        Bookmark result = bookmarkService.addBookmark(null, "https://www.bing.com");
        assertEquals("", result.getTitle());
    }

    @Test
    public void deleteBookmark_existingId_removesBookmark() {
        Bookmark added = bookmarkService.addBookmark("Bing", "https://www.bing.com");
        assertEquals(1, webViewData.getBookmarks().size());

        bookmarkService.deleteBookmark(added.getId());
        assertEquals(0, webViewData.getBookmarks().size());
        verify(persistenceService, times(2)).saveData();
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteBookmark_nonExistentId_throwsIllegalArgument() {
        bookmarkService.deleteBookmark("non-existent-id");
    }

    @Test(expected = IllegalArgumentException.class)
    public void deleteBookmark_nullId_throwsIllegalArgument() {
        bookmarkService.deleteBookmark(null);
    }

    @Test
    public void getAllBookmarks_returnsNewestFirst() {
        // Use explicit timestamps to guarantee ordering
        webViewData.getBookmarks().add(new Bookmark("id-1", "First", "https://www.first.com", "2024-01-01T10:00:00"));
        webViewData.getBookmarks().add(new Bookmark("id-2", "Second", "https://www.second.com", "2024-01-01T11:00:00"));
        webViewData.getBookmarks().add(new Bookmark("id-3", "Third", "https://www.third.com", "2024-01-01T12:00:00"));

        List<Bookmark> all = bookmarkService.getAllBookmarks();
        assertEquals(3, all.size());
        assertEquals("Third", all.get(0).getTitle());
        assertEquals("First", all.get(2).getTitle());
    }

    @Test
    public void searchBookmarks_matchesTitle_caseInsensitive() {
        bookmarkService.addBookmark("Bing Search", "https://www.bing.com");
        bookmarkService.addBookmark("Baidu", "https://www.baidu.com");

        List<Bookmark> results = bookmarkService.searchBookmarks("bing");
        assertEquals(1, results.size());
        assertEquals("Bing Search", results.get(0).getTitle());
    }

    @Test
    public void searchBookmarks_matchesUrl_caseInsensitive() {
        bookmarkService.addBookmark("Search Engine", "https://www.bing.com");
        bookmarkService.addBookmark("Baidu", "https://www.baidu.com");

        List<Bookmark> results = bookmarkService.searchBookmarks("baidu");
        assertEquals(1, results.size());
        assertEquals("Baidu", results.get(0).getTitle());
    }

    @Test
    public void searchBookmarks_noMatch_returnsEmpty() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");

        List<Bookmark> results = bookmarkService.searchBookmarks("google");
        assertTrue(results.isEmpty());
    }

    @Test
    public void searchBookmarks_nullQuery_returnsEmpty() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");
        assertTrue(bookmarkService.searchBookmarks(null).isEmpty());
    }

    @Test
    public void searchBookmarks_emptyQuery_returnsEmpty() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");
        assertTrue(bookmarkService.searchBookmarks("").isEmpty());
    }

    @Test
    public void isBookmarked_existingUrl_returnsTrue() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");
        assertTrue(bookmarkService.isBookmarked("https://www.bing.com"));
    }

    @Test
    public void isBookmarked_differentCase_returnsTrue() {
        bookmarkService.addBookmark("Bing", "https://www.bing.com");
        assertTrue(bookmarkService.isBookmarked("HTTPS://WWW.BING.COM"));
    }

    @Test
    public void isBookmarked_nonExistentUrl_returnsFalse() {
        assertFalse(bookmarkService.isBookmarked("https://www.google.com"));
    }

    @Test
    public void isBookmarked_nullUrl_returnsFalse() {
        assertFalse(bookmarkService.isBookmarked(null));
    }

    @Test
    public void findByUrl_existingUrl_returnsBookmark() {
        Bookmark added = bookmarkService.addBookmark("Bing", "https://www.bing.com");
        Bookmark found = bookmarkService.findByUrl("https://www.bing.com");
        assertNotNull(found);
        assertEquals(added.getId(), found.getId());
    }

    @Test
    public void findByUrl_nonExistentUrl_returnsNull() {
        assertNull(bookmarkService.findByUrl("https://www.google.com"));
    }
}
