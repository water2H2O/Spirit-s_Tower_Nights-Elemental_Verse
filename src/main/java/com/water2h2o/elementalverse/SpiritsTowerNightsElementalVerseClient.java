package com.water2h2o.elementalverse;

import com.water2h2o.elementalverse.client.DecayBarDecorator;
import com.water2h2o.elementalverse.client.PouchScreen;
import com.water2h2o.elementalverse.client.SealedVaultScreen;
import com.water2h2o.elementalverse.registry.EVMenus;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SpiritsTowerNightsElementalVerse.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = SpiritsTowerNightsElementalVerse.MODID, value = Dist.CLIENT)
public class SpiritsTowerNightsElementalVerseClient {
    public SpiritsTowerNightsElementalVerseClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SpiritsTowerNightsElementalVerse.LOGGER.info("元素之诗 客户端就绪");
    }

    /** 把界面绑定到菜单类型；不做这一步，右键密封仓会报"没有对应的界面"。 */
    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(EVMenus.SEALED_VAULT.get(), SealedVaultScreen::new);
        event.register(EVMenus.POUCH.get(), PouchScreen::new);
        event.register(EVMenus.RUNE_CABINET.get(),
                com.water2h2o.elementalverse.client.RuneCabinetScreen::new);
        event.register(EVMenus.RITUAL_ALTAR.get(),
                com.water2h2o.elementalverse.client.RitualAltarScreen::new);
    }

    /**
     * 法术弹射物的渲染：直接复用原版的"投掷物"渲染器。
     *
     * <p>它会画出实体身上的物品栈 —— 也就是那瓶对应元素的元素瓶，
     * 所以飞出去的是一瓶看得清的元素，而不是一个看不出是什么的光点。
     */
    @SubscribeEvent
    static void onRegisterRenderers(
            net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                com.water2h2o.elementalverse.registry.EVEntities.SPELL_PROJECTILE.get(),
                com.water2h2o.elementalverse.client.ElementProjectileRenderer::new);
    }

    /** 给三种元素容器加"衰变期"横条（外观同耐久条，但不碰原版耐久系统）。 */
    @SubscribeEvent
    static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        DecayBarDecorator decorator = new DecayBarDecorator();
        event.register(com.water2h2o.elementalverse.registry.EVItems.ELEMENT_VIAL.get(), decorator);
        event.register(com.water2h2o.elementalverse.registry.EVItems.REFRACTION_PRISM.get(), decorator);
        event.register(com.water2h2o.elementalverse.registry.EVItems.SHADOW_BOTTLE.get(), decorator);
    }

    static {
        if (false) {
            Minecraft.getInstance();
        }
    }
}
