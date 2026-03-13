package com.pulse.client.setting;

public class Setting<T> {

    private final String name;
    private T value;
    private final T defaultValue;
    private final String description;
    private double min = Double.MIN_VALUE;
    private double max = Double.MAX_VALUE;

    public Setting(String name, T defaultValue, String description) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public Setting(String name, T defaultValue) {
        this(name, defaultValue, "");
    }

    public Setting<T> setRange(double min, double max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public String getName() { return name; }
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
    public T getDefaultValue() { return defaultValue; }
    public String getDescription() { return description; }
    public double getMin() { return min; }
    public double getMax() { return max; }
}
