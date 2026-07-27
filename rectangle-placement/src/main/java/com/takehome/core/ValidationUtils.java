package com.takehome.core;

import com.takehome.model.Placement;
import com.takehome.model.PlacementResult;

import java.util.List;
import java.util.Map;

/**
 * Validation utilities to check placement correctness.
 */
public class ValidationUtils {

    private ValidationUtils() {
        // utility class
    }

    /**
     * Check if two placed rectangles overlap.
     */
    public static boolean rectsOverlap(double cx1, double cy1, double w1, double h1, int rot1,
                                        double cx2, double cy2, double w2, double h2, int rot2,
                                        double tolerance) {
        double r1w = (rot1 == 0) ? w1 : h1;
        double r1h = (rot1 == 0) ? h1 : w1;
        double r2w = (rot2 == 0) ? w2 : h2;
        double r2h = (rot2 == 0) ? h2 : w2;

        double r1Left = cx1 - r1w / 2.0 + tolerance;
        double r1Right = cx1 + r1w / 2.0 - tolerance;
        double r1Top = cy1 - r1h / 2.0 + tolerance;
        double r1Bottom = cy1 + r1h / 2.0 - tolerance;

        double r2Left = cx2 - r2w / 2.0 + tolerance;
        double r2Right = cx2 + r2w / 2.0 - tolerance;
        double r2Top = cy2 - r2h / 2.0 + tolerance;
        double r2Bottom = cy2 + r2h / 2.0 - tolerance;

        return !(r1Right <= r2Left || r2Right <= r1Left
                || r1Bottom <= r2Top || r2Bottom <= r1Top);
    }

    /**
     * Check if a rectangle is fully inside the polygon.
     */
    public static boolean rectInsidePolygon(double cx, double cy, double w, double h, int rot,
                                             double[][] polygon) {
        double rw = (rot == 0) ? w : h;
        double rh = (rot == 0) ? h : w;

        // Check 4 corners and center
        double[][] corners = {
                {cx - rw / 2.0, cy - rh / 2.0},
                {cx + rw / 2.0, cy - rh / 2.0},
                {cx + rw / 2.0, cy + rh / 2.0},
                {cx - rw / 2.0, cy + rh / 2.0},
                {cx, cy}
        };

        for (double[] corner : corners) {
            if (!GeometryUtils.pointInPolygon(polygon, corner[0], corner[1])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validate a placement result against the input data.
     * Returns a list of error messages (empty if all valid).
     */
    public static java.util.List<String> validate(double[][] boundary,
                                                    double doorStartX, double doorStartY,
                                                    double doorEndX, double doorEndY,
                                                    boolean isOpenInward,
                                                    Map<String, double[]> items,
                                                    PlacementResult result) {
        java.util.List<String> errors = new java.util.ArrayList<>();

        if (!result.isFeasible()) {
            return errors; // Infeasible is valid
        }

        Map<String, Placement> placements = result.getPlacements();

        // Check all items are placed
        for (String name : items.keySet()) {
            if (!placements.containsKey(name)) {
                errors.add("Missing placement for: " + name);
            }
        }

        if (!errors.isEmpty()) return errors;

        // Check no overlaps
        List<String> names = new java.util.ArrayList<>(placements.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                String name1 = names.get(i);
                String name2 = names.get(j);
                Placement p1 = placements.get(name1);
                Placement p2 = placements.get(name2);
                double[] d1 = items.get(name1);
                double[] d2 = items.get(name2);

                if (rectsOverlap(p1.getCenterX(), p1.getCenterY(), d1[0], d1[1], p1.getRotation(),
                        p2.getCenterX(), p2.getCenterY(), d2[0], d2[1], p2.getRotation(), 0.1)) {
                    errors.add("Overlap: " + name1 + " and " + name2);
                }
            }
        }

        // Check all items inside polygon
        for (String name : names) {
            Placement p = placements.get(name);
            double[] d = items.get(name);

            if (!rectInsidePolygon(p.getCenterX(), p.getCenterY(), d[0], d[1], p.getRotation(), boundary)) {
                errors.add("Item outside polygon: " + name);
            }
        }

        // Check door area for inward doors
        if (isOpenInward) {
            double doorWidth = GeometryUtils.distance(doorStartX, doorStartY, doorEndX, doorEndY);
            double mx = (doorStartX + doorEndX) / 2.0;
            double my = (doorStartY + doorEndY) / 2.0;
            double dx = doorEndX - doorStartX;
            double dy = doorEndY - doorStartY;

            double nx1 = -dy, ny1 = dx;
            double len1 = Math.sqrt(nx1 * nx1 + ny1 * ny1);
            nx1 /= len1; ny1 /= len1;
            double nx2 = dy, ny2 = -dx;
            double len2 = Math.sqrt(nx2 * nx2 + ny2 * ny2);
            nx2 /= len2; ny2 /= len2;

            double[] inNormal;
            if (GeometryUtils.pointInPolygon(boundary, mx + nx1, my + ny1)) {
                inNormal = new double[]{nx1, ny1};
            } else {
                inNormal = new double[]{nx2, ny2};
            }

            for (String name : names) {
                Placement p = placements.get(name);
                double[] d = items.get(name);

                double rw = (p.getRotation() == 0) ? d[0] : d[1];
                double rh = (p.getRotation() == 0) ? d[1] : d[0];

                double[][] corners = {
                        {p.getCenterX() - rw / 2.0, p.getCenterY() - rh / 2.0},
                        {p.getCenterX() + rw / 2.0, p.getCenterY() - rh / 2.0},
                        {p.getCenterX() + rw / 2.0, p.getCenterY() + rh / 2.0},
                        {p.getCenterX() - rw / 2.0, p.getCenterY() + rh / 2.0},
                };

                for (double[] corner : corners) {
                    if (isPointInDoorArea(corner[0], corner[1], doorStartX, doorStartY,
                            doorEndX, doorEndY, inNormal[0], inNormal[1], doorWidth)) {
                        errors.add("Item in door area: " + name);
                        break;
                    }
                }
            }
        }

        return errors;
    }

    private static boolean isPointInDoorArea(double px, double py,
                                              double dsx, double dsy,
                                              double dex, double dey,
                                              double inNx, double inNy,
                                              double doorWidth) {
        double dx = dex - dsx;
        double dy = dey - dsy;
        double len = Math.sqrt(dx * dx + dy * dy);
        double ux = dx / len;
        double uy = dy / len;
        double vx = px - dsx;
        double vy = py - dsy;
        double projAlong = vx * ux + vy * uy;
        double projNormal = vx * inNx + vy * inNy;
        return projAlong >= -1 && projAlong <= doorWidth + 1
                && projNormal >= 0 && projNormal <= doorWidth + 1;
    }
}