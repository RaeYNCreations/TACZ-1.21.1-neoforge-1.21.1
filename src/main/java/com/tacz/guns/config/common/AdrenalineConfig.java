package com.tacz.guns.config.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public class AdrenalineConfig {
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.IntValue KILL_THRESHOLD;
    public static ModConfigSpec.IntValue KILL_TIME_WINDOW;
    public static ModConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static ModConfigSpec.DoubleValue DAMAGE_MULTIPLIER;
    public static ModConfigSpec.IntValue ACTIVE_DURATION;
    public static ModConfigSpec.IntValue COOLDOWN_DURATION;

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("adrenaline");

        builder.comment("Enable/disable Adrenaline Mode feature");
        ENABLED = builder.define("Enabled", true);

        builder.comment("Number of kills required to activate Adrenaline Mode (default: 10)");
        KILL_THRESHOLD = builder.defineInRange("KillThreshold", 10, 1, 100);

        builder.comment("Time window in seconds for kills to count towards activation (default: 12 seconds)");
        KILL_TIME_WINDOW = builder.defineInRange("KillTimeWindow", 12, 1, 60);

        builder.comment("Health multiplier when Adrenaline Mode is active (default: 2.0x max health)");
        HEALTH_MULTIPLIER = builder.defineInRange("HealthMultiplier", 2.0, 1.0, 10.0);

        builder.comment("Damage multiplier for TACZ weapons when Adrenaline Mode is active (default: 2.5x damage)");
        DAMAGE_MULTIPLIER = builder.defineInRange("DamageMultiplier", 2.5, 1.0, 10.0);

        builder.comment("Duration in seconds that Adrenaline Mode remains active (default: 30 seconds)");
        ACTIVE_DURATION = builder.defineInRange("ActiveDuration", 30, 1, 300);

        builder.comment("Cooldown duration in seconds before Adrenaline Mode can activate again (default: 120 seconds / 2 minutes)");
        COOLDOWN_DURATION = builder.defineInRange("CooldownDuration", 120, 0, 600);

        builder.pop();
    }
}
