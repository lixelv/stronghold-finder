package dev.strongholdfinder.core;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EntryParser {
    private static final Pattern F3C = Pattern.compile(
            "execute\\s+in\\s+(\\S+)\\s+run\\s+tp\\s+\\S+\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)\\s+(-?[\\d.]+)",
            Pattern.CASE_INSENSITIVE);
    private static final String NUMBER = "[-+−]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)";
    private static final Pattern TRIPLE = Pattern.compile("^\\s*(" + NUMBER + ")\\s+(" + NUMBER + ")\\s+(" + NUMBER + ")\\s*$");
    private static final Pattern PLAIN_NUMBER = Pattern.compile("^[-+]?(\\d+\\.?\\d*|\\.\\d+)$");

    private EntryParser() {
    }

    public static Double num(String text) {
        String t = text.trim().replaceFirst(",", ".").replaceFirst("−", "-");
        if (t.isEmpty() || !PLAIN_NUMBER.matcher(t).matches()) {
            return null;
        }
        double value = Double.parseDouble(t);
        return Double.isFinite(value) ? value : null;
    }

    private static ParsedPaste parseLine(String line) {
        Matcher f3 = F3C.matcher(line);
        if (f3.find()) {
            if (!f3.group(1).toLowerCase(java.util.Locale.ROOT).contains("overworld")) {
                return new ParsedPaste.WrongDimension();
            }
            Double x = num(f3.group(2));
            Double z = num(f3.group(4));
            Double yaw = num(f3.group(5));
            if (x == null || z == null || yaw == null) {
                return new ParsedPaste.Invalid();
            }
            return new ParsedPaste.Throws(List.of(new ThrowInput(x, z, Geometry.normYaw(yaw))));
        }
        Matcher triple = TRIPLE.matcher(line);
        if (!triple.matches()) {
            return new ParsedPaste.Invalid();
        }
        Double x = num(triple.group(1));
        Double z = num(triple.group(2));
        Double yaw = num(triple.group(3));
        if (x == null || z == null || yaw == null) {
            return new ParsedPaste.Invalid();
        }
        return new ParsedPaste.Throws(List.of(new ThrowInput(x, z, Geometry.normYaw(yaw))));
    }

    public static ParsedPaste parsePaste(String text) {
        List<String> lines = text.lines().map(String::trim).filter(l -> !l.isEmpty()).toList();
        if (lines.isEmpty()) {
            return new ParsedPaste.Invalid();
        }
        ParsedPaste whole = parseLine(String.join(" ", lines));
        if (!(whole instanceof ParsedPaste.Invalid)) {
            return whole;
        }
        List<ThrowInput> throwsList = new ArrayList<>();
        for (String line : lines) {
            ParsedPaste parsed = parseLine(line);
            if (parsed instanceof ParsedPaste.Throws found) {
                throwsList.addAll(found.throwsList());
            } else {
                return parsed;
            }
        }
        return new ParsedPaste.Throws(throwsList);
    }
}
