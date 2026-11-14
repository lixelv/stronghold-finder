package dev.strongholdfinder.minimap;

import dev.strongholdfinder.core.Dimensions;
import dev.strongholdfinder.core.ThrowStore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Xaero's Minimap is optional: nothing here may touch its classes before checking that it is installed. */
public final class MinimapBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("stronghold_finder");
    private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("xaerominimap");

    private static boolean failed;

    private MinimapBridge() {
    }

    public static boolean hasFailed() {
        return failed;
    }

    /** Coordinates only mean something in an overworld, so the Nether and the End leave the waypoint alone. */
    public static void tick(Minecraft client, ThrowStore store) {
        if (!INSTALLED || failed || !Dimensions.hasRays(client.level.dimension().identifier().toString())) {
            return;
        }
        try {
            GoldPoint.sync(client, store.solution());
        } catch (Throwable error) {
            disable(error);
        }
    }

    /** While true the waypoint marks the stronghold on every map, and our own mark would only cover it. */
    public static boolean showsPoint() {
        if (!INSTALLED || failed) {
            return false;
        }
        try {
            return GoldPoint.exists();
        } catch (Throwable error) {
            disable(error);
            return false;
        }
    }

    private static void disable(Throwable error) {
        failed = true;
        LOGGER.error("Stronghold Finder gold waypoint disabled", error);
    }
}
