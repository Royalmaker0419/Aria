package com.takehome.placer;

import com.takehome.core.Grid;
import com.takehome.core.GeometryUtils;
import com.takehome.core.PolygonRasterizer;
import com.takehome.model.InputData;
import com.takehome.model.Placement;
import com.takehome.model.PlacementResult;

import java.util.*;

/**
 * Greedy sequential placement algorithm.
 * Places items largest-first, prioritizing wall-hugging positions.
 */
public class GreedyPlacer {

    private static final double CELL_SIZE = 10.0;

    // Item type priority: lower number = placed earlier
    private static final Map<String, Integer> TYPE_PRIORITY = new LinkedHashMap<>();
    static {
        TYPE_PRIORITY.put("fridge", 0);
        TYPE_PRIORITY.put("icemaker", 1);
        TYPE_PRIORITY.put("shelf", 2);
        TYPE_PRIORITY.put("overshelf", 3);
    }

    /**
     * Main entry point: try to place all items in the room.
     */
    public static PlacementResult place(InputData inputData) {
        inputData.validate();

        // Convert boundary to double array
        double[][] boundary = GeometryUtils.toDoubleArray(inputData.getBoundary());

        // Get door endpoints
        double doorStartX = inputData.getDoor().get(0)[0];
        double doorStartY = inputData.getDoor().get(0)[1];
        double doorEndX = inputData.getDoor().get(1)[0];
        double doorEndY = inputData.getDoor().get(1)[1];
        boolean isOpenInward = inputData.isOpenInward();

        // Create grid
        Grid grid = createGrid(boundary);

        // Rasterize polygon
        PolygonRasterizer.rasterize(boundary, grid, doorStartX, doorStartY, doorEndX, doorEndY);

        // Mark door area
        PolygonRasterizer.markDoorArea(boundary, doorStartX, doorStartY, doorEndX, doorEndY,
                isOpenInward, grid);

        // Compute wall distances
        PolygonRasterizer.computeWallDistances(grid);

        // Determine placement order
        List<Map.Entry<String, double[]>> orderedItems = orderItems(inputData.getAlgoToPlace());

        // Place items greedily
        Map<String, Placement> placements = new LinkedHashMap<>();

        for (Map.Entry<String, double[]> entry : orderedItems) {
            String name = entry.getKey();
            double[] dims = entry.getValue();
            String type = determineType(name);

            Placement best = findBestPlacement(grid, boundary, dims, type);
            if (best == null) {
                return PlacementResult.infeasible();
            }

            // Place the item — compute top-left corner from center
            double actualW = (best.getRotation() == 0) ? dims[0] : dims[1];
            double actualH = (best.getRotation() == 0) ? dims[1] : dims[0];
            double topLeftX = best.getCenterX() - actualW / 2.0;
            double topLeftY = best.getCenterY() - actualH / 2.0;
            int gx = grid.toGridX(topLeftX);
            int gy = grid.toGridY(topLeftY);
            int actualWCells = gridCells(actualW);
            int actualHCells = gridCells(actualH);

            grid.markOccupied(gx, gy, actualWCells, actualHCells);

            // Handle fridge clearance
            if ("fridge".equals(type)) {
                markFridgeClearance(grid, gx, gy, actualWCells, actualHCells, (int) best.getRotation());
            }

            placements.put(name, best);
        }

        return PlacementResult.success(placements);
    }

    /**
     * Create grid from boundary polygon.
     */
    private static Grid createGrid(double[][] boundary) {
        double minX = GeometryUtils.minX(boundary);
        double minY = GeometryUtils.minY(boundary);
        double maxX = GeometryUtils.maxX(boundary);
        double maxY = GeometryUtils.maxY(boundary);

        int gridWidth = (int) Math.ceil((maxX - minX) / CELL_SIZE) + 2;
        int gridHeight = (int) Math.ceil((maxY - minY) / CELL_SIZE) + 2;

        // Adjust minX/minY to be round numbers
        minX = Math.floor(minX / CELL_SIZE) * CELL_SIZE;
        minY = Math.floor(minY / CELL_SIZE) * CELL_SIZE;

        return new Grid(gridWidth, gridHeight, CELL_SIZE, minX, minY);
    }

    /**
     * Convert item dimension to grid cells.
     */
    private static int gridCells(double dim) {
        return (int) Math.round(dim / CELL_SIZE);
    }

    /**
     * Find the best placement for a single item.
     */
    private static Placement findBestPlacement(Grid grid, double[][] boundary, double[] dims, String type) {
        double length = dims[0];
        double width = dims[1];

        int lenCells = gridCells(length);
        int widCells = gridCells(width);

        // Try both rotations
        // rotation 0: w = lenCells, h = widCells
        // rotation 90: w = widCells, h = lenCells
        int[][] rotations = {{lenCells, widCells}, {widCells, lenCells}};
        int[] rotationAngles = {0, 90};

        Placement bestPlacement = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int r = 0; r < rotations.length; r++) {
            int w = rotations[r][0];
            int h = rotations[r][1];
            int rotation = rotationAngles[r];

            // Actual dimensions in original units for this rotation
            double actW = (rotation == 0) ? length : width;
            double actH = (rotation == 0) ? width : length;

            for (int gy = 0; gy <= grid.getHeight() - h; gy++) {
                for (int gx = 0; gx <= grid.getWidth() - w; gx++) {
                    if (!grid.canPlace(gx, gy, w, h)) continue;

                    // Compute item corners in original coordinates
                    double left = grid.toOriginalX(gx) - grid.getCellSize() / 2.0;
                    double right = left + actW;
                    double top = grid.toOriginalY(gy) - grid.getCellSize() / 2.0;
                    double bottom = top + actH;

                    // Check all 4 corners are inside the polygon
                    if (!GeometryUtils.pointInPolygon(boundary, left, top)
                            || !GeometryUtils.pointInPolygon(boundary, right, top)
                            || !GeometryUtils.pointInPolygon(boundary, right, bottom)
                            || !GeometryUtils.pointInPolygon(boundary, left, bottom)) {
                        continue;
                    }

                    double score = PlacementScorer.score(grid, gx, gy, w, h);

                    if (score > bestScore) {
                        bestScore = score;
                        // Compute center coordinates in original space
                        double cx = (left + right) / 2.0;
                        double cy = (top + bottom) / 2.0;
                        bestPlacement = new Placement(cx, cy, rotation);
                    }
                }
            }
        }

        return bestPlacement;
    }

    /**
     * Mark fridge door-side clearance as BLOCKED.
     * The fridge length is the door side. One side of the length side should face inward.
     */
    private static void markFridgeClearance(Grid grid, int gx, int gy, int w, int h, int rotation) {
        // Fridge: length=1220 (122 cells), width=1330 (133 cells)
        // Rotation 0: w=122 (length is horizontal), h=133 (width is vertical)
        //   → door side candidates: top (gy-1) or bottom (gy+h)
        // Rotation 90: w=133 (width is horizontal), h=122 (length is vertical)
        //   → door side candidates: left (gx-1) or right (gx+w)

        // Determine which sides are against walls
        boolean topWall = gy > 0 && grid.allCellsAre(gx, gy - 1, w, 1, Grid.BOUNDARY);
        boolean bottomWall = gy + h < grid.getHeight()
                && grid.allCellsAre(gx, gy + h, w, 1, Grid.BOUNDARY);
        boolean leftWall = gx > 0 && grid.allCellsAre(gx - 1, gy, 1, h, Grid.BOUNDARY);
        boolean rightWall = gx + w < grid.getWidth()
                && grid.allCellsAre(gx + w, gy, 1, h, Grid.BOUNDARY);

        // Determine which sides are "length" sides based on rotation
        // Rotation 0: length is horizontal (w dimension), door sides are top and bottom
        // Rotation 90: length is vertical (h dimension), door sides are left and right
        boolean lengthIsHorizontal = (rotation == 0);

        int doorSide = -1; // 0=top, 1=bottom, 2=left, 3=right

        if (lengthIsHorizontal) {
            // Door side is top or bottom
            if (topWall && !bottomWall) {
                doorSide = 1; // bottom is door side (opposite to wall)
            } else if (bottomWall && !topWall) {
                doorSide = 0; // top is door side
            } else {
                // Both or neither: pick the side with more free cells
                int topFree = gy > 0 ? grid.countFreeCells(gx, gy - 1, w, 1) : 0;
                int bottomFree = gy + h < grid.getHeight()
                        ? grid.countFreeCells(gx, gy + h, w, 1) : 0;
                doorSide = (topFree >= bottomFree) ? 0 : 1;
            }
        } else {
            // Door side is left or right
            if (leftWall && !rightWall) {
                doorSide = 3; // right is door side
            } else if (rightWall && !leftWall) {
                doorSide = 2; // left is door side
            } else {
                int leftFree = gx > 0 ? grid.countFreeCells(gx - 1, gy, 1, h) : 0;
                int rightFree = gx + w < grid.getWidth()
                        ? grid.countFreeCells(gx + w, gy, 1, h) : 0;
                doorSide = (leftFree >= rightFree) ? 2 : 3;
            }
        }

        // Mark clearance strip (1 cell = 10 units)
        int clearanceDepth = 1;
        switch (doorSide) {
            case 0: // top
                for (int dx = 0; dx < w; dx++) {
                    for (int d = 1; d <= clearanceDepth; d++) {
                        grid.setState(gx + dx, gy - d, Grid.BLOCKED);
                    }
                }
                break;
            case 1: // bottom
                for (int dx = 0; dx < w; dx++) {
                    for (int d = 0; d < clearanceDepth; d++) {
                        grid.setState(gx + dx, gy + h + d, Grid.BLOCKED);
                    }
                }
                break;
            case 2: // left
                for (int dy = 0; dy < h; dy++) {
                    for (int d = 1; d <= clearanceDepth; d++) {
                        grid.setState(gx - d, gy + dy, Grid.BLOCKED);
                    }
                }
                break;
            case 3: // right
                for (int dy = 0; dy < h; dy++) {
                    for (int d = 0; d < clearanceDepth; d++) {
                        grid.setState(gx + w + d, gy + dy, Grid.BLOCKED);
                    }
                }
                break;
        }
    }

    /**
     * Determine item type from name.
     */
    private static String determineType(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("fridge")) return "fridge";
        if (lower.startsWith("icemaker")) return "icemaker";
        if (lower.startsWith("overshelf")) return "overshelf";
        if (lower.startsWith("shelf")) return "shelf";
        return "unknown";
    }

    /**
     * Order items by priority: fridge first, then by type, then by area (largest first).
     */
    private static List<Map.Entry<String, double[]>> orderItems(Map<String, double[]> items) {
        List<Map.Entry<String, double[]>> list = new ArrayList<>(items.entrySet());
        list.sort(Comparator
                .<Map.Entry<String, double[]>>comparingInt(e -> {
                    String type = determineType(e.getKey());
                    return TYPE_PRIORITY.getOrDefault(type, Integer.MAX_VALUE);
                })
                .thenComparing(e -> -area(e.getValue()))
                .thenComparing(Map.Entry::getKey));
        return list;
    }

    private static int area(double[] dims) {
        return (int) (dims[0] * dims[1]);
    }
}