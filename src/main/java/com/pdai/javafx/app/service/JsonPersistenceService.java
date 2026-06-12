package com.pdai.javafx.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdai.javafx.app.model.WebViewData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class JsonPersistenceService {

    private static final Logger LOG = LoggerFactory.getLogger(JsonPersistenceService.class);

    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final File dataFile;
    private WebViewData cachedData;

    public JsonPersistenceService() {
        this.objectMapper = new ObjectMapper();
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, ".springfxapp");
        this.dataFile = new File(appDir, "webview-data.json");
    }

    JsonPersistenceService(ObjectMapper objectMapper, File dataFile) {
        this.objectMapper = objectMapper;
        this.dataFile = dataFile;
    }

    @PostConstruct
    public void init() {
        loadData();
    }

    public WebViewData getData() {
        lock.readLock().lock();
        try {
            return cachedData;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void saveData() {
        lock.writeLock().lock();
        try {
            File parentDir = dataFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            File tempFile = new File(dataFile.getParent(), dataFile.getName() + ".tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(tempFile, cachedData);

            if (dataFile.exists()) {
                dataFile.delete();
            }
            if (!tempFile.renameTo(dataFile)) {
                // Fallback: if rename fails (can happen on some Windows setups), copy directly
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, cachedData);
                tempFile.delete();
            }
        } catch (IOException e) {
            LOG.error("Failed to save data to {}", dataFile.getAbsolutePath(), e);
            throw new RuntimeException("Failed to persist webview data", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadData() {
        if (!dataFile.exists()) {
            LOG.info("No data file found at {}, starting fresh", dataFile.getAbsolutePath());
            cachedData = new WebViewData();
            return;
        }

        if (dataFile.length() == 0) {
            LOG.warn("Data file is empty, starting fresh");
            handleCorruptFile();
            cachedData = new WebViewData();
            return;
        }

        try {
            cachedData = objectMapper.readValue(dataFile, WebViewData.class);
            LOG.info("Loaded {} bookmarks and {} history records",
                    cachedData.getBookmarks().size(), cachedData.getHistory().size());
        } catch (Exception e) {
            LOG.warn("Data file is corrupted, backing up and starting fresh: {}", e.getMessage());
            handleCorruptFile();
            cachedData = new WebViewData();
        }
    }

    private void handleCorruptFile() {
        try {
            String backupName = dataFile.getName() + ".bak." + System.currentTimeMillis();
            File backupFile = new File(dataFile.getParent(), backupName);
            if (dataFile.renameTo(backupFile)) {
                LOG.info("Corrupt data file backed up to {}", backupFile.getAbsolutePath());
            } else {
                LOG.warn("Failed to backup corrupt data file");
                dataFile.delete();
            }
        } catch (Exception e) {
            LOG.error("Failed to handle corrupt file", e);
        }
    }
}
