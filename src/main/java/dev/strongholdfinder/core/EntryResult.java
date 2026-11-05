package dev.strongholdfinder.core;

/** {@code invalid} is the index of the bad field, or -1 when there is none. */
public record EntryResult(String[] fields, int focus, int invalid, boolean commit) {
}
