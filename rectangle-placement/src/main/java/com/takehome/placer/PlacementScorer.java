package com.takehome.placer;

import com.takehome.core.Grid;

/**
 * Scoring function for candidate item placements.
 * Prioritizes wall-hugging positions (adjacent to BOUNDARY cells).
 */
public class PlacementScorer {

    private PlacementScorer() {
        // utility class
    }

    /**
     * Compute a score for placing an item at (gx, gy) with dimensions (w, h).
     * Higher score = better placement.
     *
     * @param grid the grid
     * @param gx  top-left grid x of item
     * @param gy  top-left grid y of item
     * @param w   width in grid cells
     * @param h   height in grid cells
     * @return placement score
     */
    public static double score(Grid grid, int gx, int gy, int w, int h) {
        double score = 0.0;

        int wallSides = 0;
        int adjacentSides = 0;

        // Check top side: cells (gx..gx+w-1, gy-1)
        if (gy > 0 && grid.allCellsAre(gx, gy - 1, w, 1, Grid.BOUNDARY)) {
            wallSides++;
        } else if (gy > 0 && grid.allCellsAre(gx, gy - 1, w, 1, Grid.OCCUPIED)) {
            adjacentSides++;
        }

        // Check bottom side: cells (gx..gx+w-1, gy+h)
        if (gy + h < grid.getHeight() && grid.allCellsAre(gx, gy + h, w, 1, Grid.BOUNDARY)) {
            wallSides++;
        } else if (gy + h < grid.getHeight() && grid.allCellsAre(gx, gy + h, w, 1, Grid.OCCUPIED)) {
            adjacentSides++;
        }

        // Check left side: cells (gx-1, gy..gy+h-1)
        if (gx > 0 && grid.allCellsAre(gx - 1, gy, 1, h, Grid.BOUNDARY)) {
            wallSides++;
        } else if (gx > 0 && grid.allCellsAre(gx - 1, gy, 1, h, Grid.OCCUPIED)) {
            adjacentSides++;
        }

        // Check right side: cells (gx+w, gy..gy+h-1)
        if (gx + w < grid.getWidth() && grid.allCellsAre(gx + w, gy, 1, h, Grid.BOUNDARY)) {
            wallSides++;
        } else if (gx + w < grid.getWidth() && grid.allCellsAre(gx + w, gy, 1, h, Grid.OCCUPIED)) {
            adjacentSides++;
        }

        // Wall adjacency is the primary scoring factor
        score += wallSides * 100.0;
        score += adjacentSides * 30.0;

        // Corner bonus: items in corners are especially good
        if (wallSides >= 2) {
            score += 50.0;
        }

        // Penalty for average distance from walls
        double sumDist = 0;
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int dist = grid.getWallDistance(gx + dx, gy + dy);
                sumDist += (dist == Integer.MAX_VALUE ? 1000 : dist);
            }
        }
        double avgDist = sumDist / (w * h);
        score -= avgDist * 5.0;

        return score;
    }
}