package com.trading.chartapp;

import com.trading.chartapp.ui.ChartPanel;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.SVGPath;

public class MainApp extends Application {
    private GridPane gridPane;
    private List<ChartPanel> chartPanels = new ArrayList<>();
    private int chartCounter = 1;
    private int sceneWidth = 1250;
    private int sceneHeight = 900;
    private ChartPanel fullscreenPanel = null;
    private List<ChartPanel> prevPanelsState = null;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Multi-Chart Trading Platform");
        
        // Create a grid layout for multiple charts
        gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));
        gridPane.setStyle("-fx-background-color: #ffffff;"); // Changed to white
        gridPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Create control panel for layout options
        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;"); // Light gray with bottom border
        
        Button addChartBtn = new Button();
        addChartBtn.setGraphic(createPlusIcon());
        addChartBtn.setMinHeight(28);
        addChartBtn.setPrefHeight(28);
        addChartBtn.setMaxHeight(28);
        Button removeChartBtn = new Button();
        removeChartBtn.setGraphic(createMinusIcon());
        removeChartBtn.setMinHeight(28);
        removeChartBtn.setPrefHeight(28);
        removeChartBtn.setMaxHeight(28);
        
        addChartBtn.setOnAction(e -> addChart());
        removeChartBtn.setOnAction(e -> removeChart());
        
        controlPanel.getChildren().addAll(addChartBtn, removeChartBtn);
        
        // Create main layout
        VBox root = new VBox();
        root.getChildren().addAll(controlPanel, gridPane);
        root.setStyle("-fx-background-color: #ffffff;"); // Changed to white
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(gridPane, javafx.scene.layout.Priority.ALWAYS); // Ensure gridPane fills available space
        
        Scene scene = new Scene(root, sceneWidth, sceneHeight);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start with one chart
        addChart();
    }

    private void addChart() {
        if (chartPanels.size() >= 9) {
            System.out.println("Maximum number of charts reached (9)");
            return;
        }

        int numCharts = chartPanels.size() + 1;
        int cols, rows;
        if (numCharts == 1) {
            cols = 1; rows = 1;
        } else if (numCharts == 2) {
            cols = 2; rows = 1;
        } else if (numCharts <= 4) {
            cols = 2; rows = 2;
        } else if (numCharts <= 6) {
            cols = 3; rows = 2;
        } else {
            cols = 3; rows = 3;
        }
        int chartWidth = sceneWidth / cols;
        int chartHeight = sceneHeight / rows;

        // Save state of all existing charts
        java.util.List<ChartPanel.ChartState> savedStates = new java.util.ArrayList<>();
        for (ChartPanel panel : chartPanels) {
            savedStates.add(panel.getChartState());
        }
        // Remove all charts from the grid and list
        gridPane.getChildren().clear();
        List<ChartPanel> newPanels = new ArrayList<>();
        for (int i = 0; i < numCharts; i++) {
            String title = "Chart " + (i + 1);
            ChartPanel panel = new ChartPanel(title, chartWidth, chartHeight);
            newPanels.add(panel);
        }
        // Reapply saved states to corresponding charts
        for (int i = 0; i < savedStates.size() && i < newPanels.size(); i++) {
            newPanels.get(i).applyChartState(savedStates.get(i));
        }
        // For any new charts, apply the last saved state (if any)
        if (!savedStates.isEmpty()) {
            ChartPanel.ChartState lastState = savedStates.get(savedStates.size() - 1);
            for (int i = savedStates.size(); i < newPanels.size(); i++) {
                newPanels.get(i).applyChartState(lastState);
            }
        }
        chartPanels.clear();
        chartPanels.addAll(newPanels);

        // After adding new charts, trigger symbol change action for each chart
        for (ChartPanel panel : chartPanels) {
            ComboBox<String> pairBox = panel.getPairBox();
            if (pairBox != null && pairBox.getOnAction() != null) {
                pairBox.getOnAction().handle(null);
            }
            // Set up fullscreen toggle listener
            panel.setOnFullscreenToggle(() -> handleFullscreenToggle(panel));
        }

        // Add all panels to the grid
        for (int i = 0; i < chartPanels.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            gridPane.add(chartPanels.get(i), col, row);
            GridPane.setFillWidth(chartPanels.get(i), true);
            GridPane.setFillHeight(chartPanels.get(i), true);
            chartPanels.get(i).setPrefSize(0, 0);
            chartPanels.get(i).setMinSize(0, 0);
            chartPanels.get(i).setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }

        updateGridLayout(); // Optionally keep this if you want to update constraints
    }

    private void removeChart() {
        if (chartPanels.isEmpty()) {
            System.out.println("No charts to remove");
            return;
        }
        
        ChartPanel lastChart = chartPanels.remove(chartPanels.size() - 1);
        gridPane.getChildren().remove(lastChart);
        
        updateGridLayout();
    }

    private void updateGridLayout() {
        int numCharts = chartPanels.size();
        int cols, rows;

        if (numCharts == 1) {
            cols = 1; rows = 1;
        } else if (numCharts == 2) {
            cols = 2; rows = 1;
        } else if (numCharts <= 4) {
            cols = 2; rows = 2;
        } else if (numCharts <= 6) {
            cols = 3; rows = 2;
        } else {
            cols = 3; rows = 3;
        }

        gridPane.getChildren().clear();
        int chartsToShow = Math.min(chartPanels.size(), cols * rows);
        for (int i = 0; i < chartsToShow; i++) {
            int row = i / cols;
            int col = i % cols;
            gridPane.add(chartPanels.get(i), col, row);
            GridPane.setFillWidth(chartPanels.get(i), true);
            GridPane.setFillHeight(chartPanels.get(i), true);
            chartPanels.get(i).setPrefSize(0, 0);
            chartPanels.get(i).setMinSize(0, 0);
            chartPanels.get(i).setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        // Set column and row constraints to make charts expand
        gridPane.getColumnConstraints().clear();
        gridPane.getRowConstraints().clear();
        for (int c = 0; c < cols; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(100.0 / cols);
            cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            gridPane.getColumnConstraints().add(cc);
        }
        for (int r = 0; r < rows; r++) {
            javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
            rc.setPercentHeight(100.0 / rows);
            rc.setVgrow(javafx.scene.layout.Priority.ALWAYS);
            gridPane.getRowConstraints().add(rc);
        }
        // Reset all chart panels' preferred and minimum sizes to allow resizing
        for (ChartPanel panel : chartPanels) {
            panel.setPrefSize(0, 0);
            panel.setMinSize(0, 0);
        }
    }

    private void handleFullscreenToggle(ChartPanel panel) {
        if (panel.isFullscreen()) {
            // Enter fullscreen: hide all other panels, expand this one
            fullscreenPanel = panel;
            prevPanelsState = new ArrayList<>(chartPanels);
            gridPane.getChildren().clear();
            gridPane.getColumnConstraints().clear();
            gridPane.getRowConstraints().clear();
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(100);
            cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            gridPane.getColumnConstraints().add(cc);
            javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
            rc.setPercentHeight(100);
            rc.setVgrow(javafx.scene.layout.Priority.ALWAYS);
            gridPane.getRowConstraints().add(rc);
            gridPane.add(panel, 0, 0);
            GridPane.setFillWidth(panel, true);
            GridPane.setFillHeight(panel, true);
            VBox.setVgrow(panel, javafx.scene.layout.Priority.ALWAYS);
            panel.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
            panel.setMinSize(0, 0);
            panel.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            for (ChartPanel p : chartPanels) {
                if (p != panel) p.setVisible(false);
            }
            panel.requestLayout();
            gridPane.requestLayout();
        } else {
            // Exit fullscreen: restore all panels and layout
            if (prevPanelsState != null) {
                gridPane.getChildren().clear();
                gridPane.getColumnConstraints().clear();
                gridPane.getRowConstraints().clear();
                chartPanels.clear();
                chartPanels.addAll(prevPanelsState);
                int numCharts = chartPanels.size();
                int cols, rows;
                if (numCharts == 1) {
                    cols = 1; rows = 1;
                } else if (numCharts == 2) {
                    cols = 2; rows = 1;
                } else if (numCharts <= 4) {
                    cols = 2; rows = 2;
                } else if (numCharts <= 6) {
                    cols = 3; rows = 2;
                } else {
                    cols = 3; rows = 3;
                }
                for (int c = 0; c < cols; c++) {
                    javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
                    cc.setPercentWidth(100.0 / cols);
                    cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
                    gridPane.getColumnConstraints().add(cc);
                }
                for (int r = 0; r < rows; r++) {
                    javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
                    rc.setPercentHeight(100.0 / rows);
                    rc.setVgrow(javafx.scene.layout.Priority.ALWAYS);
                    gridPane.getRowConstraints().add(rc);
                }
                for (int i = 0; i < chartPanels.size(); i++) {
                    int row = i / cols;
                    int col = i % cols;
                    ChartPanel p = chartPanels.get(i);
                    gridPane.add(p, col, row);
                    GridPane.setFillWidth(p, true);
                    GridPane.setFillHeight(p, true);
                    VBox.setVgrow(p, javafx.scene.layout.Priority.ALWAYS);
                    p.setPrefSize(0, 0);
                    p.setMinSize(0, 0);
                    p.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                    p.setVisible(true);
                    p.setFullscreen(false);
                }
                fullscreenPanel = null;
                prevPanelsState = null;
                gridPane.requestLayout();
            }
        }
    }

    private SVGPath createPlusIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M12 5 V19 M5 12 H19");
        svg.setStyle("-fx-stroke: #333; -fx-stroke-width: 2; -fx-fill: none;");
        svg.setScaleX(20.0 / 24.0);
        svg.setScaleY(20.0 / 24.0);
        return svg;
    }
    private SVGPath createMinusIcon() {
        SVGPath svg = new SVGPath();
        svg.setContent("M5 12 H19");
        svg.setStyle("-fx-stroke: #333; -fx-stroke-width: 2; -fx-fill: none;");
        svg.setScaleX(20.0 / 24.0);
        svg.setScaleY(20.0 / 24.0);
        return svg;
    }

    public static void main(String[] args) {
        launch(args);
    }
} 