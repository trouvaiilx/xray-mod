package io.github.trouvaiilx.xray.keybind;

import io.github.trouvaiilx.xray.XrayClient;
import io.github.trouvaiilx.xray.gui.XrayConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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
    private static KeyMapping toggleXrayKey;
    private static KeyMapping togglePeekKey;

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

        toggleXrayKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + XrayClient.MOD_ID + ".toggle_xray",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_BACKSLASH,
                category
        ));

        togglePeekKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + XrayClient.MOD_ID + ".toggle_peek",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_APOSTROPHE,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                toggleMenu(client);
            }
            while (toggleXrayKey.consumeClick()) {
                toggleXray(client);
            }
            while (togglePeekKey.consumeClick()) {
                togglePeek(client);
            }
        });
    }

    private static void toggleXray(Minecraft client) {
        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        "§c[X-Ray] Sodium is not installed! X-ray rendering requires the Sodium mod to be installed."));
            }
            return;
        }
        boolean enabled = io.github.trouvaiilx.xray.XrayState.toggle();
        XrayClient.refreshRender();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(
                    "§a[X-Ray] X-ray " + (enabled ? "enabled" : "disabled")));
        }
    }

    private static void togglePeek(Minecraft client) {
        boolean peek = !io.github.trouvaiilx.xray.config.XrayConfig.isPeekEnabled();
        io.github.trouvaiilx.xray.config.XrayConfig.setPeekEnabled(peek);
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(
                    "§a[X-Ray] Peek Mode " + (peek ? "enabled" : "disabled")));
        }
    }

    private static void toggleMenu(Minecraft client) {
        if (!FabricLoader.getInstance().isModLoaded("sodium")) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal(
                        "§c[X-Ray] Sodium is not installed! X-ray rendering requires the Sodium mod to be installed."));
            }
            return;
        }
        if (client.gui.screen() instanceof XrayConfigScreen) {
            client.gui.setScreen(null);
        } else if (client.gui.screen() == null && client.level != null && client.player != null) {
            client.gui.setScreen(new XrayConfigScreen());
        }
    }
}
