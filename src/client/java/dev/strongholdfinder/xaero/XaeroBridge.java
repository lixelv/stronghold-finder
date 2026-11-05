package dev.strongholdfinder.xaero;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.map.WorldMap;

public final class XaeroBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger("stronghold_finder");

    private static boolean installed;
    private static boolean failed;

    private XaeroBridge() {
    }

    public static boolean install() {
        if (installed || failed) {
            return installed;
        }
        try {
            if (WorldMap.mapElementRenderHandler == null) {
                return false;
            }
            WorldMap.mapElementRenderHandler.add(new StrongholdRenderer());
            installed = true;
        } catch (Throwable error) {
            disable(error);
        }
        return installed;
    }

    public static boolean isActive() {
        return installed && !failed;
    }

    public static void disable(Throwable error) {
        if (!failed) {
            failed = true;
            LOGGER.error("Stronghold Finder map layer disabled", error);
        }
    }
}
