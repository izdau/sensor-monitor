package com.somecompany.util;

import com.somecompany.dto.Point;

import java.util.List;

public class GeofenceUtil {

    public static boolean isInsideGeofence(Point point, List<Point> polygon) {
        int n = polygon.size();
        boolean inside = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point pi = polygon.get(i);
            Point pj = polygon.get(j);

            boolean intersects = ((pi.lat() > point.lat()) != (pj.lat() > point.lat()))
                    && (point.lon() < (pj.lon() - pi.lon()) * (point.lat() - pi.lat())
                    / (pj.lat() - pi.lat()) + pi.lon());

            if (intersects) {
                inside = !inside;
            }
        }

        return inside;
    }
}