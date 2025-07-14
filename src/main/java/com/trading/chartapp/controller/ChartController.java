package com.trading.chartapp.controller;

import com.trading.chartapp.db.SQLiteConnection;
import com.trading.chartapp.model.Candlestick;
import com.trading.chartapp.model.ChartData;
import com.trading.chartapp.model.Indicator;
import com.trading.chartapp.model.Timeframe;
import com.trading.chartapp.ui.ChartCanvas;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ChartController {
    private SQLiteConnection db;
    private ChartCanvas chartCanvas;
    private String currentPair;
    private Timeframe currentTimeframe;
    private String dbPath;
    private int currentPeriod = 14;
    private long startDate = -1;
    private long endDate = -1;

    public ChartController(ChartCanvas chartCanvas, String dbPath) {
        this.chartCanvas = chartCanvas;
        this.dbPath = dbPath;
        this.db = new SQLiteConnection();
    }

    public void loadData(String pair, Timeframe tf, long startDate, long endDate) {
        try {
            db.connect(dbPath);
            List<Candlestick> candles = db.loadCandlesticks(pair, tf, startDate, endDate);
            Map<String, Indicator> indicators = db.loadIndicators(pair, tf, startDate, endDate);
            chartCanvas.drawCandlesticks(candles);
            chartCanvas.drawIndicators(indicators);
            this.currentPair = pair;
            this.currentTimeframe = tf;
            this.startDate = startDate;
            this.endDate = endDate;
            db.disconnect();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addIndicator(Indicator indicator) {
        chartCanvas.addIndicator(indicator);
    }

    public void removeIndicator(String indicatorName) {
        chartCanvas.removeIndicator(indicatorName);
    }

    public void setTimeframe(Timeframe tf) {
        if (currentPair != null) {
            loadData(currentPair, tf, startDate, endDate);
        }
    }

    public void setPair(String pair) {
        if (currentTimeframe != null) {
            loadData(pair, currentTimeframe, startDate, endDate);
        }
    }

    public void setPeriod(int period) {
        this.currentPeriod = period;
        if (currentPair != null && currentTimeframe != null) {
            loadData(currentPair, currentTimeframe, startDate, endDate);
        }
    }

    public int getCurrentPeriod() {
        return currentPeriod;
    }

    public void setDateRange(long start, long end) {
        this.startDate = start;
        this.endDate = end;
        if (currentPair != null && currentTimeframe != null) {
            loadData(currentPair, currentTimeframe, start, end);
        }
    }

    public long getStartDate() { return startDate; }
    public long getEndDate() { return endDate; }
} 