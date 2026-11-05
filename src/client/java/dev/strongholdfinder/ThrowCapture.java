package dev.strongholdfinder;

import dev.strongholdfinder.core.AddResult;
import dev.strongholdfinder.core.Aim;
import dev.strongholdfinder.core.EyeAim;
import dev.strongholdfinder.core.EyeCandidate;
import dev.strongholdfinder.core.Solution;
import dev.strongholdfinder.core.ThrowInput;
import dev.strongholdfinder.core.ThrowStore;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class ThrowCapture {
    private static final double EYE_SEARCH_RADIUS = 64;

    private ThrowCapture() {
    }

    static void fromLook(Minecraft client, ThrowStore store) {
        LocalPlayer player = client.player;
        if (isInOverworld(client)) {
            record(player, store, player.getYRot());
        }
    }

    static void fromEye(Minecraft client, ThrowStore store) {
        LocalPlayer player = client.player;
        if (!isInOverworld(client)) {
            return;
        }
        MinecraftServer server = client.getSingleplayerServer();
        if (!EyeAim.isAllowed(server != null, server != null && server.isPublished())) {
            player.sendOverlayMessage(Component.translatable("message.stronghold_finder.aim_singleplayer_only"));
            return;
        }
        List<EyeCandidate> eyes = client.level
                .getEntitiesOfClass(EyeOfEnder.class, player.getBoundingBox().inflate(EYE_SEARCH_RADIUS))
                .stream()
                .map(eye -> {
                    Vec3 center = eye.getBoundingBox().getCenter();
                    return new EyeCandidate(center.x, center.y, center.z, eye.tickCount);
                })
                .toList();
        EyeCandidate eye = EyeAim.pick(eyes);
        if (eye == null) {
            player.sendOverlayMessage(Component.translatable("message.stronghold_finder.no_eye"));
            return;
        }
        Vec3 from = player.getEyePosition();
        Aim aim = EyeAim.compute(from.x, from.y, from.z, eye.x(), eye.y(), eye.z());
        if (aim == null) {
            player.sendOverlayMessage(Component.translatable("message.stronghold_finder.eye_too_close"));
            return;
        }
        float yaw = (float) aim.yaw();
        float pitch = (float) aim.pitch();
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        record(player, store, aim.yaw());
    }

    private static boolean isInOverworld(Minecraft client) {
        if (client.level.dimension() == Level.OVERWORLD) {
            return true;
        }
        client.player.sendOverlayMessage(Component.translatable("message.stronghold_finder.overworld_only"));
        return false;
    }

    private static void record(LocalPlayer player, ThrowStore store, double yaw) {
        if (store.add(new ThrowInput(player.getX(), player.getZ(), yaw)) == AddResult.DUPLICATE) {
            player.sendOverlayMessage(Component.translatable("message.stronghold_finder.duplicate"));
            return;
        }
        Solution solution = store.solution();
        if (solution.mean() == null) {
            player.sendOverlayMessage(Component.translatable("message.stronghold_finder.added", store.lastAddedId()));
            return;
        }
        player.sendOverlayMessage(Component.translatable(
                "message.stronghold_finder.added_result",
                store.lastAddedId(),
                Math.round(solution.mean().x()),
                Math.round(solution.mean().z()),
                Math.round(solution.err())));
    }
}
