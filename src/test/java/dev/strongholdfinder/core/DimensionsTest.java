package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DimensionsTest {
    @Test
    void overworldAndCustomOverworldsHaveRays() {
        for (String id : new String[] {
                "minecraft:overworld", "minecraft:game_world", "speedrun:run_42", "OVERWORLD", "world"}) {
            assertTrue(Dimensions.hasRays(id), id);
        }
    }

    @Test
    void netherAndEndHaveNoRaysUnderAnyName() {
        for (String id : new String[] {
                "minecraft:the_nether", "minecraft:the_end", "minecraft:game_world_nether",
                "minecraft:game_world_the_end", "world_nether", "world_the_end", "MINECRAFT:THE_NETHER"}) {
            assertFalse(Dimensions.hasRays(id), id);
        }
    }
}
