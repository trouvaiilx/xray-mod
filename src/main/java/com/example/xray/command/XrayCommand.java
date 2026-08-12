package com.example.xray.command;

import com.example.xray.XrayState;
import com.example.xray.compat.SodiumRenderRefresher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/**
 * Registers a CLIENT-ONLY command, "/trigger xray". It never touches the server or a real
 * scoreboard objective — it just gives you the familiar "/trigger <name>" muscle memory
 * without depending on a datapack objective existing in the world you're in.
 *
 * NOTE: Fabric API renamed the old "ClientCommandManager" helper class to "ClientCommands"
 * (to match vanilla's own Commands/ClientCommands naming under Mojang mappings), and command
 * registration now happens inside a ClientCommandRegistrationCallback.EVENT listener rather
 * than directly against a static DISPATCHER field at mod-init time. Verified against Fabric's
 * current docs (docs.fabricmc.net/develop/commands/basics, current as of this writing).
 *
 * If you specifically want to drive this off a REAL vanilla scoreboard trigger objective
 * instead, see the comment at the bottom of this file for the alternative approach.
 */
public final class XrayCommand {

    private XrayCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    ClientCommands.literal("trigger")
                            .then(ClientCommands.literal("xray")
                                    .executes(XrayCommand::toggle)
                                    .then(ClientCommands.argument("state", BoolArgumentType.bool())
                                            .executes(XrayCommand::setExplicit)))
            );
        });
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        boolean nowEnabled = XrayState.toggle();
        forceChunkRefresh();
        feedback(ctx, nowEnabled);
        return 1;
    }

    private static int setExplicit(CommandContext<FabricClientCommandSource> ctx) {
        boolean value = BoolArgumentType.getBool(ctx, "state");
        XrayState.setEnabled(value);
        forceChunkRefresh();
        feedback(ctx, value);
        return 1;
    }

    /**
     * Sodium only rebuilds a chunk section's mesh when something marks that section dirty
     * (a block edit, a chunk load) -- it has no idea our XrayState flag just changed. Without
     * forcing a refresh here, only sections that happen to get touched some other way (e.g.
     * breaking a block near them) would ever pick up the new state, which is why toggling
     * looked "stuck" / patchy / only-local before this was added.
     */
    private static void forceChunkRefresh() {
        if (FabricLoader.getInstance().isModLoaded("sodium")) {
            SodiumRenderRefresher.refreshAllChunks();
        }
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
        ctx.getSource().sendFeedback(Component.literal(
                "X-ray " + (enabled ? "enabled" : "disabled")));
    }
}

/*
 * ALTERNATIVE: driving this off a REAL "/trigger <objective>" scoreboard command.
 *
 * This only makes sense if you (or a datapack you ship alongside the mod) already run:
 *   scoreboard objectives add xray trigger
 *
 * so that a real player can type vanilla "/trigger xray set 1" and have the SERVER accept it.
 * Your client then needs to watch for scoreboard score updates for that objective and flip
 * XrayState accordingly. Sketch:
 *
 *   ClientPlayNetworking.registerGlobalReceiver(ClientboundSetScorePacket.TYPE, (packet, context) -> {
 *       if (packet.objectiveName().equals("xray")) {
 *           context.client().execute(() -> XrayState.setEnabled(packet.score() != 0));
 *       }
 *   });
 *
 * This is strictly more fragile (needs the objective to exist server-side, needs the player
 * to be tracked in that objective, and only fires on score CHANGES, not on join) so the
 * client-only command above is the recommended default.
 */
