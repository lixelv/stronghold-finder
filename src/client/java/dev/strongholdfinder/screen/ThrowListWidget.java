package dev.strongholdfinder.screen;

import dev.strongholdfinder.RayColors;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.Throw;
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

final class ThrowListWidget extends ContainerObjectSelectionList<ThrowListWidget.Row> {
    static final int ROW_HEIGHT = 18;

    private final ThrowStore store;
    private final Runnable onChange;

    ThrowListWidget(Minecraft minecraft, ThrowStore store, Runnable onChange) {
        super(minecraft, 100, 100, 0, ROW_HEIGHT);
        this.store = store;
        this.onChange = onChange;
    }

    void refresh() {
        clearEntries();
        store.throwsList().forEach(t -> addEntry(new Row(t)));
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

    final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final Throw source;
        private final Button toggle;
        private final Button delete;

        Row(Throw source) {
            this.source = source;
            toggle = Button.builder(Component.literal(source.on() ? "✔" : "–"), b -> {
                store.setOn(source.id(), !source.on());
                onChange.run();
            }).size(16, 16).build();
            delete = Button.builder(Component.literal("✕"), b -> {
                store.remove(source.id());
                onChange.run();
            }).size(16, 16).build();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(toggle, delete);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(toggle, delete);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            Font font = minecraft.font;
            int left = getContentX();
            int top = getContentYMiddle() - 4;
            int color = source.on() ? 0xFFFFFFFF : RayColors.MUTED;
            int right = left + getContentWidth();
            int unit = (getContentWidth() - 66) / 3;
            graphics.text(font, String.valueOf(source.id()), left + 2, top, RayColors.of(source));
            graphics.text(font, String.valueOf(Math.round(source.x())), left + 14, top, color);
            graphics.text(font, String.valueOf(Math.round(source.z())), left + 14 + unit, top, color);
            graphics.text(font, String.format(java.util.Locale.ROOT, "%.1f°", source.yaw()), left + 14 + unit * 2, top, color);
            Component side = Component.translatable("stronghold_finder.short." + Geometry.compass(source.yaw()).key());
            graphics.text(font, side, right - 34 - 2 - font.width(side), top, 0xFFAAAAAA);
            toggle.setPosition(right - 34, getContentY());
            delete.setPosition(right - 16, getContentY());
            toggle.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            delete.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }
}
