package com.example.xray.keybind;

import com.example.xray.XrayClient;
import com.example.xray.gui.XrayConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Registers the Right Shift keybinding that opens/closes the X-ray config screen, and the
 * client-tick hook that reacts to it.
 *
 * Registration pattern verified against Fabric docs "Key Mappings 26.2"
 * (docs.fabricmc.net/develop/key-mappings): a KeyMapping.Category, a KeyMapping registered via
 * KeyMappingHelper (net.fabricmc.fabric.api.client.keymapping.v1 -- Fabric API 26.1 renamed
 * this package+class from the older client.keybinding.v1.KeyBindingHelper to match Mojang's
 * own KeyBinding -> KeyMapping rename), and reacting via consumeClick() inside
 * ClientTickEvents.END_CLIENT_TICK so held-key auto-repeat doesn't reopen the screen every
 * tick it's held.
 *
 * `InputConstants.KEY_RSHIFT` names the physical Right Shift key regardless of what the user
 * has it bound to in Options > Controls (this key mapping is intentionally not
 * user-rebindable in spirit -- it still shows up under Controls like any KeyMapping, since
 * that's a core part of how Minecraft's input system works, but Right Shift specifically is
 * what the product spec asked for as the open/close key).
 */
public final class XrayKeybinds {
    private static KeyMapping openMenuKey;

    private XrayKeybinds() {
    }

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(XrayClient.MOD_ID, "general"));

        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + XrayClient.MOD_ID + ".open_menu",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_RSHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                toggleMenu(client);
            }
        });
    }

    private static void toggleMenu(Minecraft client) {
        if (client.gui.screen() instanceof XrayConfigScreen) {
            client.gui.setScreen(null);
        } else if (client.gui.screen() == null && client.level != null && client.player != null) {
            // client.gui.screen() == null is NOT the same thing as "nothing is covering the
            // HUD". Minecraft's full-screen loading progress bar (LoadingOverlay) is a
            // separate Overlay, not a Screen -- it never occupies gui.screen(), so that alone
            // stays null for the entire ~5-15s a world takes to load. Without the level/player
            // check, pressing Right Shift during that window opens this screen (which isn't a
            // pause screen, so the game immediately tries to render the world behind it)
            // before the camera has ever been set up -- Camera#setup only runs once the player
            // entity actually exists in a level, so GameRenderer.extract() NPEs dereferencing
            // Camera's still-null `level` field. That's the exact crash this fixes.
            //
            // Closing (the branch above) is never gated the same way -- setScreen(null) can't
            // trigger a premature world render, so it's always safe regardless of load state.
            client.gui.setScreen(new XrayConfigScreen());
        }
    }
}
