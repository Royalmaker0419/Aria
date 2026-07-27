package com.takehome.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single item placement result.
 */
public class Placement {

    @JsonProperty("cx")
    private final double centerX;

    @JsonProperty("cy")
    private final double centerY;

    @JsonProperty("rotation")
    private final int rotation; // 0 or 90 degrees

    public Placement(double centerX, double centerY, int rotation) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.rotation = rotation;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterY() {
        return centerY;
    }

    public int getRotation() {
        return rotation;
    }
}