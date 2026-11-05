package dev.strongholdfinder;

import dev.strongholdfinder.core.KeyEdge;
import dev.strongholdfinder.core.ThrowStore;
import dev.strongholdfinder.core.WorldKey;
import dev.strongholdfinder.screen.StrongholdScreen;
import dev.strongholdfinder.xaero.XaeroBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

public final class StrongholdFinderClient implements ClientModInitializer {
    private static final KeyEdge captureEdge = new KeyEdge();
    private static ThrowStore store;

    public static ThrowStore store() {
        return store;
    }

    @Override
    public void onInitializeClient() {
        store = new ThrowStore(FabricLoader.getInstance().getConfigDir().resolve("stronghold_finder"));
        ModKeys.register();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> store.load(worldKey(client)));
        ClientTickEvents.END_CLIENT_TICK.register(StrongholdFinderClient::tick);
    }

    private static void tick(Minecraft client) {
        XaeroBridge.install();
        if (client.player == null || client.level == null) {
            return;
        }
        boolean clicked = false;
        while (ModKeys.CAPTURE.consumeClick()) {
            clicked = true;
        }
        if (captureEdge.update(clicked, ModKeys.CAPTURE.isDown())) {
            if (client.hasShiftDown()) {
                ThrowCapture.fromEye(client, store);
            } else {
                ThrowCapture.fromLook(client, store);
            }
        }
        while (ModKeys.OPEN_SCREEN.consumeClick()) {
            client.gui.setScreen(new StrongholdScreen());
        }
    }

    private static String worldKey(Minecraft client) {
        if (client.getSingleplayerServer() != null) {
            String folder = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
            return WorldKey.of("singleplayer/" + folder);
        }
        if (client.getCurrentServer() != null) {
            return WorldKey.of("server/" + client.getCurrentServer().ip);
        }
        return WorldKey.of("unknown");
    }
}
