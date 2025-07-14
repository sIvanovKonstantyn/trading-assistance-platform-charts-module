package com.trading.chartapp.ui;

import com.trading.chartapp.controller.ChartController;
import com.trading.chartapp.model.Timeframe;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;

public class ChartWindow {
    private static final String DEFAULT_DB_PATH = "../analitic-data-module/demo/usdc-symbol-dbs/BTCUSDC.db";
    private static final String[] PAIRS = {"BTCUSDC", "ETHUSDC", "BNBUSDC"};

    private Stage stage;
    private ChartCanvas chartCanvas;
    private ChartController controller;
    private MenuButton indicatorMenu;
    private HashMap<String, CheckMenuItem> indicatorChecks;
    private ComboBox<String> pairBox;
    private ComboBox<Timeframe> tfBox;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;

    public ChartWindow(String title, double width, double height) {
        stage = new Stage();
        stage.setTitle(title);
        
        // Initialize chart canvas and controller
        chartCanvas = new ChartCanvas(width, height);
        controller = new ChartController(chartCanvas, DEFAULT_DB_PATH);
        indicatorMenu = new MenuButton("Indicators");
        indicatorChecks = new HashMap<>();

        // Create controls
        createControls();
        
        // Create layout
        BorderPane root = new BorderPane();
        root.setTop(createToolPanel());
        root.setCenter(chartCanvas);

        Scene scene = new Scene(root, width, height + 100); // Extra height for tools panel
        stage.setScene(scene);
        
        // Initial load
        loadInitialData();
    }

    private void createControls() {
        pairBox = new ComboBox<>();
        pairBox.getItems().addAll(PAIRS);
        pairBox.setValue(PAIRS[0]);

        tfBox = new ComboBox<>();
        tfBox.getItems().addAll(Timeframe.values());
        tfBox.setValue(Timeframe.ONE_MIN);

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

        HBox controls = new HBox(10, pairBox, tfBox, prevBtn, startDatePicker, endDatePicker, nextBtn, indicatorMenu);
        controls.setPadding(new Insets(10));
        
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

    public void show() {
        stage.show();
    }

    public void setTitle(String title) {
        stage.setTitle(title);
    }

    public ChartController getController() {
        return controller;
    }

    public ChartCanvas getChartCanvas() {
        return chartCanvas;
    }
} 