package com.water2h2o.elementalverse.effect;

import com.water2h2o.elementalverse.registry.EVMobEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 把「易伤」挂进伤害结算。 */
@EventBusSubscriber(modid = "elemental_verse")
public final class VulnerableHandler {

    private VulnerableHandler() {
    }

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        var effect = event.getEntity().getEffect(EVMobEffects.VULNERABLE);
        if (effect == null) {
            return;
        }
        event.setAmount((float) (event.getAmount() * EVMobEffects.damageMultiplier(effect.getAmplifier())));
    }
}
