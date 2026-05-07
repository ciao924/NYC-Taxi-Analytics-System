package com.taxi.analytics.modules.ai.model;

import java.util.List;

public class ChartData {
    private List<String> x;
    private List<Number> y;

    public ChartData() {
    }

    public ChartData(List<String> x, List<Number> y) {
        this.x = x;
        this.y = y;
    }

    public List<String> getX() {
        return x;
    }

    public void setX(List<String> x) {
        this.x = x;
    }

    public List<Number> getY() {
        return y;
    }

    public void setY(List<Number> y) {
        this.y = y;
    }
}