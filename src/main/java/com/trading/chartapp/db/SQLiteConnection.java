package com.trading.chartapp.db;

import com.trading.chartapp.model.Candlestick;
import com.trading.chartapp.model.Indicator;
import com.trading.chartapp.model.Timeframe;

import java.sql.*;
import java.util.*;

public class SQLiteConnection {
    private Connection connection;

    public void connect(String dbPath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public List<Candlestick> loadCandlesticks(String pair, Timeframe tf, long startDate, long endDate) throws SQLException {
        String sql = "SELECT openTime, open, high, low, close, volume FROM candles WHERE symbol = ? AND interval = ? AND openTime BETWEEN ? AND ? ORDER BY openTime ASC";
        List<Candlestick> candles = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pair);
            stmt.setString(2, tf.getLabel());
            stmt.setLong(3, startDate);
            stmt.setLong(4, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                candles.add(new Candlestick(
                        rs.getLong("openTime"),
                        rs.getDouble("open"),
                        rs.getDouble("high"),
                        rs.getDouble("low"),
                        rs.getDouble("close"),
                        rs.getDouble("volume")
                ));
            }
        }
        return candles;
    }

    public Map<String, Indicator> loadIndicators(String pair, Timeframe tf, long startDate, long endDate) throws SQLException {
        String sql = "SELECT indicator, timestamp, value FROM indicators WHERE symbol = ? AND timestamp BETWEEN ? AND ? ORDER BY indicator, timestamp ASC";
        Map<String, List<Double>> valuesMap = new HashMap<>();
        Map<String, List<Long>> timesMap = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pair);
            stmt.setLong(2, startDate);
            stmt.setLong(3, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String indicatorName = rs.getString("indicator");
                long ts = rs.getLong("timestamp");
                double val = rs.getDouble("value");
                valuesMap.computeIfAbsent(indicatorName, k -> new ArrayList<>()).add(val);
                timesMap.computeIfAbsent(indicatorName, k -> new ArrayList<>()).add(ts);
            }
        }
        Map<String, Indicator> indicators = new HashMap<>();
        for (String name : valuesMap.keySet()) {
            indicators.put(name, new Indicator(name, valuesMap.get(name), timesMap.get(name)));
        }
        return indicators;
    }
} 