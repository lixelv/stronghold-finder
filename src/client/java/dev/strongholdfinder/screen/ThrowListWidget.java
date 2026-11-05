package dev.strongholdfinder.screen;

import dev.strongholdfinder.RayColors;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.NumberText;
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
    private static final int LEFT = 14;
    private static final int GAP = 4;
    private static final int BUTTONS = 34 + 2;
    /** {coordinate decimals, angle decimals}, tried in order until the row fits. The saved numbers keep every digit. */
    private static final int[][] PRECISIONS = {
        {NumberText.COORDINATE_DECIMALS, NumberText.ANGLE_DECIMALS},
        {NumberText.COORDINATE_DECIMALS, 4},
        {NumberText.COORDINATE_DECIMALS, 3},
        {NumberText.COORDINATE_DECIMALS, 2},
        {2, 2},
        {1, 1},
        {0, 1},
    };

    private final ThrowStore store;
    private final Runnable onChange;
    private Columns columns;
    private int columnsWidth = -1;

    /** Column positions after the id; the compass letters show only when there is room for them. */
    private record Columns(int coordinateDecimals, int angleDecimals, int zAt, int yawAt, boolean showSide) {
    }

    ThrowListWidget(Minecraft minecraft, ThrowStore store, Runnable onChange) {
        super(minecraft, 100, 100, 0, ROW_HEIGHT);
        this.store = store;
        this.onChange = onChange;
    }

    void refresh() {
        columnsWidth = -1;
        clearEntries();
        store.throwsList().forEach(t -> addEntry(new Row(t)));
    }

    private Columns columnsFor(Font font, int contentWidth) {
        if (contentWidth == columnsWidth) {
            return columns;
        }
        int available = contentWidth - LEFT - BUTTONS;
        List<Throw> rows = store.throwsList();
        int sideWidth = rows.stream()
                .mapToInt(t -> font.width(Component.translatable("stronghold_finder.short." + Geometry.compass(t.yaw()).key())))
                .max()
                .orElse(0);
        for (int[] precision : PRECISIONS) {
            int xWidth = rows.stream().mapToInt(t -> font.width(NumberText.fixed(t.x(), precision[0]))).max().orElse(0);
            int zWidth = rows.stream().mapToInt(t -> font.width(NumberText.fixed(t.z(), precision[0]))).max().orElse(0);
            int yawWidth = rows.stream().mapToInt(t -> font.width(NumberText.fixed(t.yaw(), precision[1]) + "°")).max().orElse(0);
            int zAt = xWidth + GAP;
            int yawAt = zAt + zWidth + GAP;
            int used = yawAt + yawWidth;
            columns = new Columns(precision[0], precision[1], zAt, yawAt, used + GAP + sideWidth <= available);
            if (used <= available) {
                break;
            }
        }
        columnsWidth = contentWidth;
        return columns;
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
            Columns at = columnsFor(font, getContentWidth());
            graphics.text(font, String.valueOf(source.id()), left + 2, top, RayColors.of(source));
            graphics.text(font, NumberText.fixed(source.x(), at.coordinateDecimals()), left + LEFT, top, color);
            graphics.text(font, NumberText.fixed(source.z(), at.coordinateDecimals()), left + LEFT + at.zAt(), top, color);
            graphics.text(font, NumberText.fixed(source.yaw(), at.angleDecimals()) + "°", left + LEFT + at.yawAt(), top, color);
            if (at.showSide()) {
                Component side = Component.translatable("stronghold_finder.short." + Geometry.compass(source.yaw()).key());
                graphics.text(font, side, right - BUTTONS - font.width(side), top, 0xFFAAAAAA);
            }
            toggle.setPosition(right - 34, getContentY());
            delete.setPosition(right - 16, getContentY());
            toggle.extractRenderState(graphics, mouseX, mouseY, partialTicks);
            delete.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }
}
