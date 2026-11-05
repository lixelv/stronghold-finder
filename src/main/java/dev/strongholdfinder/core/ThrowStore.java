package dev.strongholdfinder.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThrowStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("stronghold_finder");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int UNDO_LIMIT = 30;

    private final Path dir;
    private final Deque<State> undo = new ArrayDeque<>();
    private State state = new State();
    private String key;
    private int lastAddedId;
    private Solution solution;

    /** Plain fields so Gson can read and write this as the save file. */
    private static final class State {
        List<Throw> throwsList = new ArrayList<>();
        Map<String, Boolean> over = new HashMap<>();
        int next = 1;
        boolean overlayVisible = true;

        State copy() {
            State copy = new State();
            copy.throwsList = new ArrayList<>(throwsList);
            copy.over = new HashMap<>(over);
            copy.next = next;
            copy.overlayVisible = overlayVisible;
            return copy;
        }

        boolean isValid() {
            return throwsList != null && over != null && next > 0;
        }
    }

    public ThrowStore(Path dir) {
        this.dir = dir;
    }

    public void load(String key) {
        this.key = key;
        undo.clear();
        state = read(dir.resolve(key + ".json"));
        lastAddedId = 0;
        solution = null;
    }

    private static State read(Path file) {
        if (!Files.exists(file)) {
            return new State();
        }
        try {
            State loaded = GSON.fromJson(Files.readString(file), State.class);
            if (loaded != null && loaded.isValid()) {
                return loaded;
            }
        } catch (IOException | RuntimeException error) {
            LOGGER.warn("Could not read {}", file, error);
        }
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            LOGGER.warn("Could not back up {}", file, error);
        }
        return new State();
    }

    private void save() {
        solution = null;
        if (key == null) {
            return;
        }
        Path file = dir.resolve(key + ".json");
        try {
            Files.createDirectories(dir);
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(state));
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            LOGGER.warn("Could not save {}", file, error);
        }
    }

    private void remember() {
        undo.push(state.copy());
        while (undo.size() > UNDO_LIMIT) {
            undo.removeLast();
        }
    }

    private boolean isDuplicate(double x, double z, double yaw) {
        return state.throwsList.stream().anyMatch(t ->
                Math.abs(t.x() - x) < 0.01 && Math.abs(t.z() - z) < 0.01 && Math.abs(t.yaw() - yaw) < 0.01);
    }

    private void append(ThrowInput input, double yaw) {
        lastAddedId = state.next++;
        state.throwsList.add(new Throw(lastAddedId, input.x(), input.z(), yaw, true));
    }

    public AddResult add(ThrowInput input) {
        double yaw = Geometry.normYaw(input.yaw());
        if (isDuplicate(input.x(), input.z(), yaw)) {
            return AddResult.DUPLICATE;
        }
        remember();
        append(input, yaw);
        save();
        return AddResult.ADDED;
    }

    public int addAll(List<ThrowInput> inputs) {
        State before = state.copy();
        int added = 0;
        for (ThrowInput input : inputs) {
            double yaw = Geometry.normYaw(input.yaw());
            if (!isDuplicate(input.x(), input.z(), yaw)) {
                append(input, yaw);
                added++;
            }
        }
        if (added > 0) {
            undo.push(before);
            save();
        }
        return added;
    }

    public void remove(int id) {
        remember();
        state.throwsList.removeIf(t -> t.id() == id);
        state.over.keySet().removeIf(pair -> List.of(pair.split("-")).contains(String.valueOf(id)));
        save();
    }

    public void setOn(int id, boolean on) {
        state.throwsList.replaceAll(t -> t.id() == id ? new Throw(t.id(), t.x(), t.z(), t.yaw(), on) : t);
        save();
    }

    public void setPair(String pair, boolean used) {
        state.over.put(pair, used);
        save();
    }

    public void clear() {
        remember();
        state.throwsList.clear();
        state.over.clear();
        state.next = 1;
        lastAddedId = 0;
        save();
    }

    public boolean undo() {
        State previous = undo.poll();
        if (previous == null) {
            return false;
        }
        state = previous;
        save();
        return true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public List<Throw> throwsList() {
        return List.copyOf(state.throwsList);
    }

    public Solution solution() {
        if (solution == null) {
            solution = Solver.solve(state.throwsList, state.over);
        }
        return solution;
    }

    public boolean overlayVisible() {
        return state.overlayVisible;
    }

    public void setOverlayVisible(boolean visible) {
        state.overlayVisible = visible;
        save();
    }

    public int lastAddedId() {
        return lastAddedId;
    }
}
