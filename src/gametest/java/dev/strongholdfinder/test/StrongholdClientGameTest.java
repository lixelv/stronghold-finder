package dev.strongholdfinder.test;

import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

public final class StrongholdClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        for (String mod : new String[] {"stronghold_finder", "xaeroworldmap"}) {
            if (!FabricLoader.getInstance().isModLoaded(mod)) {
                throw new AssertionError("Mod is not loaded: " + mod);
            }
        }
        context.takeScreenshot("boot");

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getServer().runCommand("tp @a 100 ~ 0");
            context.waitTicks(100);
            context.getInput().pressKey(openMapKey(context));
            context.waitTicks(80);
            context.takeScreenshot("map_player_at_100_0");
            context.getInput().scroll(3);
            context.waitTicks(40);
            context.takeScreenshot("map_zoomed_in");
            context.getInput().scroll(-6);
            context.waitTicks(40);
            context.takeScreenshot("map_zoomed_out");
            context.setScreen(() -> null);
            context.waitTick();
        }
    }

    private static KeyMapping openMapKey(ClientGameTestContext context) {
        return context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
                .filter(key -> key.getName().equals("gui.xaero_open_map"))
                .findFirst()
                .orElseThrow());
    }
}
