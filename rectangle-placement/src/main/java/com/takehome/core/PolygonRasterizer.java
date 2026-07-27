package com.takehome.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rasterizes a polygon onto the grid using scanline ray casting.
 * Marks interior cells as FREE, boundary cells as BOUNDARY, and exterior as EXTERIOR.
 */
public class PolygonRasterizer {

    private PolygonRasterizer() {
        // utility class
    }

    /**
     * Rasterize the boundary polygon onto the grid.
     * @param polygonVertices the polygon vertices as [x, y] arrays
     * @param grid the grid to rasterize into
     * @param doorStartX door start x (to skip door edge), or NaN if no door
     * @param doorStartY door start y
     * @param doorEndX door end x
     * @param doorEndY door end y
     */
    public static void rasterize(double[][] polygonVertices, Grid grid,
                                  double doorStartX, double doorStartY,
                                  double doorEndX, double doorEndY) {
        double cellSize = grid.getCellSize();
        double minX = grid.getMinX();
        double minY = grid.getMinY();

        int n = polygonVertices.length;

        // Step 1: Scanline interior marking
        for (int gy = 0; gy < grid.getHeight(); gy++) {
            double y = (gy + 0.5) * cellSize + minY;
            List<Double> intersections = new ArrayList<>();

            for (int i = 0, j = n - 1; i < n; j = i++) {
                double x1 = polygonVertices[i][0];
                double y1 = polygonVertices[i][1];
                double x2 = polygonVertices[j][0];
                double y2 = polygonVertices[j][1];

                // Skip horizontal edges (they don't create useful intersections)
                if (y1 == y2) continue;

                // Standard ray casting: edge crosses the scanline if
                // one endpoint is strictly above or at y, and the other is strictly above
                if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
                    double x = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                    intersections.add(x - minX);
                }
            }

            Collections.sort(intersections);

            // Pair intersections: [x0, x1], [x2, x3], ... mark as interior
            for (int k = 0; k + 1 < intersections.size(); k += 2) {
                double xStart = intersections.get(k);
                double xEnd = intersections.get(k + 1);

                int gxStart = (int) Math.round(xStart / cellSize);
                int gxEnd = (int) Math.round(xEnd / cellSize);

                // Clamp to grid bounds
                gxStart = Math.max(0, gxStart);
                gxEnd = Math.min(grid.getWidth() - 1, gxEnd);

                for (int gx = gxStart; gx <= gxEnd; gx++) {
                    grid.setState(gx, gy, Grid.FREE);
                }
            }
        }

        // Step 2: Mark boundary cells by rasterizing each edge
        for (int i = 0; i < n; i++) {
            double x1 = polygonVertices[i][0];
            double y1 = polygonVertices[i][1];
            double x2 = polygonVertices[(i + 1) % n][0];
            double y2 = polygonVertices[(i + 1) % n][1];

            // Skip door edges
            if (isSameEdge(x1, y1, x2, y2, doorStartX, doorStartY, doorEndX, doorEndY)) {
                continue;
            }

            rasterizeEdge(x1, y1, x2, y2, grid);
        }

        // Step 3: Mark boundary-adjacent cells as BOUNDARY
        // A cell is BOUNDARY if any of its 4 neighbors is EXTERIOR
        for (int gy = 0; gy < grid.getHeight(); gy++) {
            for (int gx = 0; gx < grid.getWidth(); gx++) {
                if (grid.getState(gx, gy) == Grid.FREE) {
                    if (hasExteriorNeighbor(grid, gx, gy)) {
                        grid.setState(gx, gy, Grid.BOUNDARY);
                    }
                }
            }
        }
    }

    /**
     * Rasterize a line segment onto the grid, marking cells as BOUNDARY.
     */
    private static void rasterizeEdge(double x1, double y1, double x2, double y2, Grid grid) {
        double cellSize = grid.getCellSize();
        double minX = grid.getMinX();
        double minY = grid.getMinY();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length < 0.01) return;

        // Walk along the edge in small steps
        double step = cellSize / 3.0;
        int steps = (int) (length / step) + 1;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = x1 + t * dx;
            double y = y1 + t * dy;

            int gx = (int) Math.round((x - minX) / cellSize);
            int gy = (int) Math.round((y - minY) / cellSize);

            if (grid.inBounds(gx, gy)) {
                grid.setState(gx, gy, Grid.BOUNDARY);
            }
        }
    }

    /**
     * Check if 4 edges of a cell have any EXTERIOR neighbor.
     */
    private static boolean hasExteriorNeighbor(Grid grid, int gx, int gy) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = gx + d[0];
            int ny = gy + d[1];
            if (grid.getState(nx, ny) == Grid.EXTERIOR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if two edges represent the same line segment (for door detection).
     * Uses tolerance for floating point comparison.
     */
    private static boolean isSameEdge(double x1, double y1, double x2, double y2,
                                       double dx1, double dy1, double dx2, double dy2) {
        double tolerance = 1.0;
        // Check both directions (door might be specified in either direction)
        return (Math.abs(x1 - dx1) < tolerance && Math.abs(y1 - dy1) < tolerance
                && Math.abs(x2 - dx2) < tolerance && Math.abs(y2 - dy2) < tolerance)
                || (Math.abs(x1 - dx2) < tolerance && Math.abs(y1 - dy2) < tolerance
                && Math.abs(x2 - dx1) < tolerance && Math.abs(y2 - dy1) < tolerance);
    }

    /**
     * Compute wall distances using BFS from all BOUNDARY cells.
     */
    public static void computeWallDistances(Grid grid) {
        int height = grid.getHeight();
        int width = grid.getWidth();
        int[][] wallDist = grid.getWallDistArray();

        java.util.Queue<int[]> queue = new java.util.ArrayDeque<>();

        // Initialize: all BOUNDARY cells have distance 0
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid.getState(x, y) == Grid.BOUNDARY) {
                    wallDist[y][x] = 0;
                    queue.add(new int[]{x, y});
                } else {
                    wallDist[y][x] = Integer.MAX_VALUE;
                }
            }
        }

        // BFS
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0], cy = cur[1];
            int curDist = wallDist[cy][cx];

            for (int[] d : dirs) {
                int nx = cx + d[0], ny = cy + d[1];
                if (grid.inBounds(nx, ny)
                        && wallDist[ny][nx] == Integer.MAX_VALUE
                        && grid.getState(nx, ny) != Grid.EXTERIOR
                        && grid.getState(nx, ny) != Grid.BLOCKED) {
                    wallDist[ny][nx] = curDist + 1;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
    }

    /**
     * Mark door area. For inward doors, marks NxN area as BLOCKED.
     */
    public static void markDoorArea(double[][] polygonVertices,
                                     double doorStartX, double doorStartY,
                                     double doorEndX, double doorEndY,
                                     boolean isOpenInward, Grid grid) {
        if (!isOpenInward) {
            // For outward doors, no extra area. But we should mark the door
            // segment itself so items can't be placed on it.
            // The door gap is already handled by skipping the edge in rasterization.
            return;
        }

        double doorWidth = GeometryUtils.distance(doorStartX, doorStartY, doorEndX, doorEndY);
        if (doorWidth <= 0) return;

        // Compute inward normal
        double mx = (doorStartX + doorEndX) / 2.0;
        double my = (doorStartY + doorEndY) / 2.0;
        double dx = doorEndX - doorStartX;
        double dy = doorEndY - doorStartY;

        // Two candidate normals (perpendicular to door)
        double nx1 = -dy, ny1 = dx;  // left normal
        double nx2 = dy, ny2 = -dx;  // right normal

        // Normalize
        double len1 = Math.sqrt(nx1 * nx1 + ny1 * ny1);
        nx1 /= len1;
        ny1 /= len1;
        double len2 = Math.sqrt(nx2 * nx2 + ny2 * ny2);
        nx2 /= len2;
        ny2 /= len2;

        // Determine which normal points inward
        double testOffset = 1.0;
        double[] inwardNormal;
        if (GeometryUtils.pointInPolygon(polygonVertices, mx + nx1 * testOffset, my + ny1 * testOffset)) {
            inwardNormal = new double[]{nx1, ny1};
        } else {
            inwardNormal = new double[]{nx2, ny2};
        }

        double inNx = inwardNormal[0];
        double inNy = inwardNormal[1];

        // Mark the N x N door area by iterating over grid cells
        double cellSize = grid.getCellSize();
        double minX = grid.getMinX();
        double minY = grid.getMinY();

        for (int gy = 0; gy < grid.getHeight(); gy++) {
            for (int gx = 0; gx < grid.getWidth(); gx++) {
                double cx = grid.toOriginalX(gx);
                double cy = grid.toOriginalY(gy);

                // Check if this cell center is within the door area rectangle
                // The door area is a rectangle: door segment + N * inwardNormal
                if (isPointInDoorArea(cx, cy, doorStartX, doorStartY, doorEndX, doorEndY,
                        inNx, inNy, doorWidth)) {
                    if (grid.getState(gx, gy) == Grid.FREE || grid.getState(gx, gy) == Grid.BOUNDARY) {
                        grid.setState(gx, gy, Grid.BLOCKED);
                    }
                }
            }
        }
    }

    /**
     * Check if a point is inside the door area rectangle.
     * The rectangle is defined by the door segment and extending inward by doorWidth.
     */
    private static boolean isPointInDoorArea(double px, double py,
                                              double dsx, double dsy,
                                              double dex, double dey,
                                              double inNx, double inNy,
                                              double doorWidth) {
        // Project the point onto the door direction and normal direction
        double dx = dex - dsx;
        double dy = dey - dsy;
        double len = Math.sqrt(dx * dx + dy * dy);

        // Unit vector along door
        double ux = dx / len;
        double uy = dy / len;

        // Vector from door start to point
        double vx = px - dsx;
        double vy = py - dsy;

        // Projection along door direction
        double projAlong = vx * ux + vy * uy;

        // Projection along inward normal
        double projNormal = vx * inNx + vy * inNy;

        return projAlong >= -1 && projAlong <= doorWidth + 1
                && projNormal >= 0 && projNormal <= doorWidth + 1;
    }
}