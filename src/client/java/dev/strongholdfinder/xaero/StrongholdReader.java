package dev.strongholdfinder.xaero;

import net.minecraft.client.Minecraft;
import xaero.map.element.render.ElementReader;
import xaero.map.element.render.ElementRenderLocation;

final class StrongholdReader extends ElementReader<MapMark, Object, StrongholdRenderer> {
    private static final int BOX = 8;

    @Override
    public boolean isHidden(MapMark mark, Object context) {
        return false;
    }

    @Override
    public double getRenderX(MapMark mark, Object context, float partialTicks) {
        return mark.x();
    }

    @Override
    public double getRenderZ(MapMark mark, Object context, float partialTicks) {
        return mark.z();
    }

    @Override
    public int getInteractionBoxLeft(MapMark mark, Object context, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getInteractionBoxRight(MapMark mark, Object context, float partialTicks) {
        return BOX;
    }

    @Override
    public int getInteractionBoxTop(MapMark mark, Object context, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getInteractionBoxBottom(MapMark mark, Object context, float partialTicks) {
        return BOX;
    }

    @Override
    public int getRenderBoxLeft(MapMark mark, Object context, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getRenderBoxRight(MapMark mark, Object context, float partialTicks) {
        return BOX;
    }

    @Override
    public int getRenderBoxTop(MapMark mark, Object context, float partialTicks) {
        return -BOX;
    }

    @Override
    public int getRenderBoxBottom(MapMark mark, Object context, float partialTicks) {
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
        return false;
    }

    @Override
    public boolean isOnScreen(MapMark mark, double cameraX, double cameraZ, int width, int height, double scale, double screenSizeBasedScale, double dimDiv, Object context, float partialTicks) {
        return true;
    }

    @Override
    public boolean isRightClickValid(MapMark mark) {
        return false;
    }
}
