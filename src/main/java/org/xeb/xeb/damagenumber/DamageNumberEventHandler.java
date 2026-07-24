package org.xeb.xeb.damagenumber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.xeb.xeb.Xeb;
import org.xeb.xeb.network.DamageNumberPacket;
import org.xeb.xeb.network.XEBNetwork;

/**
 * Event Listener para capturar el daño real final (LivingDamageEvent) en el Servidor
 * y sincronizarlo al Cliente para su renderizado.
 */
@Mod.EventBusSubscriber(modid = Xeb.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamageNumberEventHandler {

    /**
     * IMPORTANTE: Se engancha a LivingDamageEvent (NO LivingHurtEvent) porque este se dispara
     * después de aplicar armadura, encantos y resistencia, obteniendo el valor final exacto.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target == null || target.level().isClientSide()) return;

        float amount = event.getAmount();
        if (amount <= 0.001F) return;

        DamageSource source = event.getSource();
        boolean isCrit = false;
        String sourceType = getSourceCategory(source);

        // Verificación de Golpe Crítico Vanilla si el atacante es un jugador
        if (source.getEntity() instanceof ServerPlayer player) {
            boolean isJumpAttacking = player.fallDistance > 0.0F
                    && !player.onGround()
                    && !player.onClimbable()
                    && !player.isInWater()
                    && !player.hasEffect(MobEffects.BLINDNESS)
                    && !player.isPassenger();

            if (isJumpAttacking) {
                isCrit = true;
            }
        }

        Vec3 pos = target.position().add(0, target.getBbHeight() * 0.75D, 0);

        // Envía paquete únicamente a los jugadores en rango de visibilidad de la entidad
        XEBNetwork.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target),
                new DamageNumberPacket(target.getId(), amount, isCrit, sourceType, pos.x, pos.y, pos.z)
        );
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DamageNumberManager.getInstance().tick();
        }
    }

    private static String getSourceCategory(DamageSource source) {
        if (source == null) return "generic";
        String msgId = source.getMsgId();
        if (msgId == null) return "generic";

        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return "fire";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            return "explosion";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO) || msgId.contains("poison") || msgId.contains("wither")) {
            return "poison";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING)) {
            return "lightning";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) || msgId.contains("magic") || msgId.contains("indirectMagic")) {
            return "magic";
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return "fall";
        }
        return msgId;
    }
}
