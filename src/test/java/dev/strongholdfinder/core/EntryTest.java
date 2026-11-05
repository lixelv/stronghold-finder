package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EntryTest {
    private static final String[] EMPTY = {"", "", ""};

    private static List<ThrowInput> throwsOf(String text) {
        return assertInstanceOf(ParsedPaste.Throws.class, EntryParser.parsePaste(text)).throwsList();
    }

    private static void assertEntry(EntryResult r, String[] fields, int focus, int invalid, boolean commit) {
        assertArrayEquals(fields, r.fields());
        assertEquals(focus, r.focus());
        assertEquals(invalid, r.invalid());
        assertEquals(commit, r.commit());
    }

    @Test
    void numAcceptsIntsFloatsCommaDecimalsAndUnicodeMinus() {
        assertEquals(12.0, EntryParser.num("12"));
        assertEquals(-12.5, EntryParser.num("-12.5"));
        assertEquals(-12.5, EntryParser.num("−12,5"));
        assertEquals(0.5, EntryParser.num(".5"));
    }

    @Test
    void numRejectsEverythingElse() {
        for (String v : new String[] {"", "abc", "1.2.3", "-", "1 2"}) {
            assertNull(EntryParser.num(v), v);
        }
    }

    @Test
    void pasteReadsThreeNumbersSeparatedByAnyWhitespace() {
        List<ThrowInput> want = List.of(new ThrowInput(120, 310.5, -47.3));
        assertEquals(want, throwsOf("120 310.5 -47.3"));
        assertEquals(want, throwsOf("120\t310.5\t-47.3"));
        assertEquals(want, throwsOf("  120   310.5\n-47.3\n"));
        assertEquals(want, throwsOf("120\r\n310.5\r\n-47.3"));
    }

    @Test
    void pasteReadsIntsFloatsCommaDecimalsAndUnicodeMinus() {
        assertEquals(List.of(new ThrowInput(1, 2, 3)), throwsOf("1 2 3"));
        assertEquals(List.of(new ThrowInput(-1.5, 2, 0.5)), throwsOf("-1.5 +2 .5"));
        assertEquals(List.of(new ThrowInput(100.5, -200.25, 45.5)), throwsOf("100,5 −200,25 45,5"));
    }

    @Test
    void pasteWrapsOutOfRangeYaw() {
        assertEquals(List.of(new ThrowInput(1, 2, 10)), throwsOf("1 2 370"));
    }

    @Test
    void pasteReadsSeveralThrowsOnePerLine() {
        assertEquals(List.of(new ThrowInput(1, 2, 3), new ThrowInput(4, 5, 6)), throwsOf("1 2 3\n4 5 6\n"));
    }

    @Test
    void pasteReadsF3cAndRejectsOtherDimensions() {
        assertEquals(
                List.of(new ThrowInput(-120.5, 310.25, -47.3)),
                throwsOf("/execute in minecraft:overworld run tp @s -120.50 71.00 310.25 -47.30 -32.10"));
        assertInstanceOf(
                ParsedPaste.WrongDimension.class,
                EntryParser.parsePaste("/execute in minecraft:the_nether run tp @s 1.00 2.00 3.00 4.00 5.00"));
    }

    @Test
    void pasteRejectsAnythingElse() {
        for (String t : new String[] {"", "hello", "1 2", "1 2 3 4", "1 2 x", "1 2 3\nhello"}) {
            assertInstanceOf(ParsedPaste.Invalid.class, EntryParser.parsePaste(t), t);
        }
    }

    @Test
    void typingStaysInPlaceUntilWhitespace() {
        assertEntry(FieldEntry.enterText(EMPTY, 0, "12"), new String[] {"12", "", ""}, 0, -1, false);
    }

    @Test
    void trailingWhitespaceMovesToNextField() {
        assertEntry(FieldEntry.enterText(new String[] {"12", "", ""}, 0, "12 "), new String[] {"12", "", ""}, 1, -1, false);
        assertEntry(FieldEntry.enterText(new String[] {"12", "5", ""}, 1, "5\n"), new String[] {"12", "5", ""}, 2, -1, false);
    }

    @Test
    void thirdFieldCommits() {
        assertEntry(FieldEntry.enterText(new String[] {"12", "5", "-4.5"}, 2, "-4.5 "), new String[] {"12", "5", "-4.5"}, 2, -1, true);
    }

    @Test
    void onlyWhitespaceStaysOnEmptyField() {
        assertEntry(FieldEntry.enterText(EMPTY, 0, " "), EMPTY, 0, -1, false);
    }

    @Test
    void badValueStaysAndIsFlagged() {
        assertEntry(FieldEntry.enterText(EMPTY, 0, "abc "), new String[] {"abc", "", ""}, 0, 0, false);
        assertEntry(FieldEntry.enterText(new String[] {"1", "2", ""}, 2, "- "), new String[] {"1", "2", "-"}, 2, 2, false);
    }

    @Test
    void severalValuesSpreadAcrossFields() {
        assertEntry(FieldEntry.enterText(EMPTY, 0, "120 310"), new String[] {"120", "310", ""}, 1, -1, false);
        assertEntry(FieldEntry.enterText(EMPTY, 0, "120 310 "), new String[] {"120", "310", ""}, 2, -1, false);
        assertEntry(FieldEntry.enterText(EMPTY, 0, "120 310 -47 "), new String[] {"120", "310", "-47"}, 2, -1, true);
        assertEntry(FieldEntry.enterText(new String[] {"7", "", ""}, 1, "310 -47 "), new String[] {"7", "310", "-47"}, 2, -1, true);
    }

    @Test
    void inputFieldsAreNotChangedInPlace() {
        String[] fields = {"", "", ""};
        FieldEntry.enterText(fields, 0, "120 ");
        assertTrue(fields[0].isEmpty());
        assertFalse(fields[0].equals("120"));
    }

    @Test
    void readReturnsThrowWhenAllFieldsAreNumbers() {
        assertEquals(new ThrowInput(1, 2.5, 10), FieldEntry.read(new String[] {"1", "2,5", "370"}));
    }

    @Test
    void invalidFieldsAreNamed() {
        assertNull(FieldEntry.read(new String[] {"", "2", "x"}));
        assertArrayEquals(new int[] {0, 2}, FieldEntry.invalid(new String[] {"", "2", "x"}));
    }
}
