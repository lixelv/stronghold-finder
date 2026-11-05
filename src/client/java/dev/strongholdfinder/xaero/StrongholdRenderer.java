package dev.strongholdfinder.xaero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import xaero.lib.client.graphics.XaeroBufferProvider;
import xaero.map.element.MapElementGraphics;
import xaero.map.element.render.ElementRenderInfo;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

final class StrongholdRenderer extends ElementRenderer<MapMark, Object, StrongholdRenderer> {
    StrongholdRenderer() {
        super(new Object(), new StrongholdProvider(), new StrongholdReader());
    }

    @Override
    public void preRender(ElementRenderInfo info, XaeroBufferProvider buffers, MultiTextureRenderTypeRendererProvider textures, boolean shadow) {
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
        // Xaero translates the pose to the mark's position in pixels, so one block is info.scale units here.
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            graphics.fill(-6, -1, 6, 1, 0xFFFF0000);
            graphics.fill(-1, -6, 1, 6, 0xFFFF0000);
            int east = (int) Math.round(100 * info.scale);
            graphics.fill(0, -1, east, 1, 0xFF00FF00);
            pose.rotateDegrees(Axis.ZP, 45);
            graphics.fill(0, -1, east, 1, 0xFF00FFFF);
        } catch (Throwable error) {
            XaeroBridge.disable(error);
        } finally {
            pose.popPose();
        }
        return false;
    }

    @Override
    public boolean shouldRender(ElementRenderLocation location, boolean shadow) {
        return location == ElementRenderLocation.WORLD_MAP && !shadow && XaeroBridge.isActive();
    }

    @Override
    public int getOrder() {
        return 300;
    }
}
