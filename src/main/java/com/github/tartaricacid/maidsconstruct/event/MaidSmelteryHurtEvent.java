package com.github.tartaricacid.maidsconstruct.event;

import com.github.tartaricacid.maidsconstruct.MaidsConstruct;
import com.github.tartaricacid.maidsconstruct.config.MaidsConstructConfig;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAttackEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static slimeknights.tconstruct.common.TinkerDamageTypes.SMELTERY_HEAT;
import static slimeknights.tconstruct.common.TinkerDamageTypes.SMELTERY_MAGIC;

/**
 * 阻止女仆被 冶炼炉、熔铸炉 熔炼为流体。
 * 通过拦截 LivingAttackEvent，在 entity.hurt() 返回 false 时，
 * TiC 的 EntityMeltingModule 也不会向冶炼炉注入流体。
 */
@Mod.EventBusSubscriber(modid = MaidsConstruct.MOD_ID)
public class MaidSmelteryHurtEvent {
    @SubscribeEvent
    public static void onMaidAttack(MaidAttackEvent event) {
        if (!MaidsConstructConfig.MAID_SMELTERY_IMMUNITY.get()) {
            return;
        }
        DamageSource source = event.getSource();
        if (source.is(SMELTERY_HEAT) || source.is(SMELTERY_MAGIC)) {
            event.setCanceled(true);
        }
    }
}
