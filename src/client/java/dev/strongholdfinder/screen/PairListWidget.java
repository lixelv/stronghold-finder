package dev.strongholdfinder.screen;

import dev.strongholdfinder.core.Pair;
import dev.strongholdfinder.core.PairStatus;
import dev.strongholdfinder.core.ThrowStore;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

final class PairListWidget extends ContainerObjectSelectionList<PairListWidget.Row> {
    private final ThrowStore store;
    private final Runnable onChange;

    PairListWidget(Minecraft minecraft, ThrowStore store, Runnable onChange) {
        super(minecraft, 100, 100, 0, ThrowListWidget.ROW_HEIGHT);
        this.store = store;
        this.onChange = onChange;
    }

    void refresh() {
        clearEntries();
        store.solution().pairs().forEach(p -> addEntry(new Row(p)));
    }

    @Override
    public int getRowWidth() {
        return width - 10;
    }

    @Override
    protected int scrollBarX() {
        return getRight() - 6;
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {
        graphics.fill(getX(), getY(), getRight(), getBottom(), 0x60000000);
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {
    }

    private static String statusKey(Pair pair) {
        if (pair.status() == PairStatus.OK || pair.status() == PairStatus.WEAK) {
            String suffix = pair.status() == PairStatus.WEAK ? "weak" : pair.used() ? "used" : "off";
            return "screen.stronghold_finder.pair." + suffix;
        }
        return "screen.stronghold_finder.pair." + pair.status().name().toLowerCase(java.util.Locale.ROOT);
    }

    final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final Pair pair;
        private final Button toggle;

        Row(Pair pair) {
            this.pair = pair;
            toggle = Button.builder(Component.literal(pair.used() ? "✔" : "–"), b -> {
                store.setPair(pair.key(), !pair.used());
                onChange.run();
            }).size(16, 16).build();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return pair.usable() ? List.of(toggle) : List.of();
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return pair.usable() ? List.of(toggle) : List.of();
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            Font font = minecraft.font;
            int left = getContentX();
            int top = getContentYMiddle() - 4;
            int color = pair.used() ? 0xFFFFFFFF : 0xFFAAAAAA;
            int right = left + getContentWidth();
            graphics.text(font, pair.a().id() + "×" + pair.b().id(), left + 2, top, color);
            int angleX = left + (pair.usable() ? 102 : 28);
            if (pair.usable()) {
                graphics.text(font, String.valueOf(Math.round(pair.x())), left + 26, top, color);
                graphics.text(font, String.valueOf(Math.round(pair.z())), left + 64, top, color);
            }
            graphics.text(font, String.format(java.util.Locale.ROOT, "%.0f°", pair.angle()), angleX, top, color);
            int statusX = angleX + 24;
            String status = Component.translatable(statusKey(pair)).getString();
            graphics.text(font, font.plainSubstrByWidth(status, right - 18 - statusX), statusX, top, 0xFFAAAAAA);
            if (pair.usable()) {
                toggle.setPosition(left + getContentWidth() - 16, getContentY());
                toggle.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            }
        }
    }
}
