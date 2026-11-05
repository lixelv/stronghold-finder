package dev.strongholdfinder;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class ModKeys {
    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("stronghold_finder", "main"));

    public static final KeyMapping CAPTURE = new KeyMapping(
            "key.stronghold_finder.capture", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping OPEN_SCREEN = new KeyMapping(
            "key.stronghold_finder.open", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, CATEGORY);

    private ModKeys() {
    }

    static void register() {
        KeyMappingHelper.registerKeyMapping(CAPTURE);
        KeyMappingHelper.registerKeyMapping(OPEN_SCREEN);
    }
}
