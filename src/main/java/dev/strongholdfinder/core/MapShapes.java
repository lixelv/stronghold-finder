package dev.strongholdfinder.core;

import java.util.ArrayList;
import java.util.List;

/** Sizes for drawing on the map, kept finite and few because each rectangle costs a render pass. */
public final class MapShapes {
    private static final double MAX_SPAN = 2e8;

    public record RaySpan(int start, int end) {
    }

    public record Band(int top, int bottom, int halfWidth) {
    }

    private MapShapes() {
    }

    /** The visible part of a ray in GUI units along the ray, or null when the throw is absurdly far away. */
    public static RaySpan raySpan(double fromCenter, double diagonal, double ui) {
        double far = (fromCenter + diagonal) / ui;
        if (!(far < MAX_SPAN)) {
            return null;
        }
        return new RaySpan((int) (Math.max(0, fromCenter - diagonal) / ui), (int) far);
    }

    public static List<Band> circleBands(double radius, int maxBands) {
        int r = (int) Math.round(radius);
        int height = Math.max(1, (int) Math.ceil(2.0 * r / maxBands));
        List<Band> bands = new ArrayList<>();
        for (int top = -r; top < r; top += height) {
            int bottom = Math.min(r, top + height);
            double middle = (top + bottom) / 2.0;
            int half = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - middle * middle)));
            bands.add(new Band(top, bottom, half));
        }
        return bands;
    }
}
