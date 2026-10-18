package com.water2h2o.elementalverse;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.water2h2o.elementalverse.config.EVConfig;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.VialDecayEvents;
import com.water2h2o.elementalverse.registry.EVBlockEntities;
import com.water2h2o.elementalverse.registry.EVBlocks;
import com.water2h2o.elementalverse.registry.EVCreativeTabs;
import com.water2h2o.elementalverse.registry.EVDataComponents;
import com.water2h2o.elementalverse.registry.EVItems;
import com.water2h2o.elementalverse.registry.EVMenus;
import com.water2h2o.elementalverse.registry.EVMobEffects;
import com.water2h2o.elementalverse.registry.EVSounds;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 《水塔夜谈 · 元素之诗》主类。
 *
 * <p>阶段 01 只做一件事：让「瓶子里的元素会变质」成立。
 * 这里保持极薄 —— 注册交给 registry 包，逻辑交给 element 包。
 */
@Mod(SpiritsTowerNightsElementalVerse.MODID)
public class SpiritsTowerNightsElementalVerse {

    /** 模组 ID，必须与 neoforge.mods.toml 及 gradle.properties 一致。 */
    public static final String MODID = "elemental_verse";

    public static final Logger LOGGER = LogUtils.getLogger();

    public SpiritsTowerNightsElementalVerse(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // 注册表：顺序无所谓，DeferredRegister 只登记、不立即解析
        EVDataComponents.register(modEventBus);
        EVBlocks.register(modEventBus);
        EVItems.register(modEventBus);
        EVBlockEntities.register(modEventBus);
        EVMenus.register(modEventBus);
        EVCreativeTabs.register(modEventBus);
        EVSounds.register(modEventBus);
        EVMobEffects.register(modEventBus);

        // 衰变需要监听游戏事件（玩家背包、掉落物），挂到 NeoForge 事件总线
        NeoForge.EVENT_BUS.register(VialDecayEvents.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, EVConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[元素之诗] 阶段 01 就绪：{} 种元素形态在册，衰变预设 = {}",
                ElementType.values().length,
                EVConfig.getPreset());
    }
}
