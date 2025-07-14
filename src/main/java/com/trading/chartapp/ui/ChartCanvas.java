package com.trading.chartapp.ui;

import com.trading.chartapp.model.Candlestick;
import com.trading.chartapp.model.Indicator;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartCanvas extends Canvas {
    private List<Candlestick> candlesticks;
    private Map<String, Indicator> indicators = new HashMap<>();
    private Integer hoverIndex = null;
    private double hoverX = 0;
    private double hoverY = 0;
    private final HashMap<String, Boolean> indicatorEnabled = new HashMap<>();
    private double subchartHeight = 120;
    private boolean draggingSeparator = false;
    private static final double SEPARATOR_THICKNESS = 6;
    private static final double CHART_GAP = 40;

    {
        setOnMouseMoved(e -> {
            if (candlesticks == null || candlesticks.isEmpty()) {
                hoverIndex = null;
                return;
            }
            double w = getWidth();
            double chartW = w - LEFT_PAD - RIGHT_PAD;
            int n = candlesticks.size();
            double x = e.getX();
            if (x < LEFT_PAD || x > w - RIGHT_PAD) {
                hoverIndex = null;
                redraw();
                return;
            }
            int idx = (int) ((x - LEFT_PAD) / (chartW / n));
            idx = Math.max(0, Math.min(n - 1, idx));
            hoverIndex = idx;
            hoverX = e.getX();
            hoverY = e.getY();
            redraw();
            double h = getHeight();
            double sepY = h - subchartHeight - BOTTOM_PAD + SEPARATOR_THICKNESS / 2;
            if (Math.abs(e.getY() - sepY) < SEPARATOR_THICKNESS) {
                setCursor(javafx.scene.Cursor.V_RESIZE);
            } else {
                setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
        setOnMouseExited(e -> {
            hoverIndex = null;
            redraw();
        });
        setOnMousePressed(e -> {
            double h = getHeight();
            double sepY = h - subchartHeight - BOTTOM_PAD + SEPARATOR_THICKNESS / 2;
            if (Math.abs(e.getY() - sepY) < SEPARATOR_THICKNESS) {
                draggingSeparator = true;
            }
        });
        setOnMouseDragged(e -> {
            if (draggingSeparator) {
                double h = getHeight();
                double newSubchartHeight = h - BOTTOM_PAD - e.getY();
                subchartHeight = Math.max(60, Math.min(newSubchartHeight, h - 120));
                redraw();
            }
        });
        setOnMouseReleased(e -> draggingSeparator = false);
    }

    private static final double LEFT_PAD = 60;
    private static final double RIGHT_PAD = 20;
    private static final double TOP_PAD = 20;
    private static final double BOTTOM_PAD = 40;
    private static final double SUBCHART_HEIGHT = 120;
    private static final String[] OSCILLATORS = {"RSI", "MACD"};

    public ChartCanvas() {
        super(0, 0);
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());
    }

    public ChartCanvas(double width, double height) {
        super(width, height);
        widthProperty().addListener((obs, oldVal, newVal) -> redraw());
        heightProperty().addListener((obs, oldVal, newVal) -> redraw());
    }

    @Override
    public void resize(double width, double height) {
        super.resize(width, height); // Do not call setWidth/setHeight here
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    public void drawCandlesticks(List<Candlestick> data) {
        this.candlesticks = data;
        redraw();
    }

    public void drawIndicators(Map<String, Indicator> indicators) {
        this.indicators = indicators;
        redraw();
    }

    public void addIndicator(Indicator indicator) {
        indicators.put(indicator.getName(), indicator);
        redraw();
    }

    public void removeIndicator(String indicatorName) {
        indicators.remove(indicatorName);
        redraw();
    }

    public Map<String, Indicator> getIndicators() {
        return indicators;
    }

    public void setIndicatorEnabled(String name, boolean enabled) {
        System.out.println("Toggling indicator: " + name + " -> " + enabled);
        indicatorEnabled.put(name, enabled);
        redraw();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        double w = getWidth();
        double h = getHeight();
        double subchartTop = h - subchartHeight - BOTTOM_PAD;
        double gapTop = subchartTop - CHART_GAP / 2;
        double gapBottom = subchartTop + CHART_GAP / 2;
        if (candlesticks != null) {
            drawCandles(gc, 0, gapTop);
        }
        if (indicators != null) {
            gc.save();
            gc.beginPath();
            gc.rect(LEFT_PAD, 0, w - LEFT_PAD - RIGHT_PAD, gapTop);
            gc.closePath();
            gc.clip();
            drawAllIndicators(gc, 0, gapTop);
            gc.restore();
            gc.save();
            gc.beginPath();
            gc.rect(LEFT_PAD, gapBottom, w - LEFT_PAD - RIGHT_PAD, h - gapBottom - BOTTOM_PAD);
            gc.closePath();
            gc.clip();
            drawOscillators(gc, gapBottom, h - BOTTOM_PAD);
            gc.restore();
        }
        drawAxes(gc, 0, gapTop, false);
        drawAxes(gc, gapBottom, h - BOTTOM_PAD, true);
        drawSeparator(gc, subchartTop);
        // Border is now handled by ChartPanel
        drawHoverTooltip(gc);
        // TODO: draw annotations
    }

    private void drawSeparator(GraphicsContext gc, double y) {
        double w = getWidth();
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(SEPARATOR_THICKNESS);
        gc.strokeLine(LEFT_PAD, y, w - RIGHT_PAD, y);
        gc.setLineWidth(1);
    }

    private void drawHoverTooltip(GraphicsContext gc) {
        if (hoverIndex == null || candlesticks == null || hoverIndex < 0 || hoverIndex >= candlesticks.size()) return;
        Candlestick c = candlesticks.get(hoverIndex);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        StringBuilder sb = new StringBuilder();
        sb.append("Time: ").append(fmt.format(Instant.ofEpochMilli(c.getTimestamp()))).append("\n");
        sb.append(String.format("O: %.2f  H: %.2f  L: %.2f  C: %.2f\nV: %.2f\n", c.getOpen(), c.getHigh(), c.getLow(), c.getClose(), c.getVolume()));
        if (indicators != null && !indicators.isEmpty()) {
            for (Indicator ind : indicators.values()) {
                if (hoverIndex < ind.getValues().size()) {
                    sb.append(ind.getName()).append(": ").append(String.format("%.4f", ind.getValues().get(hoverIndex))).append("\n");
                }
            }
        }
        String text = sb.toString();
        String[] lines = text.split("\\n");
        double boxW = 0;
        for (String line : lines) boxW = Math.max(boxW, gc.getFont().getSize() * line.length() * 0.6);
        double boxH = lines.length * 18 + 8;
        double w = getWidth();
        double h = getHeight();
        double chartW = w - LEFT_PAD - RIGHT_PAD;
        double chartH = h - TOP_PAD - BOTTOM_PAD;
        int n = candlesticks.size();
        double candleWidth = Math.max(2, chartW / n);
        double x = LEFT_PAD + hoverIndex * candleWidth + candleWidth / 2;
        double min = candlesticks.stream().mapToDouble(Candlestick::getLow).min().orElse(0);
        double max = candlesticks.stream().mapToDouble(Candlestick::getHigh).max().orElse(1);
        double y = TOP_PAD + chartH - ((c.getClose() - min) / (max - min)) * chartH;
        // Draw crosshair lines
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineDashes(4);
        gc.strokeLine(x, TOP_PAD, x, h - BOTTOM_PAD); // vertical
        gc.strokeLine(LEFT_PAD, y, w - RIGHT_PAD, y); // horizontal
        gc.setLineDashes(null);
        // Draw tooltip box
        double tooltipX = hoverX + 10;
        double tooltipY = hoverY + 10;
        if (tooltipX + boxW > w) tooltipX = w - boxW - 10;
        if (tooltipY + boxH > h) tooltipY = h - boxH - 10;
        gc.setFill(Color.rgb(255,255,255,0.95));
        gc.fillRect(tooltipX, tooltipY, boxW, boxH);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(tooltipX, tooltipY, boxW, boxH);
        gc.setFill(Color.BLACK);
        for (int i = 0; i < lines.length; i++) {
            gc.fillText(lines[i], tooltipX + 8, tooltipY + 20 + i * 18 - 10);
        }
    }

    private void drawAxes(GraphicsContext gc, double top, double bottom, boolean isOscillator) {
        if (candlesticks == null || candlesticks.isEmpty()) return;
        double w = getWidth();
        double chartW = w - LEFT_PAD - RIGHT_PAD;
        double chartH = bottom - top;
        double min, max;
        if (isOscillator) {
            min = 0; max = 100;
            for (String osc : OSCILLATORS) {
                if (indicators != null && indicators.containsKey(osc)) {
                    List<Double> vals = indicators.get(osc).getValues();
                    if (!vals.isEmpty()) {
                        min = Math.min(min, vals.stream().mapToDouble(Double::doubleValue).min().orElse(min));
                        max = Math.max(max, vals.stream().mapToDouble(Double::doubleValue).max().orElse(max));
                    }
                }
            }
        } else {
            min = candlesticks.stream().mapToDouble(Candlestick::getLow).min().orElse(0);
            max = candlesticks.stream().mapToDouble(Candlestick::getHigh).max().orElse(1);
        }
        long startTs = candlesticks.get(0).getTimestamp();
        long endTs = candlesticks.get(candlesticks.size() - 1).getTimestamp();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.LIGHTGRAY);
        int yTicks = 5;
        for (int i = 0; i < yTicks; i++) {
            double frac = i / (double)(yTicks - 1);
            double value = max - frac * (max - min);
            double y = top + frac * chartH;
            gc.strokeLine(LEFT_PAD, y, w - RIGHT_PAD, y);
            gc.fillText(String.format("%.2f", value), 6, y + 4);
        }
        int xTicks = 5;
        for (int i = 0; i < xTicks; i++) {
            double frac = i / (double)(xTicks - 1);
            long ts = startTs + (long)((endTs - startTs) * frac);
            double x = LEFT_PAD + frac * chartW;
            gc.strokeLine(x, bottom, x, top);
            if (!isOscillator) gc.fillText(fmt.format(Instant.ofEpochMilli(ts)), x - 40, bottom + 12);
        }
        // Draw axis lines
        gc.setStroke(Color.BLACK);
        gc.strokeLine(LEFT_PAD, top, LEFT_PAD, bottom); // Y axis
        gc.strokeLine(LEFT_PAD, bottom, w - RIGHT_PAD, bottom); // X axis
    }

    private void drawLegend(GraphicsContext gc) {
        if (candlesticks == null || candlesticks.isEmpty()) return;
        double min = candlesticks.stream().mapToDouble(Candlestick::getLow).min().orElse(0);
        double max = candlesticks.stream().mapToDouble(Candlestick::getHigh).max().orElse(1);
        long startTs = candlesticks.get(0).getTimestamp();
        long endTs = candlesticks.get(candlesticks.size() - 1).getTimestamp();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
        String legend = String.format("Price: %.2f - %.2f\nTime: %s - %s",
                min, max,
                fmt.format(Instant.ofEpochMilli(startTs)),
                fmt.format(Instant.ofEpochMilli(endTs)));
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, 320, 40);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(0, 0, 320, 40);
        gc.setFill(Color.BLACK);
        gc.fillText(legend, 10, 18);
    }

    private void drawCandles(GraphicsContext gc, double top, double bottom) {
        if (candlesticks == null || candlesticks.isEmpty()) return;
        double w = getWidth();
        double chartW = w - LEFT_PAD - RIGHT_PAD;
        double chartH = bottom - top;
        int n = candlesticks.size();
        double candleWidth = Math.max(2, chartW / n);
        double min = candlesticks.stream().mapToDouble(Candlestick::getLow).min().orElse(0);
        double max = candlesticks.stream().mapToDouble(Candlestick::getHigh).max().orElse(1);
        for (int i = 0; i < n; i++) {
            Candlestick c = candlesticks.get(i);
            double x = LEFT_PAD + i * candleWidth;
            double openY = top + chartH - ((c.getOpen() - min) / (max - min)) * chartH;
            double closeY = top + chartH - ((c.getClose() - min) / (max - min)) * chartH;
            double highY = top + chartH - ((c.getHigh() - min) / (max - min)) * chartH;
            double lowY = top + chartH - ((c.getLow() - min) / (max - min)) * chartH;
            gc.setStroke(Color.BLACK);
            gc.strokeLine(x + candleWidth / 2, highY, x + candleWidth / 2, lowY);
            gc.setFill(c.getClose() >= c.getOpen() ? Color.LIMEGREEN : Color.RED);
            gc.fillRect(x, Math.min(openY, closeY), candleWidth, Math.abs(openY - closeY));
        }
    }

    private void drawAllIndicators(GraphicsContext gc, double top, double bottom) {
        if (indicators == null || indicators.isEmpty() || candlesticks == null) return;
        double w = getWidth();
        double chartW = w - LEFT_PAD - RIGHT_PAD;
        double chartH = bottom - top;
        int n = candlesticks.size();
        double min = candlesticks.stream().mapToDouble(Candlestick::getLow).min().orElse(0);
        double max = candlesticks.stream().mapToDouble(Candlestick::getHigh).max().orElse(1);
        for (Indicator indicator : indicators.values()) {
            boolean enabled = !indicatorEnabled.containsKey(indicator.getName()) || indicatorEnabled.get(indicator.getName());
            if (!enabled) continue;
            // Only draw overlays (not oscillators)
            boolean isOsc = false;
            for (String osc : OSCILLATORS) if (indicator.getName().toUpperCase().contains(osc)) isOsc = true;
            if (isOsc) continue;
            List<Double> values = indicator.getValues();
            if (values.size() < 2) continue;
            gc.setStroke(Color.BLUE); // TODO: color per indicator
            gc.beginPath();
            for (int i = 0; i < Math.min(n, values.size()); i++) {
                double x = LEFT_PAD + i * (chartW / n) + (chartW / n) / 2;
                double y = top + chartH - ((values.get(i) - min) / (max - min)) * chartH;
                if (i == 0) gc.moveTo(x, y);
                else gc.lineTo(x, y);
            }
            gc.stroke();
        }
    }

    private void drawOscillators(GraphicsContext gc, double top, double bottom) {
        if (indicators == null || indicators.isEmpty() || candlesticks == null) return;
        double w = getWidth();
        double chartW = w - LEFT_PAD - RIGHT_PAD;
        double chartH = bottom - top;
        int n = candlesticks.size();
        // Find min/max for all enabled oscillators
        double min = 0, max = 100;
        for (String osc : OSCILLATORS) {
            if (indicators.containsKey(osc) && indicatorEnabled.getOrDefault(osc, true)) {
                List<Double> vals = indicators.get(osc).getValues();
                if (!vals.isEmpty()) {
                    min = Math.min(min, vals.stream().mapToDouble(Double::doubleValue).min().orElse(min));
                    max = Math.max(max, vals.stream().mapToDouble(Double::doubleValue).max().orElse(max));
                }
            }
        }
        for (String osc : OSCILLATORS) {
            if (!indicators.containsKey(osc) || !indicatorEnabled.getOrDefault(osc, true)) continue;
            List<Double> values = indicators.get(osc).getValues();
            if (values.size() < 2) continue;
            gc.setStroke(Color.PURPLE); // TODO: color per oscillator
            gc.beginPath();
            for (int i = 0; i < Math.min(n, values.size()); i++) {
                double x = LEFT_PAD + i * (chartW / n) + (chartW / n) / 2;
                double y = top + chartH - ((values.get(i) - min) / (max - min)) * chartH;
                if (i == 0) gc.moveTo(x, y);
                else gc.lineTo(x, y);
            }
            gc.stroke();
        }
    }

    private void drawUnifiedBorder(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(2);
        // Draw border around the entire canvas area, including space for tools panel
        gc.strokeRect(0, 0, w, h);
        gc.setLineWidth(1);
    }
} 