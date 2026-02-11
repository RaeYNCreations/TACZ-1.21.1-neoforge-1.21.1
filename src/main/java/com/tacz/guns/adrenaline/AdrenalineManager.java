package com.tacz.guns.adrenaline;

import com.tacz.guns.config.common.AdrenalineConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AdrenalineManager {
    private static final Map<UUID, PlayerAdrenalineData> playerData = new ConcurrentHashMap<>();
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("a8b3c4d5-e6f7-8901-2345-6789abcdef01");

    public static class PlayerAdrenalineData {
        private final List<Long> recentKills = new ArrayList<>();
        private boolean isActive = false;
        private long activeEndTime = 0;
        private long cooldownEndTime = 0;
        private double originalMaxHealth = 0;

        public void addKill(long currentTime) {
            recentKills.add(currentTime);
            cleanOldKills(currentTime);
        }

        public void cleanOldKills(long currentTime) {
            long timeWindow = AdrenalineConfig.KILL_TIME_WINDOW.get() * 1000L;
            recentKills.removeIf(killTime -> currentTime - killTime > timeWindow);
        }

        public int getRecentKillCount(long currentTime) {
            cleanOldKills(currentTime);
            return recentKills.size();
        }

        public boolean canActivate(long currentTime) {
            return !isActive && currentTime >= cooldownEndTime;
        }

        public void activate(long currentTime, double maxHealth) {
            isActive = true;
            originalMaxHealth = maxHealth;
            int duration = AdrenalineConfig.ACTIVE_DURATION.get();
            activeEndTime = currentTime + (duration * 1000L);
            recentKills.clear();
        }

        public void deactivate(long currentTime) {
            if (isActive) {
                isActive = false;
                int cooldown = AdrenalineConfig.COOLDOWN_DURATION.get();
                cooldownEndTime = currentTime + (cooldown * 1000L);
            }
        }

        public boolean isActive() {
            return isActive;
        }

        public long getActiveEndTime() {
            return activeEndTime;
        }

        public long getCooldownEndTime() {
            return cooldownEndTime;
        }

        public double getOriginalMaxHealth() {
            return originalMaxHealth;
        }
    }

    public static void onPlayerKill(ServerPlayer player) {
        if (!AdrenalineConfig.ENABLED.get()) {
            return;
        }

        UUID playerId = player.getUUID();
        PlayerAdrenalineData data = playerData.computeIfAbsent(playerId, k -> new PlayerAdrenalineData());
        
        long currentTime = System.currentTimeMillis();
        data.addKill(currentTime);

        int killCount = data.getRecentKillCount(currentTime);
        int threshold = AdrenalineConfig.KILL_THRESHOLD.get();

        if (killCount >= threshold && data.canActivate(currentTime)) {
            activateAdrenalineMode(player, data, currentTime);
        }
    }

    private static void activateAdrenalineMode(ServerPlayer player, PlayerAdrenalineData data, long currentTime) {
        double currentMaxHealth = player.getMaxHealth();
        data.activate(currentTime, currentMaxHealth);

        double healthMultiplier = AdrenalineConfig.HEALTH_MULTIPLIER.get();
        applyHealthModifier(player, healthMultiplier);

        player.sendSystemMessage(Component.translatable("message.tacz.adrenaline.activated"));
    }

    private static void applyHealthModifier(ServerPlayer player, double multiplier) {
        AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttribute != null) {
            // Remove existing modifier if present
            healthAttribute.removeModifier(HEALTH_MODIFIER_UUID);
            
            // Calculate the additive value (multiplier - 1) * base value
            double baseValue = healthAttribute.getBaseValue();
            double additiveValue = (multiplier - 1.0) * baseValue;
            
            // Create and add new modifier
            AttributeModifier modifier = new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                "Adrenaline Mode Health Boost",
                additiveValue,
                AttributeModifier.Operation.ADD_VALUE
            );
            healthAttribute.addPermanentModifier(modifier);
            
            // Heal player to match new max health
            player.setHealth((float) (player.getHealth() * multiplier));
        }
    }

    private static void removeHealthModifier(ServerPlayer player) {
        AttributeInstance healthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.removeModifier(HEALTH_MODIFIER_UUID);
            
            // Adjust current health if it's higher than new max
            float currentHealth = player.getHealth();
            float newMaxHealth = player.getMaxHealth();
            if (currentHealth > newMaxHealth) {
                player.setHealth(newMaxHealth);
            }
        }
    }

    public static void tick(ServerPlayer player) {
        if (!AdrenalineConfig.ENABLED.get()) {
            return;
        }

        UUID playerId = player.getUUID();
        PlayerAdrenalineData data = playerData.get(playerId);
        
        if (data != null && data.isActive()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime >= data.getActiveEndTime()) {
                deactivateAdrenalineMode(player, data, currentTime);
            }
        }
    }

    private static void deactivateAdrenalineMode(ServerPlayer player, PlayerAdrenalineData data, long currentTime) {
        removeHealthModifier(player);
        data.deactivate(currentTime);
        player.sendSystemMessage(Component.translatable("message.tacz.adrenaline.deactivated"));
    }

    public static void onPlayerDeath(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerAdrenalineData data = playerData.get(playerId);
        
        if (data != null && data.isActive()) {
            removeHealthModifier(player);
            data.deactivate(System.currentTimeMillis());
        }
    }

    public static void onPlayerLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerAdrenalineData data = playerData.get(playerId);
        
        if (data != null && data.isActive()) {
            removeHealthModifier(player);
        }
        // Data is kept in memory for when player rejoins
    }

    public static boolean isPlayerInAdrenalineMode(UUID playerId) {
        PlayerAdrenalineData data = playerData.get(playerId);
        return data != null && data.isActive();
    }

    public static double getDamageMultiplier(UUID playerId) {
        if (isPlayerInAdrenalineMode(playerId)) {
            return AdrenalineConfig.DAMAGE_MULTIPLIER.get();
        }
        return 1.0;
    }

    public static PlayerAdrenalineData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }

    public static void clearPlayerData(UUID playerId) {
        playerData.remove(playerId);
    }

    public static void setEnabled(boolean enabled) {
        AdrenalineConfig.ENABLED.set(enabled);
        AdrenalineConfig.ENABLED.save();
    }

    public static void setKillThreshold(int threshold) {
        AdrenalineConfig.KILL_THRESHOLD.set(threshold);
        AdrenalineConfig.KILL_THRESHOLD.save();
    }

    public static void setKillTimeWindow(int seconds) {
        AdrenalineConfig.KILL_TIME_WINDOW.set(seconds);
        AdrenalineConfig.KILL_TIME_WINDOW.save();
    }

    public static void setHealthMultiplier(double multiplier) {
        AdrenalineConfig.HEALTH_MULTIPLIER.set(multiplier);
        AdrenalineConfig.HEALTH_MULTIPLIER.save();
    }

    public static void setDamageMultiplier(double multiplier) {
        AdrenalineConfig.DAMAGE_MULTIPLIER.set(multiplier);
        AdrenalineConfig.DAMAGE_MULTIPLIER.save();
    }

    public static void setActiveDuration(int seconds) {
        AdrenalineConfig.ACTIVE_DURATION.set(seconds);
        AdrenalineConfig.ACTIVE_DURATION.save();
    }

    public static void setCooldownDuration(int seconds) {
        AdrenalineConfig.COOLDOWN_DURATION.set(seconds);
        AdrenalineConfig.COOLDOWN_DURATION.save();
    }
}
