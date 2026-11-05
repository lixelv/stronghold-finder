package dev.strongholdfinder.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

public final class StrongholdClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        for (String mod : new String[] {"stronghold_finder", "xaeroworldmap"}) {
            if (!FabricLoader.getInstance().isModLoaded(mod)) {
                throw new AssertionError("Mod is not loaded: " + mod);
            }
        }
        context.takeScreenshot("boot");
    }
}
