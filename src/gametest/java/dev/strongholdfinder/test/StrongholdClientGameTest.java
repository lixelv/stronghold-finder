package dev.strongholdfinder.test;

import dev.strongholdfinder.ModKeys;
import dev.strongholdfinder.StrongholdFinderClient;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.Point;
import dev.strongholdfinder.core.Throw;
import dev.strongholdfinder.core.ThrowInput;
import dev.strongholdfinder.core.ThrowStore;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;

public final class StrongholdClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            testKeys(context, world);
            testMap(context, world);
        }
    }

    private static void testKeys(ClientGameTestContext context, TestSingleplayerContext world) {
        ThrowStore store = StrongholdFinderClient.store();
        TestInput input = context.getInput();
        world.getServer().runCommand("gamerule spawn_mobs false");
        world.getServer().runCommand("gamemode creative @a");
        world.getServer().runCommand("tp @a 0 ~ 0 0 0");
        context.waitTicks(40);

        context.getInput().lookAt(0, 0);
        input.pressKey(ModKeys.CAPTURE);
        context.waitTick();
        check(store.throwsList().size() == 1, "G should add one ray");
        check(Math.abs(last(store).yaw()) < 0.01, "looking south should give yaw 0, got " + last(store).yaw());

        input.pressKey(ModKeys.CAPTURE);
        context.waitTick();
        check(store.throwsList().size() == 1, "repeating G without moving should not add a ray");

        input.holdShift();
        input.pressKey(ModKeys.CAPTURE);
        input.releaseShift();
        context.waitTick();
        check(store.throwsList().size() == 1, "Shift+G without an eye should not add a ray");
        context.takeScreenshot("no_eye_message");

        world.getServer().runCommand("execute as @a at @s run summon minecraft:eye_of_ender ~10 ~3 ~");
        context.waitTicks(4);
        input.holdShift();
        input.pressKey(ModKeys.CAPTURE);
        input.releaseShift();
        context.waitTick();
        check(store.throwsList().size() == 2, "Shift+G with an eye should add a ray");
        check(Math.abs(last(store).yaw() + 90) < 20, "eye to the east should give yaw near -90, got " + last(store).yaw());
        float cameraYaw = context.computeOnClient(client -> client.player.getYRot());
        check(Math.abs(cameraYaw - last(store).yaw()) < 0.01, "camera should face the recorded yaw, got " + cameraYaw);
        context.takeScreenshot("aimed_at_eye");

        world.getServer().runCommand("kill @e[type=minecraft:eye_of_ender]");
        world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 128 0");
        context.waitTicks(60);
        input.pressKey(ModKeys.CAPTURE);
        context.waitTick();
        check(store.throwsList().size() == 2, "G in the Nether should not add a ray");
        world.getServer().runCommand("execute in minecraft:overworld run tp @a 0 100 0");
        context.waitTicks(60);
    }

    private static void testMap(ClientGameTestContext context, TestSingleplayerContext world) {
        ThrowStore store = StrongholdFinderClient.store();
        TestInput input = context.getInput();
        context.runOnClient(client -> {
            store.clear();
            Point target = new Point(60, -40);
            for (Point from : List.of(new Point(-50, 40), new Point(70, 60), new Point(-30, -60))) {
                store.add(new ThrowInput(from.x(), from.z(), Geometry.yawTo(from, target)));
            }
            store.add(new ThrowInput(5, 5, 130));
            store.setOn(store.lastAddedId(), false);
        });
        world.getServer().runCommand("tp @a 60 ~ -40");
        context.waitTicks(60);

        input.pressKey(mapKey(context));
        context.waitTicks(60);
        int[] center = context.computeOnClient(client ->
                new int[] {client.getWindow().getScreenWidth() / 2, client.getWindow().getScreenHeight() / 2});
        input.setCursorPos(center[0], center[1]);
        context.waitTicks(5);
        context.takeScreenshot("map_three_rays");
        input.scroll(2);
        context.waitTicks(30);
        context.takeScreenshot("map_zoomed_in");
        input.scroll(-5);
        context.waitTicks(30);
        context.takeScreenshot("map_zoomed_out");
        context.setScreen(() -> null);
        context.waitTick();
    }

    private static KeyMapping mapKey(ClientGameTestContext context) {
        return context.computeOnClient(client -> Arrays.stream(client.options.keyMappings)
                .filter(key -> key.getName().equals("gui.xaero_open_map"))
                .findFirst()
                .orElseThrow());
    }

    private static Throw last(ThrowStore store) {
        List<Throw> throwsList = store.throwsList();
        return throwsList.get(throwsList.size() - 1);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
