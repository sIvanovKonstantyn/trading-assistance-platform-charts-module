package com.trading.chartapp.model;

import java.util.List;

public class Indicator {
    private String name;
    private List<Double> values;
    private List<Long> timestamps;

    public Indicator(String name, List<Double> values, List<Long> timestamps) {
        this.name = name;
        this.values = values;
        this.timestamps = timestamps;
    }

    public String getName() { return name; }
    public List<Double> getValues() { return values; }
    public List<Long> getTimestamps() { return timestamps; }
} 