package com.water2h2o.elementalverse.registry;

import com.water2h2o.elementalverse.menu.PouchMenu;
import com.water2h2o.elementalverse.menu.SealedVaultMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 菜单（界面）类型注册。 */
public final class EVMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, "elemental_verse");

    /** 密封仓界面。带一个 BlockPos 以便客户端找到方块实体。 */
    public static final DeferredHolder<MenuType<?>, MenuType<SealedVaultMenu>> SEALED_VAULT =
            MENUS.register("sealed_vault",
                    () -> IMenuTypeExtension.create(SealedVaultMenu::new));

    /** 元素小包界面。 */
    public static final DeferredHolder<MenuType<?>, MenuType<PouchMenu>> POUCH =
            MENUS.register("pouch", () -> IMenuTypeExtension.create(PouchMenu::new));
    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private EVMenus() {
    }
}
