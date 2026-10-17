package com.water2h2o.elementalverse;

import com.water2h2o.elementalverse.client.SealedVaultScreen;
import com.water2h2o.elementalverse.registry.EVMenus;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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
    }

    static {
        if (false) {
            Minecraft.getInstance();
        }
    }
}
