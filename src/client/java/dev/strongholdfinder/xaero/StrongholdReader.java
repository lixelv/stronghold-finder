package dev.strongholdfinder.xaero;

import dev.strongholdfinder.core.Compass;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.NumberText;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

final class StrongholdReader extends ElementReader<MapMark, MapFrame, StrongholdRenderer> {
    private static final int BOX = 8;

    @Override
    public boolean isHidden(MapMark mark, MapFrame frame) {
        return false;
    }

    @Override
    public double getRenderX(MapMark mark, MapFrame frame, float partialTicks) {
        return mark.x();
    }

    @Override
    public double getRenderZ(MapMark mark, MapFrame frame, float partialTicks) {
        return mark.z();
    }

    @Override
    public int getInteractionBoxLeft(MapMark mark, MapFrame frame, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getInteractionBoxRight(MapMark mark, MapFrame frame, float partialTicks) {
        return BOX;
    }

    @Override
    public int getInteractionBoxTop(MapMark mark, MapFrame frame, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getInteractionBoxBottom(MapMark mark, MapFrame frame, float partialTicks) {
        return BOX;
    }

    @Override
    public int getRenderBoxLeft(MapMark mark, MapFrame frame, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getRenderBoxRight(MapMark mark, MapFrame frame, float partialTicks) {
        return BOX;
    }

    @Override
    public int getRenderBoxTop(MapMark mark, MapFrame frame, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getRenderBoxBottom(MapMark mark, MapFrame frame, float partialTicks) {
        return BOX;
    }

    @Override
    public int getLeftSideLength(MapMark mark, Minecraft minecraft) {
        return 0;
    }

    @Override
    public String getMenuName(MapMark mark) {
        return "";
    }

    @Override
    public String getFilterName(MapMark mark) {
        return "";
    }

    @Override
    public int getMenuTextFillLeftPadding(MapMark mark) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(MapMark mark) {
        return 0;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }

    @Override
    public boolean isInteractable(ElementRenderLocation location, MapMark mark) {
        return !(mark instanceof MapMark.RayMark || mark instanceof MapMark.CircleMark);
    }

    @Override
    public float getBoxScale(ElementRenderLocation location, MapMark mark, MapFrame frame) {
        return frame.ui;
    }

    @Override
    public boolean isOnScreen(MapMark mark, double cameraX, double cameraZ, int width, int height, double scale, double screenSizeBasedScale, double dimDiv, MapFrame frame, float partialTicks) {
        return true;
    }

    @Override
    public ArrayList<RightClickOption> getRightClickOptions(MapMark mark, IRightClickableElement target) {
        return new ArrayList<>();
    }

    @Override
    public boolean isRightClickValid(MapMark mark) {
        return false;
    }

    @Override
    public Tooltip getTooltip(MapMark mark, MapFrame frame, boolean overMenu) {
        long x = Math.round(mark.x());
        long z = Math.round(mark.z());
        return switch (mark) {
            case MapMark.ThrowMark t -> {
                Compass side = Geometry.compass(t.source().yaw());
                yield new Tooltip(Component.translatable(
                        "map.stronghold_finder.throw",
                        t.source().id(),
                        NumberText.coordinate(t.source().x()),
                        NumberText.coordinate(t.source().z()),
                        NumberText.exact(t.source().yaw()),
                        Component.translatable("stronghold_finder." + side.key())));
            }
            case MapMark.PairMark p -> new Tooltip(Component.translatable(
                    "map.stronghold_finder.pair",
                    p.pair().a().id(), p.pair().b().id(), x, z, String.format(java.util.Locale.ROOT, "%.1f", p.pair().angle())));
            case MapMark.MeanMark m -> new Tooltip(Component.translatable("map.stronghold_finder.mean", x, z));
            default -> null;
        };
    }
}
