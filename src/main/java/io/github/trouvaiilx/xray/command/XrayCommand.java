package io.github.trouvaiilx.xray.command;

import io.github.trouvaiilx.xray.XrayState;
import io.github.trouvaiilx.xray.compat.SodiumRenderRefresher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

/**
 * Registers TWO client-only commands:
 *
 *   /xray toggle | /xray on | /xray off   <- primary, guaranteed clean tab-completion
 *   /trigger xray [true|false]            <- kept as a muscle-memory alias
 *
 * Both are entirely client-side and never touch the server or a real scoreboard objective.
 *
 * IMPORTANT about "/trigger": vanilla itself already owns a REAL server-side "/trigger
 * <objective>" command (for scoreboard-trigger objectives). Fabric API's own docs state
 * outright that when a client-only command and a server command share a top-level literal,
 * "the precedence rules... are an implementation detail... the aim is to make commands from
 * the server take precedence over client-sided commands" -- i.e. tab-completion under a
 * shared literal is explicitly NOT guaranteed. That's almost certainly why "/trigger x" was
 * failing to suggest "xray": the real vanilla trigger node was winning. "/xray" is a literal
 * nothing else registers, so it has no such ambiguity and will always tab-complete cleanly.
 * The "/trigger xray" alias is left in for convenience, but don't rely on its autocomplete.
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

    // Whether Sodium is installed never changes after startup (mods aren't hot-swapped), so
    // this is resolved once instead of walking FabricLoader's mod container map on every
    // single toggle/on/off invocation.
    private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");

    private XrayCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            // Primary command -- no literal collision with anything vanilla/server-side,
            // so tab-completion is guaranteed to work.
            dispatcher.register(
                    ClientCommands.literal("xray")
                            .executes(XrayCommand::toggle)
                            .then(ClientCommands.literal("toggle")
                                    .executes(XrayCommand::toggle))
                            .then(ClientCommands.literal("on")
                                    .executes(ctx -> setAndRespond(ctx, true)))
                            .then(ClientCommands.literal("off")
                                    .executes(ctx -> setAndRespond(ctx, false)))
                            .then(ClientCommands.literal("peek")
                                    .executes(XrayCommand::togglePeek)
                                    .then(ClientCommands.literal("on")
                                            .executes(ctx -> setPeekAndRespond(ctx, true)))
                                    .then(ClientCommands.literal("off")
                                            .executes(ctx -> setPeekAndRespond(ctx, false)))
                                    .then(ClientCommands.literal("radius")
                                            .then(ClientCommands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 10))
                                                    .executes(XrayCommand::setPeekRadius)))
                                    .then(ClientCommands.literal("opacity")
                                            .then(ClientCommands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                                                    .executes(XrayCommand::setPeekOpacity))))
            );

            // Legacy alias -- kept for the "/trigger xray" muscle memory, but its
            // tab-completion is NOT guaranteed (see class doc above).
            dispatcher.register(
                    ClientCommands.literal("trigger")
                            .then(ClientCommands.literal("xray")
                                    .executes(XrayCommand::toggle)
                                    .then(ClientCommands.argument("state", BoolArgumentType.bool())
                                            .executes(XrayCommand::setExplicit)))
            );
        });
    }

    private static int setAndRespond(CommandContext<FabricClientCommandSource> ctx, boolean value) {
        if (!SODIUM_LOADED) {
            sendSodiumMissingWarning(ctx);
            return 0;
        }
        if (value && !XrayState.isAllowed()) {
            sendServerOptInRequiredWarning(ctx);
            return 0;
        }
        XrayState.setEnabled(value);
        forceChunkRefresh();
        feedback(ctx, value);
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        if (!SODIUM_LOADED) {
            sendSodiumMissingWarning(ctx);
            return 0;
        }
        if (!XrayState.isEnabled() && !XrayState.isAllowed()) {
            sendServerOptInRequiredWarning(ctx);
            return 0;
        }
        boolean nowEnabled = XrayState.toggle();
        forceChunkRefresh();
        feedback(ctx, nowEnabled);
        return 1;
    }

    private static int setExplicit(CommandContext<FabricClientCommandSource> ctx) {
        if (!SODIUM_LOADED) {
            sendSodiumMissingWarning(ctx);
            return 0;
        }
        boolean value = BoolArgumentType.getBool(ctx, "state");
        if (value && !XrayState.isAllowed()) {
            sendServerOptInRequiredWarning(ctx);
            return 0;
        }
        XrayState.setEnabled(value);
        forceChunkRefresh();
        feedback(ctx, value);
        return 1;
    }

    private static int togglePeek(CommandContext<FabricClientCommandSource> ctx) {
        if (!XrayState.isAllowed()) {
            sendServerOptInRequiredWarning(ctx);
            return 0;
        }
        boolean peek = !io.github.trouvaiilx.xray.config.XrayConfig.isPeekEnabled();
        io.github.trouvaiilx.xray.config.XrayConfig.setPeekEnabled(peek);
        ctx.getSource().sendFeedback(Component.literal("Peek Mode " + (peek ? "enabled" : "disabled")));
        return 1;
    }

    private static int setPeekAndRespond(CommandContext<FabricClientCommandSource> ctx, boolean value) {
        if (value && !XrayState.isAllowed()) {
            sendServerOptInRequiredWarning(ctx);
            return 0;
        }
        io.github.trouvaiilx.xray.config.XrayConfig.setPeekEnabled(value);
        ctx.getSource().sendFeedback(Component.literal("Peek Mode " + (value ? "enabled" : "disabled")));
        return 1;
    }

    private static int setPeekRadius(CommandContext<FabricClientCommandSource> ctx) {
        int radius = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
        io.github.trouvaiilx.xray.config.XrayConfig.setPeekRadius(radius);
        ctx.getSource().sendFeedback(Component.literal("Peek Radius set to " + radius + " blocks"));
        return 1;
    }

    private static int setPeekOpacity(CommandContext<FabricClientCommandSource> ctx) {
        int opacity = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
        io.github.trouvaiilx.xray.config.XrayConfig.setPeekOpacity(opacity);
        ctx.getSource().sendFeedback(Component.literal("Peek Opacity set to " + opacity + "%"));
        return 1;
    }

    private static void sendSodiumMissingWarning(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal(
                "§c[X-Ray] Sodium is not installed! X-ray rendering requires the Sodium mod to be installed."));
    }

    private static void sendServerOptInRequiredWarning(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal(
                "§c[X-Ray] X-ray is disabled on this server. A server-side opt-in is required on multiplayer servers (Modrinth Content Rules Rule 3.3.a)."));
    }

    /**
     * Sodium only rebuilds a chunk section's mesh when something marks that section dirty
     * (a block edit, a chunk load) -- it has no idea our XrayState flag just changed. Without
     * forcing a refresh here, only sections that happen to get touched some other way (e.g.
     * breaking a block near them) would ever pick up the new state, which is why toggling
     * looked "stuck" / patchy / only-local before this was added.
     */
    private static void forceChunkRefresh() {
        if (SODIUM_LOADED) {
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
