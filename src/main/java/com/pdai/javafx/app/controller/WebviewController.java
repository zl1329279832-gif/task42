package com.pdai.javafx.app.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.VisitRecord;
import com.pdai.javafx.app.service.BookmarkService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

@Component
public class WebviewController implements Initializable {

    @FXML
    private WebView webView;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private TextField urlField;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> bookmarkList;

    @FXML
    private ListView<String> historyList;

    @Autowired
    private BookmarkService bookmarkService;

    private WebEngine webEngine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = webView.getEngine();

        webEngine.setOnResized((WebEvent<Rectangle2D> event) -> {
            System.out.println("Window resized");
        });

        // Loading indicator
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                if ("SUCCEEDED".equals(newValue.toString())) {
                    progressIndicator.setVisible(false);
                    // Auto-record visit when page loads successfully
                    String loadedUrl = webEngine.getLocation();
                    String loadedTitle = webEngine.getTitle();
                    if (loadedUrl != null && !loadedUrl.isEmpty()) {
                        try {
                            bookmarkService.recordVisit(loadedTitle, loadedUrl);
                            // Ensure UI update happens on the FX application thread
                            Platform.runLater(this::refreshHistoryList);
                        } catch (IllegalArgumentException e) {
                            // Ignore invalid URLs (e.g., about:blank)
                        }
                    }
                } else {
                    progressIndicator.setVisible(true);
                }
            }
        });

        // URL bar: press Enter to navigate
        if (urlField != null) {
            urlField.setOnAction(event -> {
                String url = urlField.getText();
                if (url != null && !url.trim().isEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        url = "http://" + url;
                    }
                    setUrl(url);
                }
            });
        }

        // Search field: filter bookmarks as you type
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                refreshBookmarkList(newValue);
            });
        }

        // Click on bookmark list to navigate
        if (bookmarkList != null) {
            bookmarkList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String selected = bookmarkList.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        String url = extractUrl(selected);
                        if (url != null) {
                            setUrl(url);
                        }
                    }
                }
            });
        }

        // Click on history list to navigate
        if (historyList != null) {
            historyList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    String selected = historyList.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        String url = extractUrl(selected);
                        if (url != null) {
                            setUrl(url);
                        }
                    }
                }
            });
        }

        // Load initial lists
        refreshBookmarkList(null);
        refreshHistoryList();
    }

    @FXML
    public void clickPageABtn(Event event) throws IOException {
        setUrl("https://www.bing.com");
    }

    @FXML
    public void clickPageBBtn(Event event) throws IOException {
        setUrl("https://www.baidu.com");
    }

    @FXML
    public void onAddBookmark() {
        if (webEngine == null) return;
        String url = webEngine.getLocation();
        String title = webEngine.getTitle();
        if (url == null || url.isEmpty() || "about:blank".equals(url)) {
            showAlert("No page loaded to bookmark.");
            return;
        }
        try {
            bookmarkService.addBookmark(title, url);
            refreshBookmarkList(null);
        } catch (IllegalArgumentException e) {
            showAlert(e.getMessage());
        }
    }

    @FXML
    public void onDeleteBookmark() {
        if (bookmarkList == null) return;
        String selected = bookmarkList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select a bookmark to delete.");
            return;
        }
        String url = extractUrl(selected);
        if (url != null) {
            bookmarkService.deleteBookmark(url);
            refreshBookmarkList(null);
        }
    }

    @FXML
    public void onClearHistory() {
        bookmarkService.clearVisitRecords();
        refreshHistoryList();
    }

    public void setUrl(String url) {
        progressIndicator.setVisible(true);
        if (urlField != null) {
            urlField.setText(url);
        }
        webEngine.load(url);
    }

    private void refreshBookmarkList(String keyword) {
        if (bookmarkList == null) return;
        List<Bookmark> list = bookmarkService.searchBookmarks(keyword);
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Bookmark b : list) {
            items.add(b.getTitle() + " [" + b.getUrl() + "]");
        }
        bookmarkList.setItems(items);
    }

    private void refreshHistoryList() {
        if (historyList == null) return;
        List<VisitRecord> list = bookmarkService.getVisitRecords();
        ObservableList<String> items = FXCollections.observableArrayList();
        for (VisitRecord r : list) {
            items.add(r.getTitle() + " [" + r.getUrl() + "] " + r.getVisitedAt());
        }
        historyList.setItems(items);
    }

    /**
     * Extract URL from a display string like "Title [http://example.com]".
     */
    private String extractUrl(String displayString) {
        if (displayString == null) return null;
        int start = displayString.lastIndexOf('[');
        int end = displayString.indexOf(']', start);
        if (start >= 0 && end > start) {
            return displayString.substring(start + 1, end);
        }
        return null;
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Bookmark");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
