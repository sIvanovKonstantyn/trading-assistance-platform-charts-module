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

public class ChartPanel extends VBox {
    private static final String DEFAULT_DB_PATH = "../analitic-data-module/demo/usdc-symbol-dbs/BTCUSDC.db";
    private static final String[] PAIRS = {"BTCUSDC", "ETHUSDC", "BNBUSDC"};

    private ChartCanvas chartCanvas;
    private ChartController controller;
    private MenuButton indicatorMenu;
    private HashMap<String, CheckMenuItem> indicatorChecks;
    private ComboBox<String> pairBox;
    private ComboBox<Timeframe> tfBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private String title;

    public ChartPanel(String title, double width, double height) {
        this.title = title;
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Allow expansion
        setStyle("-fx-border-color: #cccccc; -fx-border-width: 2; -fx-background-color: #ffffff;"); // White bg, gray border

        // Initialize chart canvas and controller
        chartCanvas = new ChartCanvas(); // Use default constructor
        controller = new ChartController(chartCanvas, DEFAULT_DB_PATH);
        indicatorMenu = new MenuButton("Indicators");
        indicatorChecks = new HashMap<>();

        // Create controls
        createControls();

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

        // Delay binding until scene and window are ready
        // Remove old sceneProperty listener for binding

        // Add title
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #222; -fx-font-weight: bold; -fx-font-size: 14;"); // Dark text
        titleLabel.setPadding(new Insets(5, 0, 5, 10));

        getChildren().addAll(titleLabel, chartContainer);
        setPadding(new Insets(5));
        VBox.setVgrow(chartContainer, javafx.scene.layout.Priority.ALWAYS); // Ensure chart area grows

        // Initial load
        loadInitialData();
    }

    private void createControls() {
        pairBox = new ComboBox<>();
        pairBox.getItems().addAll(PAIRS);
        pairBox.setValue(PAIRS[0]);
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
            controller.setPair(pairBox.getValue());
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

    private HBox createToolPanel() {
        Button prevBtn = new Button("Prev");
        Button nextBtn = new Button("Next");

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

        HBox controls = new HBox(5, pairBox, tfBox, prevBtn, startDatePicker, endDatePicker, nextBtn, indicatorMenu);
        controls.setPadding(new Insets(5));
        controls.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;"); // Light gray with bottom border
        
        return controls;
    }

    private void loadInitialData() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        long startMs = weekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        controller.loadData(PAIRS[0], Timeframe.ONE_MIN, startMs, endMs);
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
        System.out.println("Updating indicator toggles. Indicators: " + chartCanvas.getIndicators().keySet());
        indicatorMenu.getItems().clear();
        indicatorChecks.clear();
        for (String name : chartCanvas.getIndicators().keySet()) {
            CheckMenuItem cb = new CheckMenuItem(name);
            cb.setSelected(true);
            cb.setOnAction(e -> {
                System.out.println("MenuItem toggled: " + name + " -> " + cb.isSelected());
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
} 