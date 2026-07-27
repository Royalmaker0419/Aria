package com.takehome.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Top-level placement result: whether feasible and the placements for each item.
 */
public class PlacementResult {

    @JsonProperty("feasible")
    private final boolean feasible;

    @JsonProperty("placements")
    private final Map<String, Placement> placements;

    private PlacementResult(boolean feasible, Map<String, Placement> placements) {
        this.feasible = feasible;
        this.placements = placements;
    }

    public static PlacementResult success(Map<String, Placement> placements) {
        return new PlacementResult(true, new LinkedHashMap<>(placements));
    }

    public static PlacementResult infeasible() {
        return new PlacementResult(false, new LinkedHashMap<>());
    }

    public boolean isFeasible() {
        return feasible;
    }

    public Map<String, Placement> getPlacements() {
        return placements;
    }
}