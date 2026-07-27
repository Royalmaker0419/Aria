package com.takehome.core;

/**
 * Geometry utilities for point-in-polygon tests, distance calculations, etc.
 */
public class GeometryUtils {

    private GeometryUtils() {
        // utility class
    }

    /**
     * Point-in-polygon test using ray casting algorithm.
     * @param polygon list of polygon vertices as [x, y] arrays
     * @param px test point x
     * @param py test point y
     * @return true if point is inside polygon
     */
    public static boolean pointInPolygon(double[][] polygon, double px, double py) {
        boolean inside = false;
        int n = polygon.length;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon[i][0], yi = polygon[i][1];
            double xj = polygon[j][0], yj = polygon[j][1];

            // Check if the edge crosses the horizontal ray
            if (((yi > py) != (yj > py))) {
                // Compute x intersection
                double xIntersect = ( (py - yi) * (xj - xi) ) / (yj - yi) + xi;
                if (px < xIntersect) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    /**
     * Compute signed area of polygon.
     * Positive area means counter-clockwise orientation.
     */
    public static double signedArea(double[][] polygon) {
        double area = 0;
        int n = polygon.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area += polygon[i][0] * polygon[j][1] - polygon[j][0] * polygon[i][1];
        }
        return 0.5 * area;
    }

    /**
     * Compute Euclidean distance between two points.
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Compute the minimum x value from vertices.
     */
    public static double minX(double[][] vertices) {
        double min = Double.MAX_VALUE;
        for (double[] v : vertices) {
            if (v[0] < min) min = v[0];
        }
        return min;
    }

    /**
     * Compute the maximum x value from vertices.
     */
    public static double maxX(double[][] vertices) {
        double max = Double.MIN_VALUE;
        for (double[] v : vertices) {
            if (v[0] > max) max = v[0];
        }
        return max;
    }

    /**
     * Compute the minimum y value from vertices.
     */
    public static double minY(double[][] vertices) {
        double min = Double.MAX_VALUE;
        for (double[] v : vertices) {
            if (v[1] < min) min = v[1];
        }
        return min;
    }

    /**
     * Compute the maximum y value from vertices.
     */
    public static double maxY(double[][] vertices) {
        double max = Double.MIN_VALUE;
        for (double[] v : vertices) {
            if (v[1] > max) max = v[1];
        }
        return max;
    }

    /**
     * Convert to 2D double array from list of [x,y] arrays.
     */
    public static double[][] toDoubleArray(java.util.List<double[]> list) {
        double[][] result = new double[list.size()][2];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).clone();
        }
        return result;
    }
}