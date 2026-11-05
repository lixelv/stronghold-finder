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
import dev.strongholdfinder.core.TwoPointRay;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.StructureTags;

public final class StrongholdClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            testKeys(context, world);
            testMap(context, world);
            testScreen(context);
            if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
                MinimapChecks.run(context, world);
            } else {
                System.out.println("SFTEST running without the minimap");
            }
        }
        testWorldSeparation(context);
        testRealThrows(context);
        testDedicatedServer(context);
    }

    /** On a real server Shift+G reads a real eye without moving the camera, and G keeps working. */
    private static void testDedicatedServer(ClientGameTestContext context) {
        ThrowStore store = StrongholdFinderClient.store();
        TestInput input = context.getInput();
        try (TestDedicatedServerContext server = context.worldBuilder().setUseConsistentSettings(false).createServer();
                TestDedicatedServerConnection connection = server.connect()) {
            context.waitTicks(40);
            check(store.throwsList().isEmpty(), "a new server should start with no throws");
            server.runCommand("gamemode creative @a");
            server.runCommand("give @a minecraft:ender_eye 16");
            BlockPos first = server.computeOnServer(s -> s.overworld()
                    .findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, BlockPos.ZERO, 100, false));
            check(first != null, "the server world should have a stronghold");
            server.runCommand("tp @a " + first.getX() / 2 + " 220 " + first.getZ() / 2);
            context.waitTicks(120);
            BlockPos here = context.computeOnClient(client -> client.player.blockPosition());
            BlockPos real = server.computeOnServer(s -> s.overworld()
                    .findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, here, 100, false));

            float yawBefore = context.computeOnClient(client -> client.player.getYRot());
            float pitchBefore = context.computeOnClient(client -> client.player.getXRot());
            throwEyeAndCapture(context, input, 10);
            check(store.throwsList().size() == 1, "Shift+G on a server should add a ray");
            Throw thrown = last(store);
            check(thrown.ray() instanceof TwoPointRay, "Shift+G should give a two-point ray, got " + thrown.ray());
            double yawToStronghold = Geometry.yawTo(new Point(thrown.x(), thrown.z()), new Point(real.getX(), real.getZ()));
            double off = Math.abs(Geometry.normYaw(thrown.yaw() - yawToStronghold));
            System.out.println("SFTEST server: Shift+G off by " + off + " degrees");
            check(off < 0.1, "the ray should point at the stronghold, off by " + off);
            float yawAfter = context.computeOnClient(client -> client.player.getYRot());
            float pitchAfter = context.computeOnClient(client -> client.player.getXRot());
            check(yawAfter == yawBefore && pitchAfter == pitchBefore, "Shift+G should not move the camera");
            context.takeScreenshot("server_shift_g");

            input.pressKey(ModKeys.CAPTURE);
            context.waitTick();
            check(store.throwsList().size() == 2, "G should still work on a server");
            server.runCommand("kill @e[type=minecraft:eye_of_ender]");
        }
    }

    /** End to end: throw real eyes in a generated world and compare the result with the stronghold the game knows. */
    private static void testRealThrows(ClientGameTestContext context) {
        ThrowStore store = StrongholdFinderClient.store();
        TestInput input = context.getInput();
        try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).create()) {
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("give @a minecraft:ender_eye 16");
            BlockPos real = world.getServer().computeOnServer(server -> server.overworld()
                    .findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, BlockPos.ZERO, 100, false));
            check(real != null, "the world should have a stronghold");
            // Halfway to the stronghold it stays the nearest one for both throws; near spawn the ring's strongholds tie.
            world.getServer().runCommand("tp @a " + real.getX() / 2 + " 200 " + real.getZ() / 2);
            context.waitTicks(200);

            // Pressed while the eye still stands still on the client, so the mod has to wait for it to move.
            throwEyeAndCapture(context, input, 3);
            check(store.throwsList().size() == 1, "the first real eye should give a ray");
            Throw first = last(store);
            check(first.ray() instanceof TwoPointRay, "Shift+G should give a two-point ray, got " + first.ray());
            Point side = Geometry.dir(first.yaw() + 90);
            int nextX = (int) Math.round(first.x() + side.x() * 400);
            int nextZ = (int) Math.round(first.z() + side.z() * 400);
            world.getServer().runCommand("tp @a " + nextX + " 200 " + nextZ);
            context.waitTicks(200);

            throwEyeAndCapture(context, input, 15);
            check(store.throwsList().size() == 2, "the second real eye should give a ray");
            for (Throw t : store.throwsList()) {
                System.out.println("SFTEST throw " + t + " yawToStronghold=" + Geometry.yawTo(new Point(t.x(), t.z()), new Point(real.getX(), real.getZ())));
            }
            Point mean = store.solution().mean();
            check(mean != null, "two real throws should give a point");
            double err = store.solution().err();
            double miss = Math.hypot(mean.x() - real.getX(), mean.z() - real.getZ());
            System.out.println("SFTEST real throws: mean=" + mean + " err=" + err + " stronghold=" + real + " miss=" + miss);
            context.setScreen(StrongholdScreen::new);
            context.waitTicks(5);
            context.takeScreenshot("real_throws_screen");
            context.setScreen(() -> null);
            context.waitTick();
            input.pressKey(mapKey(context));
            context.waitTicks(60);
            context.takeScreenshot("real_throws_map");
            context.setScreen(() -> null);
            context.waitTick();
            check(miss <= Math.max(err * 3, 64), "the result should land near the real stronghold, missed by " + miss);

            // Too late: the eye may already fly back, so it counts as not seen.
            input.pressKey(options -> options.keyUse);
            context.waitTicks(40);
            input.holdShift();
            input.pressKey(ModKeys.CAPTURE);
            input.releaseShift();
            context.waitTicks(3);
            check(store.throwsList().size() == 2, "a late Shift+G should not add a ray");
            context.takeScreenshot("late_eye_not_seen");
        }
    }

    private static void throwEyeAndCapture(ClientGameTestContext context, TestInput input, int delay) {
        input.pressKey(options -> options.keyUse);
        context.waitTicks(delay);
        input.holdShift();
        input.pressKey(ModKeys.CAPTURE);
        input.releaseShift();
        context.waitTicks(10);
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

        context.setScreen(StrongholdScreen::new);
        context.waitTicks(2);
        paste(context, "77");
        input.typeChars(" 88 99 ");
        context.waitTick();
        check(store.throwsList().size() == 3, "pasting one number should fill the field on any keyboard layout");
        check(last(store).x() == 77, "the pasted number should land in X, got " + last(store).x());
        context.runOnClient(client -> store.remove(store.lastAddedId()));
        context.setScreen(StrongholdScreen::new);
        context.waitTicks(2);

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

        // A summoned eye has no target and never moves, so there is no direction to read.
        world.getServer().runCommand("execute as @a at @s run summon minecraft:eye_of_ender ~10 ~3 ~");
        context.waitTicks(4);
        float yawBefore = context.computeOnClient(client -> client.player.getYRot());
        input.holdShift();
        input.pressKey(ModKeys.CAPTURE);
        input.releaseShift();
        context.waitTicks(40);
        check(store.throwsList().size() == 1, "Shift+G on an eye that does not move should not add a ray");
        float yawAfter = context.computeOnClient(client -> client.player.getYRot());
        check(yawAfter == yawBefore, "Shift+G should not move the camera");
        context.takeScreenshot("eye_standing_still");

        world.getServer().runCommand("kill @e[type=minecraft:eye_of_ender]");
        world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 128 0");
        context.waitTicks(60);
        input.pressKey(ModKeys.CAPTURE);
        context.waitTick();
        check(store.throwsList().size() == 1, "G in the Nether should not add a ray");
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

        input.resizeWindow(1920, 1080);
        context.waitTicks(5);
        int gui = context.computeOnClient(client -> client.getWindow().getGuiScale());
        System.out.println("SFTEST gui scale at 1080p: " + gui);
        input.pressKey(mapKey(context));
        context.waitTicks(40);
        input.setCursorPos(960 + 5 * gui, 540 - 3 * gui);
        context.waitTicks(5);
        context.takeScreenshot("map_1080p_hover_off_center");

        context.setScreen(() -> null);
        context.runOnClient(client -> {
            store.clear();
            store.add(new ThrowInput(-3000, 0, Geometry.yawTo(new Point(-3000, 0), new Point(60, -40))));
            store.add(new ThrowInput(-3000, 400, Geometry.yawTo(new Point(-3000, 400), new Point(60, -40))));
            store.add(new ThrowInput(100, 400, 170));
            store.setOn(store.lastAddedId(), false);
        });
        input.pressKey(mapKey(context));
        context.waitTicks(40);
        input.setCursorPos(960, 540);
        input.scroll(4);
        context.waitTicks(60);
        double err = context.computeOnClient(client -> store.solution().err());
        int fps = context.computeOnClient(client -> client.getFps());
        System.out.println("SFTEST error circle err=" + err + " fps=" + fps);
        context.takeScreenshot("map_1080p_error_circle");
        context.setScreen(() -> null);
        input.resizeWindow(854, 480);
        context.waitTicks(5);
    }

    static KeyMapping mapKey(ClientGameTestContext context) {
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
