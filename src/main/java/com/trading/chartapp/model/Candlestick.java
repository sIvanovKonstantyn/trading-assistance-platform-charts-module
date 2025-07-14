package com.trading.chartapp.model;

public class Candlestick {
    private long timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private double volume;

    public Candlestick(long timestamp, double open, double high, double low, double close, double volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
    }

    public long getTimestamp() { return timestamp; }
    public double getOpen() { return open; }
    public double getHigh() { return high; }
    public double getLow() { return low; }
    public double getClose() { return close; }
    public double getVolume() { return volume; }
} 