package com.takehome.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Deserialized JSON input data.
 */
public class InputData {

    @JsonProperty("boundary")
    private List<double[]> boundary;

    @JsonProperty("door")
    private List<double[]> door;

    @JsonProperty("isOpenInward")
    private boolean isOpenInward;

    @JsonProperty("algoToPlace")
    private Map<String, double[]> algoToPlace;

    public List<double[]> getBoundary() {
        return boundary;
    }

    public List<double[]> getDoor() {
        return door;
    }

    public boolean isOpenInward() {
        return isOpenInward;
    }

    public Map<String, double[]> getAlgoToPlace() {
        return algoToPlace;
    }

    /**
     * Validate that the input meets minimum requirements.
     */
    public void validate() {
        if (boundary == null || boundary.size() < 3) {
            throw new IllegalArgumentException("Boundary must have at least 3 vertices");
        }
        if (door == null || door.size() != 2) {
            throw new IllegalArgumentException("Door must have exactly 2 endpoints");
        }
        if (algoToPlace == null || algoToPlace.isEmpty()) {
            throw new IllegalArgumentException("At least one item must be specified");
        }
        for (Map.Entry<String, double[]> entry : algoToPlace.entrySet()) {
            double[] dims = entry.getValue();
            if (dims == null || dims.length != 2 || dims[0] <= 0 || dims[1] <= 0) {
                throw new IllegalArgumentException("Invalid dimensions for item: " + entry.getKey());
            }
        }
    }
}