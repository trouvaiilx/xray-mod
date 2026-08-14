package io.github.trouvaiilx.xray.server;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Server commands for managing X-Ray opt-in permissions across the server.
 *
 * Usage:
 *   /xrayserver status
 *   /xrayserver allow <true|false>
 */
public final class XrayServerCommand {

    private XrayServerCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("xrayserver")
                            .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_ADMIN))
                            .then(Commands.literal("status")
                                    .executes(XrayServerCommand::status))
                            .then(Commands.literal("allow")
                                    .then(Commands.argument("allowed", BoolArgumentType.bool())
                                            .executes(XrayServerCommand::setAllowed)))
            );
        });
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        boolean allowed = XrayServerConfig.isXrayAllowed();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§6[X-Ray Server] Server X-ray opt-in status: " + (allowed ? "§aALLOWED" : "§cDENIED")), false);
        return 1;
    }

    private static int setAllowed(CommandContext<CommandSourceStack> ctx) {
        boolean allowed = BoolArgumentType.getBool(ctx, "allowed");
        XrayServerConfig.setXrayAllowed(allowed);
        XrayServer.broadcastConsent(ctx.getSource().getServer(), allowed);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "§6[X-Ray Server] Updated X-ray opt-in permission to: " + (allowed ? "§aALLOWED" : "§cDENIED")
                + " (broadcast to all online clients)"), true);
        return 1;
    }
}
