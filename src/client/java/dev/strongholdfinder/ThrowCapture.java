package dev.strongholdfinder;

import dev.strongholdfinder.core.AddResult;
import dev.strongholdfinder.core.AngleRay;
import dev.strongholdfinder.core.EyeRayCapture;
import dev.strongholdfinder.core.EyeSample;
import dev.strongholdfinder.core.Point;
import dev.strongholdfinder.core.Ray;
import dev.strongholdfinder.core.Solution;
import dev.strongholdfinder.core.ThrowStore;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;

final class ThrowCapture {
    private static final double EYE_SEARCH_RADIUS = 64;

    private static EyeRayCapture eyeCapture;
    private static int eyeId;

    private ThrowCapture() {
    }

    static void fromLook(Minecraft client, ThrowStore store) {
        LocalPlayer player = client.player;
        if (isInOverworld(client)) {
            record(player, store, new AngleRay(new Point(player.getX(), player.getZ()), player.getYRot()));
        }
    }

    /** Starts watching the newest eye. The ray comes on a later tick, from {@link #tick}. */
    static void fromEye(Minecraft client, ThrowStore store) {
        if (eyeCapture != null || !isInOverworld(client)) {
            return;
        }
        EyeOfEnder eye = client.level
                .getEntitiesOfClass(EyeOfEnder.class, client.player.getBoundingBox().inflate(EYE_SEARCH_RADIUS))
                .stream()
                .min(Comparator.comparingInt(found -> found.tickCount))
                .orElse(null);
        eyeCapture = new EyeRayCapture();
        eyeId = eye == null ? -1 : eye.getId();
        tick(client, store);
        if (eyeCapture != null) {
            client.player.sendOverlayMessage(Component.translatable("message.stronghold_finder.reading_eye"));
        }
    }

    static void tick(Minecraft client, ThrowStore store) {
        if (eyeCapture == null) {
            return;
        }
        EyeSample sample = client.level.getEntity(eyeId) instanceof EyeOfEnder eye && !eye.isRemoved()
                ? new EyeSample(eye.getX(), eye.getZ(), eye.tickCount)
                : null;
        EyeRayCapture.Status status = eyeCapture.accept(sample);
        if (status == EyeRayCapture.Status.WAITING) {
            return;
        }
        Ray ray = eyeCapture.ray();
        eyeCapture = null;
        if (status == EyeRayCapture.Status.DONE) {
            record(client.player, store, ray);
        } else {
            client.player.sendOverlayMessage(Component.translatable("message.stronghold_finder.no_eye"));
        }
    }

    static void cancel() {
        eyeCapture = null;
    }

    private static boolean isInOverworld(Minecraft client) {
        if (client.level.dimension() == Level.OVERWORLD) {
            return true;
        }
        client.player.sendOverlayMessage(Component.translatable("message.stronghold_finder.overworld_only"));
        return false;
    }

    private static void record(LocalPlayer player, ThrowStore store, Ray ray) {
        if (store.add(ray) == AddResult.DUPLICATE) {
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
