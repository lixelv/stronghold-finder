package dev.strongholdfinder.xaero;

import dev.strongholdfinder.core.Pair;
import dev.strongholdfinder.core.Point;
import dev.strongholdfinder.core.Throw;

public sealed interface MapMark {
    double x();

    double z();

    record RayMark(Throw source) implements MapMark {
        @Override
        public double x() {
            return source.x();
        }

        @Override
        public double z() {
            return source.z();
        }
    }

    record ErrorCircleMark(Point center, double radius) implements MapMark {
        @Override
        public double x() {
            return center.x();
        }

        @Override
        public double z() {
            return center.z();
        }
    }

    record PairMark(Pair pair) implements MapMark {
        @Override
        public double x() {
            return pair.x();
        }

        @Override
        public double z() {
            return pair.z();
        }
    }

    record ThrowMark(Throw source) implements MapMark {
        @Override
        public double x() {
            return source.x();
        }

        @Override
        public double z() {
            return source.z();
        }
    }

    record MeanMark(Point point, double err) implements MapMark {
        @Override
        public double x() {
            return point.x();
        }

        @Override
        public double z() {
            return point.z();
        }
    }
}
