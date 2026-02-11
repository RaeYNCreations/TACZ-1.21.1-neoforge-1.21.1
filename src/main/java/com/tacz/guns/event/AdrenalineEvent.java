package com.tacz.guns.event;

import com.tacz.guns.adrenaline.AdrenalineManager;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class AdrenalineEvent {

    @SubscribeEvent
    public static void onEntityKilledByGun(EntityKillByGunEvent event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }

        LivingEntity attacker = event.getAttacker();
        LivingEntity terminated = event.getKilledEntity();

        if (attacker instanceof ServerPlayer player && terminated instanceof Enemy) {
            AdrenalineManager.onPlayerKill(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdrenalineManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdrenalineManager.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdrenalineManager.onPlayerLogout(player);
        }
    }
}
