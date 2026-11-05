package dev.strongholdfinder.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strongholdfinder.StrongholdFinderClient;
import dev.strongholdfinder.core.AddResult;
import dev.strongholdfinder.core.EntryParser;
import dev.strongholdfinder.core.EntryResult;
import dev.strongholdfinder.core.FieldEntry;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.ParsedPaste;
import dev.strongholdfinder.core.Solution;
import dev.strongholdfinder.core.ThrowInput;
import dev.strongholdfinder.core.ThrowStore;
import dev.strongholdfinder.xaero.XaeroBridge;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class StrongholdScreen extends Screen {
    private static final int MARGIN = 8;
    private static final int FIELD_HEIGHT = 18;
    private static final int STATUS_LINE = 10;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int RED = 0xFFFF5555;

    private final ThrowStore store = StrongholdFinderClient.store();
    private final EditBox[] fields = new EditBox[3];
    private final String[] values = {"", "", ""};
    private ThrowListWidget throwList;
    private PairListWidget pairList;
    private Button undoButton;
    private Button clearButton;
    private Button copyButton;
    private Button overlayButton;
    private Component notice;
    private boolean noticeIsError;
    private boolean applying;

    private int leftX;
    private int leftWidth;
    private int rightX;
    private int rightWidth;
    private int statusY;
    private int throwsHeaderY;
    private int pairsHeaderY;

    public StrongholdScreen() {
        super(Component.translatable("screen.stronghold_finder.title"));
    }

    @Override
    protected void init() {
        leftX = MARGIN;
        leftWidth = (width - MARGIN * 3) / 2;
        rightX = leftX + leftWidth + MARGIN;
        rightWidth = width - rightX - MARGIN;

        int addWidth = 54;
        int fieldWidth = (leftWidth - addWidth - 3 * 4) / 3;
        int fieldY = 30;
        String[] names = {"x", "z", "yaw"};
        for (int i = 0; i < 3; i++) {
            int index = i;
            EditBox box = new EditBox(font, leftX + i * (fieldWidth + 4), fieldY, fieldWidth, FIELD_HEIGHT,
                    Component.translatable("screen.stronghold_finder.field." + names[i]));
            box.setMaxLength(128);
            box.setValue(values[i]);
            box.setResponder(text -> onFieldChanged(index, text));
            fields[i] = addRenderableWidget(box);
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.stronghold_finder.add"), b -> commit(values.clone()))
                .bounds(leftX + 3 * (fieldWidth + 4), fieldY - 1, addWidth, 20).build());
        statusY = fieldY + FIELD_HEIGHT + 4;

        int buttonY = height - 24;
        int count = 5;
        int buttonWidth = (width - MARGIN * 2 - (count - 1) * 4) / count;
        copyButton = button(0, buttonY, buttonWidth, Component.translatable("screen.stronghold_finder.copy"), this::copyResult);
        undoButton = button(1, buttonY, buttonWidth, Component.translatable("screen.stronghold_finder.undo"), () -> {
            if (store.undo()) {
                refresh();
            }
        });
        clearButton = button(2, buttonY, buttonWidth, Component.translatable("screen.stronghold_finder.clear"), () -> {
            store.clear();
            refresh();
        });
        overlayButton = button(3, buttonY, buttonWidth, Component.empty(), () -> {
            store.setOverlayVisible(!store.overlayVisible());
            refresh();
        });
        button(4, buttonY, buttonWidth, Component.translatable("gui.done"), this::onClose);

        int top = 30;
        int listsHeight = buttonY - 6 - top;
        int gapForHeader = 14;
        int throwsHeight = (listsHeight - gapForHeader) / 2;
        int pairsHeight = listsHeight - gapForHeader - throwsHeight;
        throwsHeaderY = top - 11;
        pairsHeaderY = top + throwsHeight + 3;
        throwList = addRenderableWidget(new ThrowListWidget(minecraft, store, this::refresh));
        throwList.updateSizeAndPosition(rightWidth, throwsHeight, rightX, top);
        pairList = addRenderableWidget(new PairListWidget(minecraft, store, this::refresh));
        pairList.updateSizeAndPosition(rightWidth, pairsHeight, rightX, top + throwsHeight + gapForHeader);

        refresh();
        setInitialFocus(fields[0]);
    }

    private Button button(int slot, int y, int width, Component label, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run())
                .bounds(MARGIN + slot * (width + 4), y, width, 20).build());
    }

    private void refresh() {
        throwList.refresh();
        pairList.refresh();
        undoButton.active = store.canUndo();
        clearButton.active = !store.throwsList().isEmpty();
        copyButton.active = store.solution().mean() != null;
        overlayButton.setMessage(Component.translatable(
                store.overlayVisible() ? "screen.stronghold_finder.overlay_on" : "screen.stronghold_finder.overlay_off"));
    }

    private void onFieldChanged(int index, String text) {
        if (applying) {
            return;
        }
        apply(FieldEntry.enterText(values, index, text), index);
    }

    private void apply(EntryResult result, int index) {
        applying = true;
        for (int i = 0; i < 3; i++) {
            values[i] = result.fields()[i];
            if (!fields[i].getValue().equals(values[i])) {
                fields[i].setValue(values[i]);
                fields[i].moveCursorToEnd(false);
            }
        }
        applying = false;
        if (result.commit()) {
            commit(values.clone());
            return;
        }
        if (result.invalid() >= 0) {
            say(Component.translatable("screen.stronghold_finder.enter_number", fieldName(result.invalid())), true);
        } else if (noticeIsError) {
            notice = null;
            noticeIsError = false;
        }
        focusField(result.focus());
    }

    private void focusField(int index) {
        if (getFocused() != fields[index]) {
            setFocused(fields[index]);
        }
    }

    private Component fieldName(int index) {
        String[] names = {"x", "z", "yaw"};
        return Component.translatable("screen.stronghold_finder.field." + names[index]);
    }

    private void commit(String[] current) {
        ThrowInput input = FieldEntry.read(current);
        if (input == null) {
            int[] bad = FieldEntry.invalid(current);
            Object[] names = Arrays.stream(bad).mapToObj(i -> fieldName(i).getString()).toArray();
            say(Component.translatable("screen.stronghold_finder.enter_number", String.join(", ", Arrays.copyOf(names, names.length, String[].class))), true);
            focusField(bad[0]);
            return;
        }
        if (store.add(input) == AddResult.DUPLICATE) {
            say(Component.translatable("message.stronghold_finder.duplicate"), true);
            focusField(2);
            return;
        }
        say(Component.translatable("message.stronghold_finder.added", store.lastAddedId()), false);
        clearFields();
        refresh();
    }

    private void clearFields() {
        applying = true;
        for (int i = 0; i < 3; i++) {
            values[i] = "";
            fields[i].setValue("");
        }
        applying = false;
        focusField(0);
    }

    private void say(Component text, boolean error) {
        notice = text;
        noticeIsError = error;
    }

    private void copyResult() {
        Solution s = store.solution();
        if (s.mean() != null) {
            minecraft.keyboardHandler.setClipboard(Math.round(s.mean().x()) + " ~ " + Math.round(s.mean().z()));
            say(Component.translatable("screen.stronghold_finder.copied"), false);
        }
    }

    private int focusedFieldIndex() {
        for (int i = 0; i < 3; i++) {
            if (getFocused() == fields[i]) {
                return i;
            }
        }
        return -1;
    }

    /** Returns true when the text was handled; a single number is left to the field's normal paste. */
    private boolean paste(int index, String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty() || (!trimmed.matches("(?s).*\\s.*") && index >= 0)) {
            return false;
        }
        switch (EntryParser.parsePaste(text)) {
            case ParsedPaste.Throws found -> {
                int added = store.addAll(found.throwsList());
                if (added == 0) {
                    say(Component.translatable("message.stronghold_finder.duplicate"), true);
                } else {
                    say(Component.translatable("screen.stronghold_finder.added_count", added), false);
                    clearFields();
                    refresh();
                }
            }
            case ParsedPaste.WrongDimension wrong ->
                    say(Component.translatable("message.stronghold_finder.overworld_only"), true);
            case ParsedPaste.Invalid invalid -> {
                boolean numbers = Arrays.stream(trimmed.split("\\s+")).allMatch(t -> EntryParser.num(t) != null);
                if (numbers && index >= 0) {
                    apply(FieldEntry.enterText(values, index, trimmed + " "), index);
                } else {
                    say(Component.translatable("screen.stronghold_finder.bad_paste"), true);
                }
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int index = focusedFieldIndex();
        // The physical V key also counts, because the virtual key code changes with the keyboard layout.
        boolean pasting = event.isPaste() || (event.key() == InputConstants.KEY_V && minecraft.hasControlDown());
        if (pasting && paste(index, minecraft.keyboardHandler.getClipboard())) {
            return true;
        }
        if (event.isConfirmation() && index >= 0) {
            apply(FieldEntry.enterText(values, index, values[index] + "\n"), index);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(leftX - 4, 18, leftX + leftWidth + 4, height - 28, 0x60000000);
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.text(font, title, leftX, 6, WHITE);
        for (int i = 0; i < 3; i++) {
            graphics.text(font, fieldName(i), fields[i].getX(), fields[i].getY() - 10, GRAY);
        }

        int at = statusY;
        Component warning = XaeroBridge.isActive() ? null : Component.translatable("screen.stronghold_finder.map_failed");
        Component status = statusText();
        for (Component line : warning == null ? List.of(status) : List.of(warning, status)) {
            int color = line == warning || noticeIsError ? RED : GRAY;
            for (FormattedCharSequence wrapped : font.split(line, leftWidth)) {
                graphics.text(font, wrapped, leftX, at, color);
                at += STATUS_LINE;
            }
        }
        ResultPanel.extract(graphics, font, store.solution(), leftX, Math.max(at + 4, statusY + STATUS_LINE * 2 + 4), leftWidth, height - 28);

        Solution solution = store.solution();
        graphics.text(font, Component.translatable("screen.stronghold_finder.throws", store.throwsList().size()), rightX, throwsHeaderY, WHITE);
        graphics.text(font, Component.translatable("screen.stronghold_finder.pairs", solution.used().size(), solution.pairs().size()), rightX, pairsHeaderY, WHITE);
    }

    private Component statusText() {
        if (noticeIsError) {
            return notice;
        }
        Double yaw = EntryParser.num(values[2]);
        if (yaw != null) {
            return Component.translatable("screen.stronghold_finder.yaw_preview",
                    Component.translatable("stronghold_finder." + Geometry.compass(yaw).key()));
        }
        if (notice != null && Arrays.stream(values).allMatch(String::isEmpty)) {
            return notice;
        }
        return Component.translatable("screen.stronghold_finder.hint");
    }
}
