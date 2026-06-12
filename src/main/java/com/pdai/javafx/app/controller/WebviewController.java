package com.pdai.javafx.app.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.pdai.javafx.app.model.Bookmark;
import com.pdai.javafx.app.model.VisitRecord;
import com.pdai.javafx.app.service.BookmarkService;
import com.pdai.javafx.app.service.VisitHistoryService;
import com.pdai.javafx.app.utils.SpringUtils;

import org.springframework.stereotype.Component;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Callback;

@Component
public class WebviewController implements Initializable {

	@FXML
	private WebView webView;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private JFXButton bookmarkBtn;

	@FXML
	private JFXButton showBookmarksBtn;

	@FXML
	private JFXButton showHistoryBtn;

	@FXML
	private VBox sidePanel;

	@FXML
	private Label sidePanelTitle;

	@FXML
	private JFXButton clearHistoryBtn;

	@FXML
	private JFXButton closePanelBtn;

	@FXML
	private JFXTextField searchField;

	@FXML
	private ListView<Object> listView;

	private WebEngine webEngine;
	private ChangeListener<Object> stateListener;
	private BookmarkService bookmarkService;
	private VisitHistoryService visitHistoryService;
	private boolean showingBookmarks = true;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		webEngine = webView.getEngine();

		// Get services from Spring context
		bookmarkService = SpringUtils.getBean(BookmarkService.class);
		visitHistoryService = SpringUtils.getBean(VisitHistoryService.class);

		// Remove old listener if re-initialized (singleton controller)
		if (stateListener != null) {
			webEngine.getLoadWorker().stateProperty().removeListener(stateListener);
		}

		stateListener = new ChangeListener<Object>() {
			@Override
			public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
				if ("SUCCEEDED".equals(newValue.toString())) {
					progressIndicator.setVisible(false);
					recordCurrentPageVisit();
					updateBookmarkButtonState();
				} else {
					progressIndicator.setVisible(true);
				}
			}
		};

		webEngine.getLoadWorker().stateProperty().addListener(stateListener);

		// Search field listener
		searchField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (showingBookmarks && sidePanel.isVisible()) {
					refreshBookmarkList(newValue);
				}
			}
		});

		// List view cell factory
		setupListViewCellFactory();
	}

	@FXML
	public void clickPageABtn(Event event) throws IOException {
		setUrl("https://www.bing.com");
	}

	@FXML
	public void clickPageBBtn(Event event) throws IOException {
		setUrl("https://www.baidu.com");
	}

	public void setUrl(String url) {
		progressIndicator.setVisible(true);
		webEngine.load(url);
	}

	@FXML
	private void onBookmarkClick(ActionEvent event) {
		String url = webEngine.getLocation();
		String title = webEngine.getTitle();

		if (url == null || url.isEmpty() || "about:blank".equals(url)) {
			return;
		}

		try {
			if (bookmarkService.isBookmarked(url)) {
				Bookmark existing = bookmarkService.findByUrl(url);
				if (existing != null) {
					bookmarkService.deleteBookmark(existing.getId());
				}
			} else {
				bookmarkService.addBookmark(title != null ? title : "", url);
			}
			updateBookmarkButtonState();
			if (sidePanel.isVisible() && showingBookmarks) {
				refreshBookmarkList(searchField.getText());
			}
		} catch (IllegalArgumentException | IllegalStateException e) {
			showError(e.getMessage());
		}
	}

	@FXML
	private void onShowBookmarks(ActionEvent event) {
		showingBookmarks = true;
		sidePanelTitle.setText("Bookmarks");
		searchField.setVisible(true);
		searchField.setManaged(true);
		clearHistoryBtn.setVisible(false);
		clearHistoryBtn.setManaged(false);
		sidePanel.setVisible(true);
		sidePanel.setManaged(true);
		setupListViewCellFactory();
		refreshBookmarkList(searchField.getText());
	}

	@FXML
	private void onShowHistory(ActionEvent event) {
		showingBookmarks = false;
		sidePanelTitle.setText("Recent History");
		searchField.setVisible(false);
		searchField.setManaged(false);
		clearHistoryBtn.setVisible(true);
		clearHistoryBtn.setManaged(true);
		sidePanel.setVisible(true);
		sidePanel.setManaged(true);
		setupListViewCellFactory();
		refreshHistoryList();
	}

	@FXML
	private void onClosePanel(ActionEvent event) {
		sidePanel.setVisible(false);
		sidePanel.setManaged(false);
	}

	@FXML
	private void onClearHistory(ActionEvent event) {
		visitHistoryService.clearHistory();
		refreshHistoryList();
	}

	private void recordCurrentPageVisit() {
		String url = webEngine.getLocation();
		String title = webEngine.getTitle();
		if (url != null && !url.isEmpty() && !"about:blank".equals(url)) {
			try {
				visitHistoryService.recordVisit(title, url);
			} catch (Exception e) {
				// Silently ignore history recording errors
			}
		}
	}

	private void updateBookmarkButtonState() {
		String url = webEngine.getLocation();
		if (url != null && bookmarkService.isBookmarked(url)) {
			bookmarkBtn.setText("\u2605"); // filled star
			bookmarkBtn.setStyle("-fx-font-size: 18; -fx-min-width: 36; -fx-min-height: 36; -fx-text-fill: #f4b400;");
		} else {
			bookmarkBtn.setText("\u2606"); // empty star
			bookmarkBtn.setStyle("-fx-font-size: 18; -fx-min-width: 36; -fx-min-height: 36;");
		}
	}

	private void refreshBookmarkList(String query) {
		List<Bookmark> bookmarks;
		if (query != null && !query.trim().isEmpty()) {
			bookmarks = bookmarkService.searchBookmarks(query);
		} else {
			bookmarks = bookmarkService.getAllBookmarks();
		}
		ObservableList<Object> items = FXCollections.observableArrayList();
		items.addAll(bookmarks);
		listView.setItems(items);
	}

	private void refreshHistoryList() {
		List<VisitRecord> history = visitHistoryService.getRecentHistory();
		ObservableList<Object> items = FXCollections.observableArrayList();
		items.addAll(history);
		listView.setItems(items);
	}

	private void setupListViewCellFactory() {
		final boolean isBookmarkMode = showingBookmarks;
		listView.setCellFactory(new Callback<ListView<Object>, ListCell<Object>>() {
			@Override
			public ListCell<Object> call(ListView<Object> param) {
				return new ListCell<Object>() {
					@Override
					protected void updateItem(Object item, boolean empty) {
						super.updateItem(item, empty);
						if (empty || item == null) {
							setText(null);
							setGraphic(null);
							return;
						}

						HBox hbox = new HBox(8);
						hbox.setAlignment(Pos.CENTER_LEFT);
						hbox.setPadding(new Insets(2, 4, 2, 4));

						VBox textBox = new VBox(2);
						String title = "";
						String url = "";

						if (item instanceof Bookmark) {
							Bookmark b = (Bookmark) item;
							title = b.getTitle() != null && !b.getTitle().isEmpty() ? b.getTitle() : "(No title)";
							url = b.getUrl();
						} else if (item instanceof VisitRecord) {
							VisitRecord v = (VisitRecord) item;
							title = v.getTitle() != null && !v.getTitle().isEmpty() ? v.getTitle() : "(No title)";
							url = v.getUrl();
						}

						Label titleLabel = new Label(title);
						titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
						titleLabel.setMaxWidth(200);
						titleLabel.setWrapText(false);

						Label urlLabel = new Label(url);
						urlLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888;");
						urlLabel.setMaxWidth(200);
						urlLabel.setWrapText(false);

						textBox.getChildren().addAll(titleLabel, urlLabel);
						HBox.setHgrow(textBox, Priority.ALWAYS);
						hbox.getChildren().add(textBox);

						if (isBookmarkMode && item instanceof Bookmark) {
							final Bookmark bookmark = (Bookmark) item;
							JFXButton deleteBtn = new JFXButton("X");
							deleteBtn.setStyle("-fx-font-size: 11; -fx-min-width: 24; -fx-min-height: 24; -fx-text-fill: #cc0000;");
							deleteBtn.setOnAction(new javafx.event.EventHandler<ActionEvent>() {
								@Override
								public void handle(ActionEvent e) {
									try {
										bookmarkService.deleteBookmark(bookmark.getId());
										updateBookmarkButtonState();
										refreshBookmarkList(searchField.getText());
									} catch (Exception ex) {
										showError(ex.getMessage());
									}
								}
							});
							hbox.getChildren().add(deleteBtn);
						}

						setGraphic(hbox);

						// Click to navigate
						final String navUrl = url;
						setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
							@Override
							public void handle(javafx.scene.input.MouseEvent event) {
								if (event.getClickCount() == 2 && navUrl != null && !navUrl.isEmpty()) {
									setUrl(navUrl);
								}
							}
						});
					}
				};
			}
		});
	}

	private void showError(String message) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Warning");
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}
}
