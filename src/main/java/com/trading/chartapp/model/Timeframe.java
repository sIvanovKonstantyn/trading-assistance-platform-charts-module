package com.trading.chartapp.model;

public enum Timeframe {
    ONE_MIN("1m"),
    FIVE_MIN("5m"),
    FIFTEEN_MIN("15m"),
    ONE_HOUR("1h"),
    FOUR_HOUR("4h"),
    ONE_DAY("1d");

    private final String label;

    Timeframe(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
} 