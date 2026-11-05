package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ThrowStoreTest {
    @TempDir
    Path dir;

    private ThrowStore store(String key) {
        ThrowStore store = new ThrowStore(dir);
        store.load(key);
        return store;
    }

    @Test
    void addNormalizesYaw() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 725));
        assertEquals(5, store.throwsList().get(0).yaw(), 1e-9);
    }

    @Test
    void duplicateIsRejected() {
        ThrowStore store = store("a");
        assertEquals(AddResult.ADDED, store.add(new ThrowInput(1, 2, 3)));
        assertEquals(AddResult.DUPLICATE, store.add(new ThrowInput(1, 2, 3)));
        assertEquals(1, store.throwsList().size());
    }

    @Test
    void addAllSkipsDuplicatesAndCountsAdded() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 3));
        assertEquals(1, store.addAll(List.of(new ThrowInput(1, 2, 3), new ThrowInput(4, 5, 6), new ThrowInput(4, 5, 6))));
        assertEquals(2, store.throwsList().size());
    }

    @Test
    void undoRestoresState() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 3));
        store.remove(store.lastAddedId());
        assertTrue(store.throwsList().isEmpty());
        assertTrue(store.undo());
        assertEquals(1, store.throwsList().size());
    }

    @Test
    void undoIsEmptyAfterLoad() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 3));
        store.load("b");
        assertFalse(store.canUndo());
        assertFalse(store.undo());
    }

    @Test
    void clearRestartsNumbering() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 3));
        store.clear();
        store.add(new ThrowInput(4, 5, 6));
        assertEquals(1, store.lastAddedId());
    }

    @Test
    void removeDropsPairOverrides() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(0, 0, -45));
        store.add(new ThrowInput(100, 0, 45));
        store.setPair("1-2", false);
        assertEquals(0, store.solution().used().size());
        store.remove(2);
        store.add(new ThrowInput(200, 0, 45));
        store.add(new ThrowInput(100, 0, 45));
        assertEquals(3, store.solution().pairs().size());
        assertTrue(store.solution().used().size() > 0);
    }

    @Test
    void survivesReload() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 3));
        store.add(new ThrowInput(4, 5, 6));
        store.setOn(1, false);
        store.setPair("1-2", true);
        store.setOverlayVisible(false);

        ThrowStore again = store("a");
        assertEquals(store.throwsList(), again.throwsList());
        assertFalse(again.throwsList().get(0).on());
        assertFalse(again.overlayVisible());
        again.add(new ThrowInput(7, 8, 9));
        assertEquals(3, again.lastAddedId());
    }

    @Test
    void worldsAreSeparate() {
        ThrowStore store = store("a");
        store.add(new ThrowInput(1, 2, 3));
        store.load("b");
        assertTrue(store.throwsList().isEmpty());
        store.load("a");
        assertEquals(1, store.throwsList().size());
    }

    @Test
    void corruptFileBecomesBak() throws IOException {
        Files.writeString(dir.resolve("a.json"), "{oops");
        ThrowStore store = store("a");
        assertTrue(store.throwsList().isEmpty());
        assertTrue(Files.exists(dir.resolve("a.json.bak")));
    }

    @Test
    void emptyFileBecomesBak() throws IOException {
        Files.writeString(dir.resolve("a.json"), "");
        assertTrue(store("a").throwsList().isEmpty());
        assertTrue(Files.exists(dir.resolve("a.json.bak")));
    }

    private void assertRejected(String json) throws IOException {
        Files.writeString(dir.resolve("a.json"), json);
        ThrowStore store = store("a");
        assertTrue(store.throwsList().isEmpty(), json);
        assertTrue(Files.exists(dir.resolve("a.json.bak")), json);
        store.add(new ThrowInput(1, 2, 3));
        assertEquals(1, store.lastAddedId(), json);
    }

    @Test
    void fileWithNullEntryBecomesBak() throws IOException {
        assertRejected("{\"throwsList\":[null],\"over\":{},\"next\":2}");
    }

    @Test
    void fileWithMissingIdBecomesBak() throws IOException {
        assertRejected("{\"throwsList\":[{\"x\":1,\"z\":2,\"yaw\":3,\"on\":true}],\"over\":{},\"next\":2}");
    }

    @Test
    void fileWithNullOverrideBecomesBak() throws IOException {
        assertRejected("{\"throwsList\":[],\"over\":{\"1-2\":null},\"next\":1}");
    }

    @Test
    void fileWithNonFiniteValueBecomesBak() throws IOException {
        assertRejected("{\"throwsList\":[{\"id\":1,\"x\":NaN,\"z\":2,\"yaw\":3,\"on\":true}],\"over\":{},\"next\":2}");
    }

    @Test
    void fileWithRepeatedIdsBecomesBak() throws IOException {
        assertRejected("{\"throwsList\":[{\"id\":1,\"x\":1,\"z\":2,\"yaw\":3,\"on\":true},{\"id\":1,\"x\":4,\"z\":5,\"yaw\":6,\"on\":true}],\"over\":{},\"next\":2}");
    }

    @Test
    void fileWithNextBelowExistingIdsBecomesBak() throws IOException {
        assertRejected("{\"throwsList\":[{\"id\":5,\"x\":1,\"z\":2,\"yaw\":3,\"on\":true}],\"over\":{},\"next\":2}");
    }

    @Test
    void worldKeysAreSafeAndDistinct() {
        String[] raws = {"play.example.com:25565", "[::1]:25565", "мой мир", "play.example.com_25565"};
        for (int i = 0; i < raws.length; i++) {
            String key = WorldKey.of(raws[i]);
            assertTrue(key.chars().noneMatch(c -> "<>:\"/\\|?*".indexOf(c) >= 0), key);
            for (int j = i + 1; j < raws.length; j++) {
                assertNotEquals(key, WorldKey.of(raws[j]));
            }
        }
    }

    @Test
    void savesIntoNestedDirectory() throws IOException {
        Path nested = dir.resolve("config").resolve("stronghold_finder");
        ThrowStore store = new ThrowStore(nested);
        store.load(WorldKey.of("[::1]:25565"));
        store.add(new ThrowInput(1, 2, 3));
        try (var files = Files.list(nested)) {
            assertEquals(1, files.count());
        }
    }
}
