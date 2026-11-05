package dev.strongholdfinder.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Solver {
    private static final double SIGMA = 0.2;
    private static final double MIN_ANGLE = 2;
    private static final double PARALLEL = 0.05;
    private static final double BACK = 8;

    private Solver() {
    }

    private record Intersection(PairStatus status, double angle, double x, double z) {
        private static Intersection none(PairStatus status, double angle) {
            return new Intersection(status, angle, Double.NaN, Double.NaN);
        }
    }

    private static Intersection intersect(Throw a, Throw b) {
        Point d1 = Geometry.dir(a.yaw());
        Point d2 = Geometry.dir(b.yaw());
        double s = d1.x() * d2.z() - d1.z() * d2.x();
        double wx = b.x() - a.x();
        double wz = b.z() - a.z();
        double angle = Math.toDegrees(Math.asin(Math.min(1, Math.abs(s))));
        if (Math.hypot(wx, wz) < 4) {
            return Intersection.none(PairStatus.SAME, angle);
        }
        if (angle < PARALLEL) {
            return Intersection.none(PairStatus.PARALLEL, angle);
        }
        double t1 = (wx * d2.z() - wz * d2.x()) / s;
        double t2 = (wx * d1.z() - wz * d1.x()) / s;
        if (t1 < -BACK || t2 < -BACK) {
            return Intersection.none(PairStatus.BEHIND, angle);
        }
        PairStatus status = angle < MIN_ANGLE ? PairStatus.WEAK : PairStatus.OK;
        return new Intersection(status, angle, a.x() + t1 * d1.x(), a.z() + t1 * d1.z());
    }

    private record Fit(double x, double z, double theory) {
    }

    private static Fit fitRays(List<Throw> rays, Point at) {
        double a = 0;
        double b = 0;
        double c = 0;
        double p = 0;
        double q = 0;
        for (Throw t : rays) {
            Point d = Geometry.dir(t.yaw());
            double dist = Math.max(16, Math.hypot(at.x() - t.x(), at.z() - t.z()));
            double w = 1 / (dist * dist);
            double ma = 1 - d.x() * d.x();
            double mb = -d.x() * d.z();
            double mc = 1 - d.z() * d.z();
            a += w * ma;
            b += w * mb;
            c += w * mc;
            p += w * (ma * t.x() + mb * t.z());
            q += w * (mb * t.x() + mc * t.z());
        }
        double det = a * c - b * b;
        if (!(det > 1e-30)) {
            return null;
        }
        return new Fit((c * p - b * q) / det, (a * q - b * p) / det, Math.toRadians(SIGMA) * Math.sqrt((a + c) / det));
    }

    public static Solution solve(List<Throw> throwsList, Map<String, Boolean> over) {
        List<Throw> on = throwsList.stream().filter(Throw::on).toList();
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < on.size(); i++) {
            for (int j = i + 1; j < on.size(); j++) {
                Throw a = on.get(i);
                Throw b = on.get(j);
                String key = a.id() + "-" + b.id();
                Intersection r = intersect(a, b);
                boolean usable = r.status() == PairStatus.OK || r.status() == PairStatus.WEAK;
                boolean used = usable && over.getOrDefault(key, r.status() == PairStatus.OK);
                pairs.add(new Pair(key, a, b, r.status(), r.angle(), r.x(), r.z(), usable, used));
            }
        }
        boolean weakFallback = false;
        if (pairs.stream().noneMatch(Pair::used)) {
            for (int i = 0; i < pairs.size(); i++) {
                Pair p = pairs.get(i);
                if (p.status() == PairStatus.WEAK && !over.containsKey(p.key())) {
                    pairs.set(i, new Pair(p.key(), p.a(), p.b(), p.status(), p.angle(), p.x(), p.z(), p.usable(), true));
                    weakFallback = true;
                }
            }
        }
        List<Pair> used = pairs.stream().filter(Pair::used).toList();
        Throw last = on.isEmpty() ? null : on.get(on.size() - 1);
        if (used.isEmpty()) {
            return new Solution(on, pairs, used, weakFallback, null, 0, 0, 0, null, last, 0, 0, 0, null, 0);
        }
        Point mean = new Point(
                used.stream().mapToDouble(Pair::x).average().orElseThrow(),
                used.stream().mapToDouble(Pair::z).average().orElseThrow());
        double rms = Math.sqrt(used.stream()
                .mapToDouble(p -> Math.pow(p.x() - mean.x(), 2) + Math.pow(p.z() - mean.z(), 2))
                .average()
                .orElseThrow());
        Set<Integer> ids = new HashSet<>();
        used.forEach(p -> {
            ids.add(p.a().id());
            ids.add(p.b().id());
        });
        List<Throw> rays = on.stream().filter(t -> ids.contains(t.id())).toList();
        Fit fit = fitRays(rays, mean);
        double theory = fit != null ? fit.theory() : rms;
        double err = Math.max(theory, rms);
        double origin = Math.hypot(mean.x(), mean.z());
        Lsq lsq = rays.size() >= 3 && fit != null
                ? new Lsq(fit.x(), fit.z(), Math.hypot(fit.x() - mean.x(), fit.z() - mean.z()))
                : null;
        return new Solution(
                on,
                pairs,
                used,
                weakFallback,
                mean,
                rms,
                theory,
                err,
                lsq,
                last,
                Math.hypot(mean.x() - last.x(), mean.z() - last.z()),
                Geometry.yawTo(new Point(last.x(), last.z()), mean),
                origin,
                Geometry.ringOf(origin, err + 64),
                used.stream().mapToDouble(Pair::angle).min().orElseThrow());
    }
}
