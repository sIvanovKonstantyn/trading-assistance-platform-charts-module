package com.trading.chartapp.model;

import java.util.List;
import java.util.Map;

public class ChartData {
    private List<Candlestick> candlesticks;
    private Map<String, Indicator> indicators;

    public ChartData(List<Candlestick> candlesticks, Map<String, Indicator> indicators) {
        this.candlesticks = candlesticks;
        this.indicators = indicators;
    }

    public List<Candlestick> getCandlesticks() { return candlesticks; }
    public Map<String, Indicator> getIndicators() { return indicators; }
} 