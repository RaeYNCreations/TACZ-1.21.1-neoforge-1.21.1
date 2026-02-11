package com.tacz.guns.event;

import com.tacz.guns.adrenaline.AdrenalineManager;
import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber
public class EntityDamageEvent {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingDamageEvent.Pre event){
        if (event.getSource().is(ModDamageTypes.BULLETS_TAG)) {
            LivingEntity living = event.getEntity();

            AttributeInstance resistance = living.getAttribute(ModAttributes.BULLET_RESISTANCE.getDelegate());
            if (resistance != null) {
                float modifiedDamage = event.getNewDamage() * (float) (1 - resistance.getValue());
                event.setNewDamage(modifiedDamage);
            }
            
            // Apply adrenaline damage multiplier if attacker is in adrenaline mode
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                double damageMultiplier = AdrenalineManager.getDamageMultiplier(attacker.getUUID());
                if (damageMultiplier > 1.0) {
                    float newDamage = event.getNewDamage() * (float) damageMultiplier;
                    event.setNewDamage(newDamage);
                }
            }
        }
    }
}
