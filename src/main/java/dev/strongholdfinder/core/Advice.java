package dev.strongholdfinder.core;

import java.util.ArrayList;
import java.util.List;

public final class Advice {
    private Advice() {
    }

    public static List<AdviceLine> of(Solution s) {
        List<AdviceLine> out = new ArrayList<>();
        int n = s.on().size();
        boolean anyBehind = s.pairs().stream().anyMatch(p -> p.status() == PairStatus.BEHIND);
        if (s.mean() == null) {
            if (n == 0) {
                out.add(new AdviceLine("advice.first_throw", false));
            } else if (n == 1) {
                double yaw = s.on().get(0).yaw();
                out.add(new AdviceLine("advice.move_aside", false, Geometry.compass(yaw + 90), Geometry.compass(yaw - 90)));
            } else if (anyBehind) {
                out.add(new AdviceLine("advice.behind", true));
            } else {
                out.add(new AdviceLine("advice.parallel", true));
            }
            return out;
        }
        if (!s.ring().inside()) {
            out.add(new AdviceLine("advice.outside_rings", true, s.ring().n(), Math.round(s.ring().gap())));
        }
        if (anyBehind) {
            out.add(new AdviceLine("advice.behind_some", true));
        }
        if (s.weakFallback()) {
            out.add(new AdviceLine("advice.weak", true, Math.round(s.minAngle() * 10) / 10.0));
        }
        if (s.err() > 30) {
            long need = Math.max(100, Math.min(1500, Math.round(s.toLast() * 0.27 / 50) * 50));
            double y = Geometry.yawTo(new Point(s.last().x(), s.last().z()), s.mean());
            out.add(new AdviceLine("advice.refine", false, need, Geometry.compass(y + 90), Geometry.compass(y - 90)));
        } else if (out.isEmpty()) {
            out.add(new AdviceLine("advice.go", false));
        }
        return out;
    }
}
