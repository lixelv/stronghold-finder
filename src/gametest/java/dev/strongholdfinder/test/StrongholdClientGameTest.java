package dev.strongholdfinder.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.strongholdfinder.ModKeys;
import dev.strongholdfinder.screen.StrongholdScreen;
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
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.KeyMapping;

public final class StrongholdClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            testKeys(context, world);
            testMap(context, world);
            testScreen(context);
        }
        testWorldSeparation(context);
    }

    private static void testScreen(ClientGameTestContext context) {
        ThrowStore store = StrongholdFinderClient.store();
        TestInput input = context.getInput();
        context.runOnClient(client -> store.clear());
        input.pressKey(ModKeys.OPEN_SCREEN);
        context.waitForScreen(StrongholdScreen.class);
        context.waitTicks(2);
        context.takeScreenshot("screen_empty");

        input.typeChars("120 310 -47.3 ");
        context.waitTick();
        check(store.throwsList().size() == 1, "typing three numbers with spaces should add a throw");
        check(last(store).x() == 120 && last(store).z() == 310 && last(store).yaw() == -47.3, "fields should map to X Z yaw");

        input.typeChars("400");
        input.pressKey(InputConstants.KEY_RETURN);
        input.typeChars("500.5");
        input.pressKey(InputConstants.KEY_RETURN);
        input.typeChars("-160");
        input.pressKey(InputConstants.KEY_RETURN);
        context.waitTick();
        check(store.throwsList().size() == 2, "Enter should move between fields and add after yaw");
        check(last(store).z() == 500.5, "Enter flow should fill Z");

        input.typeChars("abc ");
        context.waitTick();
        check(store.throwsList().size() == 2, "a bad value should not add a throw");
        context.takeScreenshot("screen_bad_value");

        paste(context, "-300\t200\n-140");
        check(store.throwsList().size() == 3, "pasting three numbers should add a throw");
        paste(context, "/execute in minecraft:overworld run tp @s 900.50 71.00 700.25 170.00 -32.10");
        check(store.throwsList().size() == 4, "pasting an F3+C line should add a throw");
        check(last(store).x() == 900.5 && last(store).z() == 700.25 && last(store).yaw() == 170, "F3+C should map to X Z yaw");
        paste(context, "/execute in minecraft:the_nether run tp @s 1.00 2.00 3.00 4.00 5.00");
        check(store.throwsList().size() == 4, "a Nether line should not add a throw");
        paste(context, "1 2 3\n4 5 6");
        check(store.throwsList().size() == 6, "several lines should add several throws");
        context.takeScreenshot("screen_filled");

        context.clickScreenButton("screen.stronghold_finder.undo");
        context.waitTick();
        check(store.throwsList().size() == 4, "undo should revert the last paste");
        context.clickScreenButton("screen.stronghold_finder.overlay_on");
        context.waitTick();
        check(!store.overlayVisible(), "the overlay button should hide the layer");
        context.clickScreenButton("screen.stronghold_finder.overlay_off");
        context.waitTick();
        check(store.overlayVisible(), "the overlay button should show the layer");

        context.runOnClient(client -> {
            client.getLanguageManager().setSelected("ru_ru");
            client.reloadResourcePacks();
        });
        context.waitFor(client -> client.gui.overlay() == null, 1200);
        context.waitTicks(5);
        context.takeScreenshot("screen_ru");
        input.resizeWindow(1280, 720);
        context.waitTicks(5);
        context.takeScreenshot("screen_ru_1280");
        input.resizeWindow(854, 480);
        context.setScreen(() -> null);
        context.waitTick();
        input.pressKey(mapKey(context));
        context.waitTicks(40);
        int[] center = context.computeOnClient(client ->
                new int[] {client.getWindow().getScreenWidth() / 2, client.getWindow().getScreenHeight() / 2});
        input.setCursorPos(center[0], center[1]);
        input.scroll(-8);
        context.waitTicks(30);
        context.takeScreenshot("map_ru_far");
        context.setScreen(() -> null);
        context.waitTick();
    }

    private static void testWorldSeparation(ClientGameTestContext context) {
        ThrowStore store = StrongholdFinderClient.store();
        TestWorldSave save;
        List<Throw> before;
        try (TestSingleplayerContext first = context.worldBuilder().create()) {
            context.waitTicks(20);
            check(store.throwsList().isEmpty(), "a new world should start with no throws");
            context.runOnClient(client -> store.add(new ThrowInput(11, 22, 33)));
            before = store.throwsList();
            save = first.getWorldSave();
        }
        try (TestSingleplayerContext second = context.worldBuilder().create()) {
            context.waitTicks(20);
            check(store.throwsList().isEmpty(), "another world should not see these throws");
        }
        try (TestSingleplayerContext again = save.open()) {
            context.waitTicks(20);
            check(store.throwsList().equals(before), "reopening the world should bring its throws back");
        }
    }

    private static void paste(ClientGameTestContext context, String text) {
        context.runOnClient(client -> client.keyboardHandler.setClipboard(text));
        context.getInput().holdControl();
        context.getInput().pressKey(InputConstants.KEY_V);
        context.getInput().releaseControl();
        context.waitTick();
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
