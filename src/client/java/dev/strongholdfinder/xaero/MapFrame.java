package dev.strongholdfinder.xaero;

import dev.strongholdfinder.core.LabelPlacer;
import dev.strongholdfinder.core.Pair;
import dev.strongholdfinder.core.Solution;
import dev.strongholdfinder.core.Throw;
import dev.strongholdfinder.core.ThrowStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;

/** What the layer draws this frame: the marks in order and where their labels go, all in pixels from the view center. */
final class MapFrame {
    static final int LABEL_HEIGHT = 9;
    static final int MEAN_PADDING = 3;

    final List<MapMark> marks = new ArrayList<>();
    final Map<String, LabelPlacer.Box> pairLabels = new HashMap<>();
    LabelPlacer.Box meanLabel;
    double diagonal;
    double cameraX;
    double cameraZ;
    double scale;
    float ui;

    void clear() {
        marks.clear();
        pairLabels.clear();
        meanLabel = null;
    }

    void build(ThrowStore store, Font font, double width, double height, double cameraX, double cameraZ, double scale, float ui) {
        clear();
        this.cameraX = cameraX;
        this.cameraZ = cameraZ;
        this.scale = scale;
        this.ui = ui;
        diagonal = Math.hypot(width, height);

        Solution solution = store.solution();
        List<Throw> throwsList = store.throwsList();
        List<Pair> crossings = solution.pairs().stream().filter(Pair::usable).toList();

        throwsList.forEach(t -> marks.add(new MapMark.RayMark(t)));
        if (solution.mean() != null) {
            marks.add(new MapMark.ErrorCircleMark(solution.mean(), solution.err()));
        }
        crossings.forEach(p -> marks.add(new MapMark.PairMark(p)));
        throwsList.forEach(t -> marks.add(new MapMark.ThrowMark(t)));
        if (solution.mean() != null) {
            marks.add(new MapMark.MeanMark(solution.mean(), solution.err()));
        }

        LabelPlacer placer = new LabelPlacer(-width / 2, -height / 2, width / 2, height / 2);
        double throwHalf = 8 * ui;
        for (Throw t : throwsList) {
            placer.reserve(px(t.x()) - throwHalf, py(t.z()) - throwHalf, px(t.x()) + throwHalf, py(t.z()) + throwHalf);
        }
        double dotHalf = 4 * ui;
        for (Pair p : crossings) {
            placer.reserve(px(p.x()) - dotHalf, py(p.z()) - dotHalf, px(p.x()) + dotHalf, py(p.z()) + dotHalf);
        }
        if (solution.mean() != null) {
            String text = meanText(solution);
            double w = (font.width(text) + MEAN_PADDING * 2) * ui;
            double h = (LABEL_HEIGHT + MEAN_PADDING * 2) * ui;
            meanLabel = placer.place(px(solution.mean().x()), py(solution.mean().z()), w, h, 10 * ui, true);
        }
        List<Pair> byPriority = crossings.stream().sorted(Comparator.comparing(Pair::used).reversed()).toList();
        for (Pair p : byPriority) {
            if (!p.used()) {
                continue;
            }
            double w = font.width(pairText(p)) * ui;
            LabelPlacer.Box box = placer.place(px(p.x()), py(p.z()), w, LABEL_HEIGHT * ui, 5 * ui, false);
            if (box != null) {
                pairLabels.put(p.key(), box);
            }
        }
    }

    double px(double x) {
        return (x - cameraX) * scale;
    }

    double py(double z) {
        return (z - cameraZ) * scale;
    }

    static String meanText(Solution solution) {
        return "X " + Math.round(solution.mean().x()) + ", Z " + Math.round(solution.mean().z());
    }

    static String pairText(Pair pair) {
        return Math.round(pair.x()) + ", " + Math.round(pair.z());
    }
}
