package dev.strongholdfinder;

import dev.strongholdfinder.xaero.XaeroBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class StrongholdFinderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> XaeroBridge.install());
    }
}
