package com.takehome.core;

/**
 * 2D grid representation of the room with occupancy and wall distance information.
 * Cell states: 0 = FREE, 1 = OCCUPIED, 2 = BLOCKED, 3 = BOUNDARY, 4 = EXTERIOR
 */
public class Grid {

    // Cell state constants
    public static final byte FREE = 0;
    public static final byte OCCUPIED = 1;
    public static final byte BLOCKED = 2;
    public static final byte BOUNDARY = 3;
    public static final byte EXTERIOR = 4;

    private final int width;
    private final int height;
    private final byte[][] state;
    private final int[][] wallDist;
    private final double cellSize;
    private final double minX;
    private final double minY;

    /**
     * Create a new grid with given dimensions and coordinate offsets.
     */
    public Grid(int width, int height, double cellSize, double minX, double minY) {
        this.width = width;
        this.height = height;
        this.cellSize = cellSize;
        this.minX = minX;
        this.minY = minY;
        this.state = new byte[height][width];
        this.wallDist = new int[height][width];

        // Initialize all cells as EXTERIOR
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                state[y][x] = EXTERIOR;
                wallDist[y][x] = Integer.MAX_VALUE;
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getCellSize() {
        return cellSize;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    /**
     * Check if grid coordinates are in bounds.
     */
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Get cell state.
     */
    public byte getState(int x, int y) {
        return inBounds(x, y) ? state[y][x] : EXTERIOR;
    }

    /**
     * Set cell state.
     */
    public void setState(int x, int y, byte newState) {
        if (inBounds(x, y)) {
            state[y][x] = newState;
        }
    }

    /**
     * Get wall distance for cell.
     */
    public int getWallDistance(int x, int y) {
        return inBounds(x, y) ? wallDist[y][x] : Integer.MAX_VALUE;
    }

    /**
     * Set wall distance for cell.
     */
    public void setWallDistance(int x, int y, int dist) {
        if (inBounds(x, y)) {
            wallDist[y][x] = dist;
        }
    }

    /**
     * Get the wall distance array for modification.
     */
    public int[][] getWallDistArray() {
        return wallDist;
    }

    /**
     * Check if a rectangle of size w x h can be placed at (gx, gy).
     * All cells must be FREE.
     */
    public boolean canPlace(int gx, int gy, int w, int h) {
        if (gx < 0 || gx + w > width || gy < 0 || gy + h > height) {
            return false;
        }
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (state[gy + dy][gx + dx] != FREE) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Mark a rectangle as occupied.
     */
    public void markOccupied(int gx, int gy, int w, int h) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                state[gy + dy][gx + dx] = OCCUPIED;
            }
        }
    }

    /**
     * Mark a rectangle as blocked.
     */
    public void markBlocked(int gx, int gy, int w, int h) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (state[gy + dy][gx + dx] == FREE || state[gy + dy][gx + dx] == BOUNDARY) {
                    state[gy + dy][gx + dx] = BLOCKED;
                }
            }
        }
    }

    /**
     * Check if all cells in rectangle are of the given state.
     */
    public boolean allCellsAre(int gx, int gy, int w, int h, byte targetState) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (!inBounds(gx + dx, gy + dy) || state[gy + dy][gx + dx] != targetState) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Count number of FREE cells in rectangle.
     */
    public int countFreeCells(int gx, int gy, int w, int h) {
        int count = 0;
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                if (inBounds(gx + dx, gy + dy) && state[gy + dy][gx + dx] == FREE) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Convert grid x to original coordinate.
     * Returns center of cell.
     */
    public double toOriginalX(int gridX) {
        return (gridX + 0.5) * cellSize + minX;
    }

    /**
     * Convert grid y to original coordinate.
     * Returns center of cell.
     */
    public double toOriginalY(int gridY) {
        return (gridY + 0.5) * cellSize + minY;
    }

    /**
     * Convert original x to grid coordinate (rounded).
     */
    public int toGridX(double originalX) {
        return (int) Math.round((originalX - minX) / cellSize);
    }

    /**
     * Convert original y to grid coordinate (rounded).
     */
    public int toGridY(double originalY) {
        return (int) Math.round((originalY - minY) / cellSize);
    }
}