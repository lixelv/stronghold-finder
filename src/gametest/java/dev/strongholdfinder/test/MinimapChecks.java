package dev.strongholdfinder.test;

import dev.strongholdfinder.StrongholdFinderClient;
import dev.strongholdfinder.core.Geometry;
import dev.strongholdfinder.core.GoldPointName;
import dev.strongholdfinder.core.Point;
import dev.strongholdfinder.core.ThrowInput;
import dev.strongholdfinder.core.ThrowStore;
import dev.strongholdfinder.minimap.MinimapBridge;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;

/** Loaded only when Xaero's Minimap is installed, because it uses the minimap's classes. */
final class MinimapChecks {
    private MinimapChecks() {
    }

    private static WaypointSet set() {
        return BuiltInHudModules.MINIMAP.getCurrentSession().getWorldManager().getCurrentWorld().getCurrentWaypointSet();
    }

    private static List<Waypoint> points(ClientGameTestContext context) {
        return context.computeOnClient(client -> {
            List<Waypoint> found = new ArrayList<>();
            for (Waypoint point : set().getWaypoints()) {
                if (GoldPointName.isOurs(point.getName())) {
                    found.add(point);
                }
            }
            return found;
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void addTwoRaysTo(ClientGameTestContext context, ThrowStore store, Point target) {
        context.runOnClient(client -> {
            store.clear();
            store.add(new ThrowInput(0, 0, Geometry.yawTo(new Point(0, 0), target)));
            store.add(new ThrowInput(40, 0, Geometry.yawTo(new Point(40, 0), target)));
        });
        context.waitTicks(2);
    }

    static void run(ClientGameTestContext context, TestSingleplayerContext world) {
        ThrowStore store = StrongholdFinderClient.store();
        context.runOnClient(client -> store.clear());
        context.waitTicks(2);
        check(points(context).isEmpty(), "no throws should mean no waypoint");

        // Two rays make a point: one gold waypoint at the average.
        addTwoRaysTo(context, store, new Point(30, 40));
        List<Waypoint> created = points(context);
        check(created.size() == 1, "two rays should make one waypoint, got " + created.size());
        Waypoint point = created.get(0);
        check(point.getX() == 30 && point.getZ() == 40, "the waypoint should be at the average, got " + point.getX() + " " + point.getZ());
        check(point.getWaypointColor() == WaypointColor.GOLD, "the waypoint should be gold");
        check(point.getY() == 64, "the waypoint should use the fixed height");
        check(context.computeOnClient(client -> MinimapBridge.showsPoint()), "the bridge should see the waypoint");
        context.waitTicks(20);
        context.takeScreenshot("gold_point_minimap");
        context.getInput().pressKey(StrongholdClientGameTest.mapKey(context));
        context.waitTicks(60);
        context.takeScreenshot("gold_point_world_map");
        context.setScreen(() -> null);
        context.waitTick();

        // A change moves the same waypoint.
        addTwoRaysTo(context, store, new Point(-20, 70));
        List<Waypoint> moved = points(context);
        check(moved.size() == 1 && moved.get(0) == point, "a new result should move the same waypoint");
        check(point.getX() == -20 && point.getZ() == 70, "the waypoint should follow the average, got " + point.getX() + " " + point.getZ());

        // Deleted by the player: stays gone until the next change, then comes back.
        context.runOnClient(client -> set().remove(point));
        context.waitTicks(5);
        check(points(context).isEmpty(), "a deleted waypoint should stay gone until the throws change");
        check(!context.computeOnClient(client -> MinimapBridge.showsPoint()), "the bridge should notice the deleted waypoint");
        addTwoRaysTo(context, store, new Point(10, 50));
        check(points(context).size() == 1, "the next change should bring the waypoint back");

        // In the Nether a change touches nothing; back home the waypoint catches up.
        world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 128 0");
        context.waitTicks(60);
        check(points(context).isEmpty(), "the Nether should have no waypoint");
        addTwoRaysTo(context, store, new Point(5, 60));
        check(points(context).isEmpty(), "a change in the Nether should not create a waypoint there");
        world.getServer().runCommand("execute in minecraft:overworld run tp @a 0 100 0");
        context.waitTicks(60);
        List<Waypoint> home = points(context);
        check(home.size() == 1 && home.get(0).getX() == 5 && home.get(0).getZ() == 60,
                "back in the Overworld the waypoint should catch up");

        // Clearing removes it.
        context.runOnClient(client -> store.clear());
        context.waitTicks(2);
        check(points(context).isEmpty(), "clearing the throws should remove the waypoint");
        System.out.println("SFTEST minimap: create, move, delete, Nether and clear work");
    }
}
