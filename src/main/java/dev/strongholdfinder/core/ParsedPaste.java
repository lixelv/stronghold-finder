package dev.strongholdfinder.core;

import java.util.List;

public sealed interface ParsedPaste {
    record Throws(List<ThrowInput> throwsList) implements ParsedPaste {
    }

    record WrongDimension() implements ParsedPaste {
    }

    record Invalid() implements ParsedPaste {
    }
}
