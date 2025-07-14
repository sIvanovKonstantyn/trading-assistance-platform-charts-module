package com.trading.chartapp.ui;

import com.trading.chartapp.controller.ChartController;
import com.trading.chartapp.model.Timeframe;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import javafx.stage.Stage;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;

public class ChartPanel extends VBox {
    private static final String DEFAULT_DB_PATH = "../analitic-data-module/demo/usdc-symbol-dbs/BTCUSDC.db";
    private static final String[] PAIRS = {"BTCUSDC", "ETHUSDC", "BNBUSDC"};
    private static final String DEFAULT_SYMBOL = "BTCUSDC";
    private static final double CONTROL_HEIGHT = 14; // 2 points smaller icon height for nav/fullscreen buttons
    private static final double BUTTON_SIZE = 25; // 1 point bigger, square buttons

    private ChartCanvas chartCanvas;
    private ChartController controller;
    private MenuButton indicatorMenu;
    private HashMap<String, CheckMenuItem> indicatorChecks;
    private ComboBox<String> pairBox;
    private ComboBox<Timeframe> tfBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private String title;
    private Button fullscreenBtn;
    private boolean isFullscreen = false;
    private Runnable onFullscreenToggle = null;
    private Stage fullscreenStage = null;

    public ChartPanel(String title, double width, double height) {
        this(title, width, height, DEFAULT_SYMBOL);
    }

    public ChartPanel(String title, double width, double height, String initialSymbol) {
        this.title = title;
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Allow expansion
        setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-background-color: #ffffff;"); // White bg, gray border

        // Initialize chart canvas and controller
        chartCanvas = new ChartCanvas(); // Use default constructor
        controller = new ChartController(chartCanvas, getDbPathForSymbol(initialSymbol));
        indicatorMenu = new MenuButton("Indicators");
        indicatorChecks = new HashMap<>();

        // Create controls
        createControls(initialSymbol);

        // Create layout
        BorderPane chartContainer = new BorderPane();
        chartContainer.setTop(createToolPanel());
        // Wrap chartCanvas in a StackPane for precise sizing
        javafx.scene.layout.StackPane canvasHolder = new javafx.scene.layout.StackPane(chartCanvas);
        chartContainer.setCenter(canvasHolder);
        chartContainer.setStyle("-fx-background-color: #f8f8f8;"); // Very light gray
        VBox.setVgrow(chartContainer, javafx.scene.layout.Priority.ALWAYS); // Allow chart area to grow

        // Bind canvas size to StackPane size (not BorderPane)
        chartCanvas.widthProperty().bind(canvasHolder.widthProperty());
        chartCanvas.heightProperty().bind(canvasHolder.heightProperty());

        // Add title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #222; -fx-font-weight: bold; -fx-font-size: 14;"); // Dark text
        titleLabel.setPadding(new Insets(5, 0, 5, 10));

        getChildren().addAll(titleLabel, chartContainer);
        setPadding(new Insets(5));
        VBox.setVgrow(chartContainer, javafx.scene.layout.Priority.ALWAYS); // Ensure chart area grows

        // Initial load
        loadInitialData(initialSymbol);
    }

    private void createControls() {
        createControls(DEFAULT_SYMBOL);
    }

    private void createControls(String initialSymbol) {
        pairBox = new ComboBox<>();
        pairBox.getItems().addAll(PAIRS);
        pairBox.setValue(initialSymbol);
        pairBox.setPrefWidth(100);

        tfBox = new ComboBox<>();
        tfBox.getItems().addAll(Timeframe.values());
        tfBox.setValue(Timeframe.ONE_MIN);
        tfBox.setPrefWidth(80);

        startDatePicker = new DatePicker();
        endDatePicker = new DatePicker();
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        startDatePicker.setValue(weekAgo);
        endDatePicker.setValue(today);

        // Set up event handlers
        pairBox.setOnAction(e -> {
            // Recreate controller with new DB path
            String selectedPair = pairBox.getValue();
            controller = new ChartController(chartCanvas, getDbPathForSymbol(selectedPair));
            controller.setPair(selectedPair);
            // Immediately load data for the new symbol
            controller.loadData(selectedPair, tfBox.getValue(),
                startDatePicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endDatePicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            updateIndicatorToggles();
        });
        
        tfBox.setOnAction(e -> {
            controller.setTimeframe(tfBox.getValue());
            updateIndicatorToggles();
        });
        
        startDatePicker.setOnAction(e -> {
            reloadWithDates();
            updateIndicatorToggles();
        });
        
        endDatePicker.setOnAction(e -> {
            reloadWithDates();
            updateIndicatorToggles();
        });
    }

    private SVGPath createLeftArrowIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M 15 2 L 5 12 L 15 22");
        svg.setStyle("-fx-stroke: #333; -fx-stroke-width: 2; -fx-fill: none;");
        svg.setScaleX(CONTROL_HEIGHT / 24.0);
        svg.setScaleY(CONTROL_HEIGHT / 24.0);
        return svg;
    }
    private SVGPath createRightArrowIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M 9 2 L 19 12 L 9 22");
        svg.setStyle("-fx-stroke: #333; -fx-stroke-width: 2; -fx-fill: none;");
        svg.setScaleX(CONTROL_HEIGHT / 24.0);
        svg.setScaleY(CONTROL_HEIGHT / 24.0);
        return svg;
    }
    private SVGPath createMaximizeIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M3 3 H21 V21 H3 Z");
        svg.setStyle("-fx-stroke: #333; -fx-stroke-width: 2; -fx-fill: none;");
        svg.setScaleX(CONTROL_HEIGHT / 24.0);
        svg.setScaleY(CONTROL_HEIGHT / 24.0);
        return svg;
    }
    private SVGPath createMinimizeIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M5 12 H19");
        svg.setStyle("-fx-stroke: #333; -fx-stroke-width: 2; -fx-fill: none;");
        svg.setScaleX(CONTROL_HEIGHT / 24.0);
        svg.setScaleY(CONTROL_HEIGHT / 24.0);
        return svg;
    }

    private HBox createToolPanel() {
        Button prevBtn = new Button();
        prevBtn.setGraphic(createLeftArrowIcon());
        prevBtn.setMinHeight(BUTTON_SIZE);
        prevBtn.setPrefHeight(BUTTON_SIZE);
        prevBtn.setMaxHeight(BUTTON_SIZE);
        prevBtn.setMinWidth(BUTTON_SIZE);
        prevBtn.setPrefWidth(BUTTON_SIZE);
        prevBtn.setMaxWidth(BUTTON_SIZE);
        prevBtn.setAlignment(Pos.CENTER);
        prevBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        Button nextBtn = new Button();
        nextBtn.setGraphic(createRightArrowIcon());
        nextBtn.setMinHeight(BUTTON_SIZE);
        nextBtn.setPrefHeight(BUTTON_SIZE);
        nextBtn.setMaxHeight(BUTTON_SIZE);
        nextBtn.setMinWidth(BUTTON_SIZE);
        nextBtn.setPrefWidth(BUTTON_SIZE);
        nextBtn.setMaxWidth(BUTTON_SIZE);
        nextBtn.setAlignment(Pos.CENTER);
        nextBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        fullscreenBtn = new Button();
        fullscreenBtn.setGraphic(createMaximizeIcon());
        fullscreenBtn.setMinHeight(BUTTON_SIZE);
        fullscreenBtn.setPrefHeight(BUTTON_SIZE);
        fullscreenBtn.setMaxHeight(BUTTON_SIZE);
        fullscreenBtn.setMinWidth(BUTTON_SIZE);
        fullscreenBtn.setPrefWidth(BUTTON_SIZE);
        fullscreenBtn.setMaxWidth(BUTTON_SIZE);
        fullscreenBtn.setAlignment(Pos.CENTER);
        fullscreenBtn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        fullscreenBtn.setOnAction(e -> {
            if (fullscreenStage == null) {
                openFullscreenWindow();
            } else {
                fullscreenStage.close();
            }
        });

        prevBtn.setOnAction(e -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            startDatePicker.setValue(start.minusDays(days + 1));
            endDatePicker.setValue(end.minusDays(days + 1));
            reloadWithDates();
            updateIndicatorToggles();
        });
        
        nextBtn.setOnAction(e -> {
            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            startDatePicker.setValue(start.plusDays(days + 1));
            endDatePicker.setValue(end.plusDays(days + 1));
            reloadWithDates();
            updateIndicatorToggles();
        });

        HBox controls = new HBox(5, pairBox, tfBox, prevBtn, startDatePicker, endDatePicker, nextBtn, indicatorMenu, fullscreenBtn);
        controls.setPadding(new Insets(5));
        controls.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;"); // Light gray with bottom border
        
        return controls;
    }

    private void loadInitialData() {
        loadInitialData(DEFAULT_SYMBOL);
    }

    private void loadInitialData(String symbol) {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        long startMs = weekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        controller.loadData(symbol, Timeframe.ONE_MIN, startMs, endMs);
        updateIndicatorToggles();
    }

    private void reloadWithDates() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        long startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = end.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        controller.setDateRange(startMs, endMs);
    }

    private void updateIndicatorToggles() {
        indicatorMenu.getItems().clear();
        indicatorChecks.clear();
        for (String name : chartCanvas.getIndicators().keySet()) {
            CheckMenuItem cb = new CheckMenuItem(name);
            cb.setSelected(true);
            cb.setOnAction(e -> {
                chartCanvas.setIndicatorEnabled(name, cb.isSelected());
            });
            indicatorMenu.getItems().add(cb);
            indicatorChecks.put(name, cb);
        }
    }

    public ChartController getController() {
        return controller;
    }

    public ChartCanvas getChartCanvas() {
        return chartCanvas;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ComboBox<String> getPairBox() {
        return pairBox;
    }

    // State holder for copying chart filters
    public static class ChartState {
        public String pair;
        public Timeframe timeframe;
        public java.time.LocalDate startDate;
        public java.time.LocalDate endDate;
        public java.util.Map<String, Boolean> indicatorStates = new java.util.HashMap<>();
    }

    public ChartState getChartState() {
        ChartState state = new ChartState();
        state.pair = pairBox.getValue();
        state.timeframe = tfBox.getValue();
        state.startDate = startDatePicker.getValue();
        state.endDate = endDatePicker.getValue();
        for (String name : chartCanvas.getIndicators().keySet()) {
            state.indicatorStates.put(name, chartCanvas.getIndicators().get(name) != null &&
                (indicatorChecks.get(name) == null || indicatorChecks.get(name).isSelected()));
        }
        return state;
    }

    public void applyChartState(ChartState state) {
        if (state == null) return;
        pairBox.setValue(state.pair);
        tfBox.setValue(state.timeframe);
        startDatePicker.setValue(state.startDate);
        endDatePicker.setValue(state.endDate);
        // Apply indicator states after toggles are updated
        updateIndicatorToggles();
        for (String name : state.indicatorStates.keySet()) {
            if (indicatorChecks.containsKey(name)) {
                indicatorChecks.get(name).setSelected(state.indicatorStates.get(name));
                chartCanvas.setIndicatorEnabled(name, state.indicatorStates.get(name));
            }
        }
        // Trigger symbol change
        controller.setPair(state.pair);
        controller.setTimeframe(state.timeframe);
        setDateRange(state.startDate, state.endDate);
    }

    private void setDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null) startDatePicker.setValue(start);
        if (end != null) endDatePicker.setValue(end);
        reloadWithDates();
    }

    private static String getDbPathForSymbol(String symbol) {
        return "../analitic-data-module/demo/usdc-symbol-dbs/" + symbol + ".db";
    }

    public void setOnFullscreenToggle(Runnable r) {
        this.onFullscreenToggle = r;
    }
    public boolean isFullscreen() {
        return isFullscreen;
    }
    public void setFullscreen(boolean fullscreen) {
        this.isFullscreen = fullscreen;
        fullscreenBtn.setGraphic(fullscreen ? createMinimizeIcon() : createMaximizeIcon());
    }

    private void openFullscreenWindow() {
        ChartPanel.ChartState state = getChartState();
        ChartPanel fullscreenPanel = new ChartPanel(title, 1200, 800, state.pair); // Use current symbol
        fullscreenPanel.applyChartState(state);
        fullscreenPanel.setTitle(this.title + " (Fullscreen)");
        fullscreenPanel.fullscreenBtn.setGraphic(createMinimizeIcon());
        fullscreenPanel.fullscreenBtn.setOnAction(e -> {
            if (fullscreenStage != null) fullscreenStage.close();
        });
        fullscreenStage = new Stage();
        fullscreenStage.setTitle(title + " (Fullscreen)");
        fullscreenStage.setScene(new javafx.scene.Scene(fullscreenPanel, 1200, 800));
        fullscreenStage.setMaximized(true);
        fullscreenStage.show();
        fullscreenStage.setOnCloseRequest(e -> fullscreenStage = null);
    }
} 