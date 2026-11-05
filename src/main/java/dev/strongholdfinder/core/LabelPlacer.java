package dev.strongholdfinder.core;

import java.util.ArrayList;
import java.util.List;

/** Finds a free spot for each label around its point, so labels do not cover each other. */
public final class LabelPlacer {
    public record Box(double left, double top, double right, double bottom) {
        public boolean overlaps(Box other) {
            return overlapArea(other) > 0;
        }

        private double overlapArea(Box other) {
            double width = Math.min(right, other.right) - Math.max(left, other.left);
            double height = Math.min(bottom, other.bottom) - Math.max(top, other.top);
            return width > 0 && height > 0 ? width * height : 0;
        }
    }

    private static final double OUT_OF_BOUNDS = 1e9;

    private final Box bounds;
    private final List<Box> taken = new ArrayList<>();

    public LabelPlacer(double left, double top, double right, double bottom) {
        bounds = new Box(left, top, right, bottom);
    }

    public void reserve(double left, double top, double right, double bottom) {
        taken.add(new Box(left, top, right, bottom));
    }

    /** Returns the chosen box, or null when every spot is taken and {@code force} is false. */
    public Box place(double x, double y, double width, double height, double gap, boolean force) {
        double[][] spots = {
            {gap, -height / 2},
            {-gap - width, -height / 2},
            {gap, -height - gap / 2},
            {-gap - width, -height - gap / 2},
            {gap, gap / 2},
            {-gap - width, gap / 2},
            {-width / 2, -height - gap},
            {-width / 2, gap},
        };
        Box best = null;
        double bestScore = Double.MAX_VALUE;
        for (double[] spot : spots) {
            Box box = new Box(x + spot[0], y + spot[1], x + spot[0] + width, y + spot[1] + height);
            double score = score(box);
            if (score < bestScore) {
                bestScore = score;
                best = box;
            }
        }
        if (bestScore > 0 && !force) {
            return null;
        }
        taken.add(best);
        return best;
    }

    private double score(Box box) {
        double score = 0;
        if (box.left < bounds.left || box.top < bounds.top || box.right > bounds.right || box.bottom > bounds.bottom) {
            score += OUT_OF_BOUNDS;
        }
        for (Box other : taken) {
            score += box.overlapArea(other);
        }
        return score;
    }
}
