package dev.strongholdfinder.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.strongholdfinder.RayColors;
import dev.strongholdfinder.StrongholdFinderClient;
import dev.strongholdfinder.core.LabelPlacer;
import dev.strongholdfinder.core.Point;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.Throw;
import dev.strongholdfinder.core.ThrowStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.level.Level;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

/**
 * Xaero translates the pose to each mark's position, rounded to a whole pixel, and one block is
 * {@code info.scale} pose units. Marks scale themselves by the GUI scale to match the game's interface.
 */
final class StrongholdRenderer extends ElementRenderer<MapMark, MapFrame, StrongholdRenderer> {
    private static final int MUTED = RayColors.MUTED;
    private static final int PANEL = 0xE0101010;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MEAN_COLOR = 0xFFFFFF55;
    private static final int CIRCLE_FILL = 0x30FFFF55;
    private static final int CIRCLE_EDGE = 0xC0FFFF55;
    private static final int DASH = 6;
    private static final int DASH_GAP = 4;

    StrongholdRenderer() {
        super(new MapFrame(), new StrongholdProvider(), new StrongholdReader());
    }

    @Override
    public void preRender(ElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider textures, boolean shadow) {
        MapFrame frame = getContext();
        frame.clear();
        try {
            ThrowStore store = StrongholdFinderClient.store();
            if (store.overlayVisible() && Level.OVERWORLD.equals(info.mapDimension)) {
                Minecraft client = Minecraft.getInstance();
                frame.build(store, client.font, client.getWindow().getWidth(), client.getWindow().getHeight(),
                        info.renderPos.x, info.renderPos.z, info.scale, (float) client.getWindow().getGuiScale());
            }
        } catch (Throwable error) {
            frame.clear();
            XaeroBridge.disable(error);
        }
    }

    @Override
    public void postRender(ElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider textures, boolean shadow) {
        buffers.endBatch();
    }

    @Override
    public void renderElementShadow(MapMark mark, boolean hovered, float scale, double partialX, double partialY, ElementRenderInfo info, MapElementGraphics graphics, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider textures) {
    }

    @Override
    public boolean renderElement(MapMark mark, boolean hovered, double depth, float optionalScale, double partialX, double partialY, ElementRenderInfo info, MapElementGraphics graphics, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider textures) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            MapFrame frame = getContext();
            Font font = Minecraft.getInstance().font;
            pose.translate((float) partialX, (float) partialY, 0f);
            switch (mark) {
                case MapMark.RayMark ray -> drawRay(ray.source(), frame, pose, graphics);
                case MapMark.ErrorCircleMark circle -> drawCircle(circle.radius() * frame.scale, frame, graphics);
                case MapMark.PairMark pair -> {
                    drawLabel(frame.pairLabels.get(pair.pair().key()), MapFrame.pairText(pair.pair()), mark, frame, pose, graphics, font);
                    pose.scale(frame.ui, frame.ui, 1f);
                    drawDot(pair.pair().used(), hovered, graphics);
                }
                case MapMark.ThrowMark throwMark -> {
                    pose.scale(frame.ui, frame.ui, 1f);
                    drawThrow(throwMark.source(), graphics, font);
                }
                case MapMark.MeanMark mean -> drawMean(mean.point(), frame, pose, graphics, font);
            }
        } catch (Throwable error) {
            XaeroBridge.disable(error);
        } finally {
            pose.popPose();
        }
        return false;
    }

    private static void drawRay(Throw source, MapFrame frame, PoseStack pose, MapElementGraphics graphics) {
        Point direction = Geometry.dir(source.yaw());
        pose.rotateDegrees(Axis.ZP, (float) Math.toDegrees(Math.atan2(direction.z(), direction.x())));
        pose.scale(frame.ui, frame.ui, 1f);
        double fromCenter = Math.hypot(frame.px(source.x()), frame.py(source.z()));
        int start = (int) (Math.max(0, fromCenter - frame.diagonal) / frame.ui);
        int end = (int) ((fromCenter + frame.diagonal) / frame.ui);
        int color = RayColors.of(source);
        if (RayColors.hasOwnColor(source)) {
            graphics.fill(start, 0, end, 1, color);
            return;
        }
        int period = DASH + DASH_GAP;
        for (int x = start - start % period; x < end; x += period) {
            graphics.fill(x, 0, x + DASH, 1, color);
        }
    }

    private static void drawCircle(double radius, MapFrame frame, MapElementGraphics graphics) {
        if (radius < 2 || radius > frame.diagonal * 2) {
            return;
        }
        int r = (int) Math.round(radius);
        for (int y = -r; y < r; y++) {
            int half = (int) Math.round(Math.sqrt(radius * radius - (y + 0.5) * (y + 0.5)));
            graphics.fill(-half, y, half, y + 1, CIRCLE_FILL);
            graphics.fill(-half - 1, y, -half, y + 1, CIRCLE_EDGE);
            graphics.fill(half, y, half + 1, y + 1, CIRCLE_EDGE);
        }
    }

    private static void drawDot(boolean used, boolean hovered, MapElementGraphics graphics) {
        int half = hovered ? 3 : 2;
        graphics.fill(-half - 1, -half - 1, half + 1, half + 1, 0xFF000000);
        graphics.fill(-half, -half, half, half, used ? WHITE : 0xFF000000);
        if (!used) {
            graphics.fill(-half, -half, half, -half + 1, MUTED);
            graphics.fill(-half, half - 1, half, half, MUTED);
            graphics.fill(-half, -half, -half + 1, half, MUTED);
            graphics.fill(half - 1, -half, half, half, MUTED);
        }
    }

    private static void drawThrow(Throw source, MapElementGraphics graphics, Font font) {
        graphics.fill(-7, -7, 7, 7, RayColors.of(source));
        graphics.fill(-6, -6, 6, 6, PANEL | 0xFF000000);
        String number = String.valueOf(source.id());
        graphics.drawString(font, number, -font.width(number) / 2, -4, WHITE, false);
    }

    private static void drawMean(Point point, MapFrame frame, PoseStack pose, MapElementGraphics graphics, Font font) {
        String text = "X " + Math.round(point.x()) + ", Z " + Math.round(point.z());
        LabelPlacer.Box box = frame.meanLabel;
        if (box != null) {
            pose.pushPose();
            pose.translate((float) (box.left() - frame.px(point.x())), (float) (box.top() - frame.py(point.z())), 0f);
            pose.scale(frame.ui, frame.ui, 1f);
            int width = font.width(text) + MapFrame.MEAN_PADDING * 2;
            int height = MapFrame.LABEL_HEIGHT + MapFrame.MEAN_PADDING * 2;
            graphics.fill(0, 0, width, height, PANEL);
            graphics.drawString(font, text, MapFrame.MEAN_PADDING, MapFrame.MEAN_PADDING, MEAN_COLOR, false);
            pose.popPose();
        }
        pose.scale(frame.ui, frame.ui, 1f);
        pose.rotateDegrees(Axis.ZP, 45);
        graphics.fill(-6, -6, 6, 6, 0xFF000000);
        graphics.fill(-5, -5, 5, 5, MEAN_COLOR);
    }

    private static void drawLabel(LabelPlacer.Box box, String text, MapMark mark, MapFrame frame, PoseStack pose, MapElementGraphics graphics, Font font) {
        if (box == null) {
            return;
        }
        pose.pushPose();
        pose.translate((float) (box.left() - frame.px(mark.x())), (float) (box.top() - frame.py(mark.z())), 0f);
        pose.scale(frame.ui, frame.ui, 1f);
        graphics.fill(-1, -1, font.width(text) + 1, MapFrame.LABEL_HEIGHT, PANEL);
        graphics.drawString(font, text, 0, 0, WHITE, false);
        pose.popPose();
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
        return location == ElementRenderLocation.WORLD_MAP && !shadow && XaeroBridge.isActive();
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }

    @Override
    public int getOrder() {
        return 300;
    }
}
