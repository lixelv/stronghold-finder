package dev.strongholdfinder.core;

import java.util.Arrays;
import java.util.stream.IntStream;

public final class FieldEntry {
    private FieldEntry() {
    }

    /**
     * Text with whitespace is read as finished values: each token fills a field and moves on,
     * and a finished third field asks the form to add the throw.
     */
    public static EntryResult enterText(String[] fields, int index, String text) {
        String[] next = fields.clone();
        if (!text.matches("(?s).*\\s.*")) {
            next[index] = text;
            return new EntryResult(next, index, -1, false);
        }
        String[] tokens = Arrays.stream(text.split("\\s+")).filter(t -> !t.isEmpty()).toArray(String[]::new);
        boolean trailing = Character.isWhitespace(text.charAt(text.length() - 1));
        int at = index;
        for (int t = 0; t < tokens.length && at < 3; t++) {
            next[at] = tokens[t];
            boolean finished = t < tokens.length - 1 || trailing;
            if (!finished) {
                return new EntryResult(next, at, -1, false);
            }
            if (EntryParser.num(tokens[t]) == null) {
                return new EntryResult(next, at, at, false);
            }
            if (at == 2) {
                return new EntryResult(next, 2, -1, true);
            }
            at++;
        }
        return new EntryResult(next, tokens.length > 0 ? at : index, -1, false);
    }

    public static ThrowInput read(String[] fields) {
        Double x = EntryParser.num(fields[0]);
        Double z = EntryParser.num(fields[1]);
        Double yaw = EntryParser.num(fields[2]);
        if (x == null || z == null || yaw == null) {
            return null;
        }
        return new ThrowInput(x, z, Geometry.normYaw(yaw));
    }

    public static int[] invalid(String[] fields) {
        return IntStream.range(0, fields.length).filter(i -> EntryParser.num(fields[i]) == null).toArray();
    }
}
