package dev.strongholdfinder.core;

import java.math.BigDecimal;
import java.util.Locale;

/** How a recorded throw's numbers read on screen. */
public final class NumberText {
    public static final int COORDINATE_DECIMALS = 3;
    public static final int ANGLE_DECIMALS = 6;

    private NumberText() {
    }

    public static String coordinate(double value) {
        return fixed(value, COORDINATE_DECIMALS);
    }

    public static String angle(double value) {
        return fixed(value, ANGLE_DECIMALS);
    }

    /** Every digit the number holds, for places with room to show them. */
    public static String exact(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    public static String fixed(double value, int decimals) {
        String text = String.format(Locale.ROOT, "%." + decimals + "f", value);
        return text.matches("-0(\\.0+)?") ? text.substring(1) : text;
    }
}
