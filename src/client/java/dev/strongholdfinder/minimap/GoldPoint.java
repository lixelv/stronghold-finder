package dev.strongholdfinder.minimap;

import dev.strongholdfinder.core.GoldPointAction;
import dev.strongholdfinder.core.GoldPointName;
import dev.strongholdfinder.core.Solution;
import net.minecraft.client.Minecraft;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/** Touches Xaero's Minimap classes, so only {@link MinimapBridge} may call it, after checking the mod is there. */
final class GoldPoint {
    private static final String SYMBOL = "S";
    /** The stronghold's height is unknown, and a waypoint needs one. */
    private static final int Y = 64;

    /** Checked by object: the store makes a new solution after every change to the throws. */
    private static Solution syncedSolution;
    private static WaypointSet syncedSet;

    private GoldPoint() {
    }

    /** The session and its world appear some ticks after joining, so an early call just tries again later. */
    static void sync(Minecraft client, Solution solution) {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        WaypointSet set = currentSet(session);
        if (set == null || (solution == syncedSolution && set == syncedSet)) {
            return;
        }
        syncedSolution = solution;
        syncedSet = set;
        Waypoint point = find(set);
        GoldPointAction action = GoldPointAction.of(solution, point != null);
        if (action == GoldPointAction.NONE) {
            return;
        }
        if (action == GoldPointAction.REMOVE) {
            set.remove(point);
        } else {
            int x = (int) Math.round(solution.mean().x());
            int z = (int) Math.round(solution.mean().z());
            boolean russian = client.getLanguageManager().getSelected().startsWith("ru");
            String name = GoldPointName.of(russian);
            if (action == GoldPointAction.CREATE) {
                set.add(new Waypoint(x, Y, z, name, SYMBOL, WaypointColor.GOLD));
            } else {
                point.setX(x);
                point.setZ(z);
                point.setName(name);
            }
        }
        // World Map keeps its own copies of the waypoints and refreshes them when this time changes.
        session.getWaypointSession().setSetChangedTime(System.currentTimeMillis());
    }

    static boolean exists() {
        WaypointSet set = currentSet(BuiltInHudModules.MINIMAP.getCurrentSession());
        return set != null && find(set) != null;
    }

    private static WaypointSet currentSet(MinimapSession session) {
        if (session == null) {
            return null;
        }
        MinimapWorld world = session.getWorldManager().getCurrentWorld();
        return world == null ? null : world.getCurrentWaypointSet();
    }

    /** Looked up every time, because the player may have deleted or renamed the waypoint. */
    private static Waypoint find(WaypointSet set) {
        for (Waypoint point : set.getWaypoints()) {
            if (GoldPointName.isOurs(point.getName())
                    && SYMBOL.equals(point.getSymbol())
                    && point.getWaypointColor() == WaypointColor.GOLD) {
                return point;
            }
        }
        return null;
    }
}
