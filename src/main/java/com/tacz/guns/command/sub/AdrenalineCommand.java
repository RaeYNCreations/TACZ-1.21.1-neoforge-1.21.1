package com.tacz.guns.command.sub;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tacz.guns.adrenaline.AdrenalineManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdrenalineCommand {
    private static final String COMMAND_NAME = "adrenaline";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        var adrenaline = Commands.literal(COMMAND_NAME);

        // /tacz adrenaline enable <true|false>
        adrenaline.then(Commands.literal("enable")
            .then(Commands.argument("state", BoolArgumentType.bool())
                .executes(AdrenalineCommand::setEnabled)));

        // /tacz adrenaline killthreshold <value>
        adrenaline.then(Commands.literal("killthreshold")
            .then(Commands.argument("value", IntegerArgumentType.integer(1, 100))
                .executes(AdrenalineCommand::setKillThreshold)));

        // /tacz adrenaline timewindow <seconds>
        adrenaline.then(Commands.literal("timewindow")
            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 60))
                .executes(AdrenalineCommand::setTimeWindow)));

        // /tacz adrenaline healthmultiplier <value>
        adrenaline.then(Commands.literal("healthmultiplier")
            .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                .executes(AdrenalineCommand::setHealthMultiplier)));

        // /tacz adrenaline damagemultiplier <value>
        adrenaline.then(Commands.literal("damagemultiplier")
            .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                .executes(AdrenalineCommand::setDamageMultiplier)));

        // /tacz adrenaline duration <seconds>
        adrenaline.then(Commands.literal("duration")
            .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 300))
                .executes(AdrenalineCommand::setDuration)));

        // /tacz adrenaline cooldown <seconds>
        adrenaline.then(Commands.literal("cooldown")
            .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 600))
                .executes(AdrenalineCommand::setCooldown)));

        // /tacz adrenaline status [player]
        adrenaline.then(Commands.literal("status")
            .executes(AdrenalineCommand::checkOwnStatus)
            .then(Commands.argument("player", EntityArgument.player())
                .executes(AdrenalineCommand::checkPlayerStatus)));

        return adrenaline;
    }

    private static int setEnabled(CommandContext<CommandSourceStack> context) {
        boolean state = BoolArgumentType.getBool(context, "state");
        AdrenalineManager.setEnabled(state);
        context.getSource().sendSuccess(() -> 
            Component.literal("Adrenaline Mode " + (state ? "enabled" : "disabled")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setKillThreshold(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        AdrenalineManager.setKillThreshold(value);
        context.getSource().sendSuccess(() -> 
            Component.literal("Kill threshold set to " + value), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setTimeWindow(CommandContext<CommandSourceStack> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        AdrenalineManager.setKillTimeWindow(seconds);
        context.getSource().sendSuccess(() -> 
            Component.literal("Time window set to " + seconds + " seconds"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setHealthMultiplier(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        AdrenalineManager.setHealthMultiplier(value);
        context.getSource().sendSuccess(() -> 
            Component.literal("Health multiplier set to " + value + "x"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDamageMultiplier(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        AdrenalineManager.setDamageMultiplier(value);
        context.getSource().sendSuccess(() -> 
            Component.literal("Damage multiplier set to " + value + "x"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDuration(CommandContext<CommandSourceStack> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        AdrenalineManager.setActiveDuration(seconds);
        context.getSource().sendSuccess(() -> 
            Component.literal("Active duration set to " + seconds + " seconds"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setCooldown(CommandContext<CommandSourceStack> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        AdrenalineManager.setCooldownDuration(seconds);
        context.getSource().sendSuccess(() -> 
            Component.literal("Cooldown duration set to " + seconds + " seconds"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int checkOwnStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            return checkStatus(context, player);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Command must be run by a player"));
            return 0;
        }
    }

    private static int checkPlayerStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            return checkStatus(context, player);
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Player not found"));
            return 0;
        }
    }

    private static int checkStatus(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        AdrenalineManager.PlayerAdrenalineData data = AdrenalineManager.getPlayerData(player.getUUID());
        
        if (data == null) {
            context.getSource().sendSuccess(() -> 
                Component.literal(player.getName().getString() + " has no adrenaline data"), false);
            return Command.SINGLE_SUCCESS;
        }

        long currentTime = System.currentTimeMillis();
        
        if (data.isActive()) {
            long remainingTime = (data.getActiveEndTime() - currentTime) / 1000;
            context.getSource().sendSuccess(() -> 
                Component.literal(player.getName().getString() + " is in Adrenaline Mode (" + remainingTime + "s remaining)"), false);
        } else if (currentTime < data.getCooldownEndTime()) {
            long cooldownRemaining = (data.getCooldownEndTime() - currentTime) / 1000;
            context.getSource().sendSuccess(() -> 
                Component.literal(player.getName().getString() + " is on cooldown (" + cooldownRemaining + "s remaining)"), false);
        } else {
            int recentKills = data.getRecentKillCount(currentTime);
            context.getSource().sendSuccess(() -> 
                Component.literal(player.getName().getString() + " has " + recentKills + " recent kills (ready for activation)"), false);
        }

        return Command.SINGLE_SUCCESS;
    }
}
