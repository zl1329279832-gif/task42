package com.pdai.javafx.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.VisitRecord;
import com.pdai.javafx.app.model.WebViewData;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class JsonPersistenceServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ObjectMapper objectMapper;
    private File dataFile;
    private JsonPersistenceService service;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        dataFile = new File(tempFolder.getRoot(), "webview-data.json");
        service = new JsonPersistenceService(objectMapper, dataFile);
    }

    @Test
    public void init_noFile_createsEmptyData() {
        assertFalse(dataFile.exists());
        service.init();

        WebViewData data = service.getData();
        assertNotNull(data);
        assertTrue(data.getBookmarks().isEmpty());
        assertTrue(data.getHistory().isEmpty());
    }

    @Test
    public void saveAndLoad_roundTrip_preservesData() {
        service.init();

        WebViewData data = service.getData();
        data.getBookmarks().add(new Bookmark("Test", "https://example.com"));
        data.getHistory().add(new VisitRecord("Visit", "https://visit.com"));
        service.saveData();

        // Create a new service instance and load from the same file
        JsonPersistenceService service2 = new JsonPersistenceService(objectMapper, dataFile);
        service2.init();

        WebViewData loaded = service2.getData();
        assertEquals(1, loaded.getBookmarks().size());
        assertEquals("Test", loaded.getBookmarks().get(0).getTitle());
        assertEquals("https://example.com", loaded.getBookmarks().get(0).getUrl());
        assertEquals(1, loaded.getHistory().size());
        assertEquals("Visit", loaded.getHistory().get(0).getTitle());
        assertEquals("https://visit.com", loaded.getHistory().get(0).getUrl());
    }

    @Test
    public void init_corruptFile_createsBackupAndStartsFresh() throws Exception {
        // Write invalid JSON to the file
        FileWriter writer = new FileWriter(dataFile);
        writer.write("{invalid json content!!!");
        writer.close();
        assertTrue(dataFile.exists());

        service.init();

        WebViewData data = service.getData();
        assertNotNull(data);
        assertTrue(data.getBookmarks().isEmpty());
        assertTrue(data.getHistory().isEmpty());

        // Verify backup file was created
        File[] files = tempFolder.getRoot().listFiles();
        assertNotNull(files);
        boolean backupFound = false;
        for (File f : files) {
            if (f.getName().startsWith("webview-data.json.bak.")) {
                backupFound = true;
                break;
            }
        }
        assertTrue("Backup file should have been created", backupFound);
    }

    @Test
    public void init_emptyFile_treatsAsCorrupt() throws Exception {
        // Create an empty file
        assertTrue(dataFile.createNewFile());
        assertEquals(0, dataFile.length());

        service.init();

        WebViewData data = service.getData();
        assertNotNull(data);
        assertTrue(data.getBookmarks().isEmpty());
    }

    @Test
    public void saveData_writesValidJson() throws Exception {
        service.init();

        WebViewData data = service.getData();
        Bookmark bookmark = new Bookmark("id-1", "Test Bookmark", "https://test.com", "2024-01-01T10:00:00");
        data.getBookmarks().add(bookmark);
        service.saveData();

        assertTrue(dataFile.exists());

        // Read the file and parse it
        WebViewData fromFile = objectMapper.readValue(dataFile, WebViewData.class);
        assertEquals(1, fromFile.getBookmarks().size());
        assertEquals("Test Bookmark", fromFile.getBookmarks().get(0).getTitle());
        assertEquals("https://test.com", fromFile.getBookmarks().get(0).getUrl());
        assertEquals("id-1", fromFile.getBookmarks().get(0).getId());
    }

    @Test
    public void concurrentReadWrite_noCorruption() throws Exception {
        service.init();

        int threadCount = 10;
        int operationsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> errors = new ArrayList<>();

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < operationsPerThread; i++) {
                            WebViewData data = service.getData();
                            assertNotNull(data);
                            assertNotNull(data.getBookmarks());
                            assertNotNull(data.getHistory());

                            // Add a record and save
                            synchronized (data.getHistory()) {
                                data.getHistory().add(new VisitRecord(
                                        "Thread " + threadId + " Visit " + i,
                                        "https://example.com/t" + threadId + "/p" + i));
                            }
                            service.saveData();
                        }
                    } catch (Exception e) {
                        synchronized (errors) {
                            errors.add(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        assertTrue("Timed out waiting for threads", latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue("Errors occurred during concurrent access: " + errors, errors.isEmpty());

        // Data should still be readable
        WebViewData finalData = service.getData();
        assertNotNull(finalData);
        assertTrue("Should have recorded visits", finalData.getHistory().size() > 0);

        // Verify file is still valid JSON
        WebViewData fromFile = objectMapper.readValue(dataFile, WebViewData.class);
        assertNotNull(fromFile);
    }
}
