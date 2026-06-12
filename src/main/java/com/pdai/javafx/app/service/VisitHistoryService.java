package com.pdai.javafx.app.service;

import com.pdai.javafx.app.model.VisitRecord;
import com.pdai.javafx.app.model.WebViewData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class VisitHistoryService {

    private static final int MAX_HISTORY_SIZE = 100;

    private final JsonPersistenceService persistenceService;

    public VisitHistoryService(JsonPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    public synchronized void recordVisit(String title, String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        String trimmedUrl = url.trim();
        if ("about:blank".equals(trimmedUrl)) {
            return;
        }

        WebViewData data = persistenceService.getData();
        VisitRecord record = new VisitRecord(title, trimmedUrl);
        data.getHistory().add(record);

        // Trim to max size, removing oldest entries
        while (data.getHistory().size() > MAX_HISTORY_SIZE) {
            // Find the oldest entry and remove it
            VisitRecord oldest = null;
            for (VisitRecord r : data.getHistory()) {
                if (oldest == null || (r.getVisitedAt() != null && oldest.getVisitedAt() != null
                        && r.getVisitedAt().compareTo(oldest.getVisitedAt()) < 0)) {
                    oldest = r;
                }
            }
            if (oldest != null) {
                data.getHistory().remove(oldest);
            } else {
                data.getHistory().remove(0);
            }
        }

        persistenceService.saveData();
    }

    public synchronized List<VisitRecord> getRecentHistory() {
        WebViewData data = persistenceService.getData();
        List<VisitRecord> result = new ArrayList<>(data.getHistory());
        Collections.sort(result, new Comparator<VisitRecord>() {
            @Override
            public int compare(VisitRecord a, VisitRecord b) {
                if (a.getVisitedAt() == null && b.getVisitedAt() == null) return 0;
                if (a.getVisitedAt() == null) return 1;
                if (b.getVisitedAt() == null) return -1;
                return b.getVisitedAt().compareTo(a.getVisitedAt());
            }
        });
        return result;
    }

    public synchronized void clearHistory() {
        WebViewData data = persistenceService.getData();
        data.getHistory().clear();
        persistenceService.saveData();
    }
}
