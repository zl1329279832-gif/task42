package com.pdai.javafx.app.service;

import com.pdai.javafx.app.model.VisitRecord;
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
public class VisitHistoryServiceTest {

    @Mock
    private JsonPersistenceService persistenceService;

    private VisitHistoryService visitHistoryService;
    private WebViewData webViewData;

    @Before
    public void setUp() {
        webViewData = new WebViewData();
        when(persistenceService.getData()).thenReturn(webViewData);
        visitHistoryService = new VisitHistoryService(persistenceService);
    }

    @Test
    public void recordVisit_validInput_addsRecord() {
        visitHistoryService.recordVisit("Bing", "https://www.bing.com");

        assertEquals(1, webViewData.getHistory().size());
        VisitRecord record = webViewData.getHistory().get(0);
        assertEquals("Bing", record.getTitle());
        assertEquals("https://www.bing.com", record.getUrl());
        assertNotNull(record.getVisitedAt());
        verify(persistenceService).saveData();
    }

    @Test
    public void recordVisit_nullTitle_recordsWithEmptyTitle() {
        visitHistoryService.recordVisit(null, "https://www.bing.com");

        assertEquals(1, webViewData.getHistory().size());
        assertEquals("", webViewData.getHistory().get(0).getTitle());
    }

    @Test
    public void recordVisit_nullUrl_doesNotRecord() {
        visitHistoryService.recordVisit("Test", null);

        assertTrue(webViewData.getHistory().isEmpty());
        verify(persistenceService, never()).saveData();
    }

    @Test
    public void recordVisit_emptyUrl_doesNotRecord() {
        visitHistoryService.recordVisit("Test", "");

        assertTrue(webViewData.getHistory().isEmpty());
        verify(persistenceService, never()).saveData();
    }

    @Test
    public void recordVisit_aboutBlank_doesNotRecord() {
        visitHistoryService.recordVisit("Blank", "about:blank");

        assertTrue(webViewData.getHistory().isEmpty());
        verify(persistenceService, never()).saveData();
    }

    @Test
    public void recordVisit_exceedsMaxSize_removesOldest() {
        // Add 100 records
        for (int i = 0; i < 100; i++) {
            visitHistoryService.recordVisit("Page " + i, "https://example.com/page" + i);
        }
        assertEquals(100, webViewData.getHistory().size());

        // Add one more - should trim to 100
        visitHistoryService.recordVisit("Page 100", "https://example.com/page100");
        assertEquals(100, webViewData.getHistory().size());
    }

    @Test
    public void getRecentHistory_returnsNewestFirst() {
        // Use explicit timestamps to guarantee ordering
        webViewData.getHistory().add(new VisitRecord("id-1", "First", "https://www.first.com", "2024-01-01T10:00:00"));
        webViewData.getHistory().add(new VisitRecord("id-2", "Second", "https://www.second.com", "2024-01-01T11:00:00"));
        webViewData.getHistory().add(new VisitRecord("id-3", "Third", "https://www.third.com", "2024-01-01T12:00:00"));

        List<VisitRecord> history = visitHistoryService.getRecentHistory();
        assertEquals(3, history.size());
        assertEquals("Third", history.get(0).getTitle());
        assertEquals("First", history.get(2).getTitle());
    }

    @Test
    public void clearHistory_removesAllRecords() {
        visitHistoryService.recordVisit("First", "https://www.first.com");
        visitHistoryService.recordVisit("Second", "https://www.second.com");
        assertEquals(2, webViewData.getHistory().size());

        visitHistoryService.clearHistory();
        assertTrue(webViewData.getHistory().isEmpty());
    }
}
