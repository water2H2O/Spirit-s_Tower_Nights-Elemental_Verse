package com.water2h2o.elementalverse.block;

import com.water2h2o.elementalverse.menu.SealedVaultMenu;
import com.water2h2o.elementalverse.registry.EVBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 石制密封仓的方块实体：9 格容器。
 *
 * <p>它自己**不做**衰变 —— 统一由 {@code ContainerDecayTicker} 扫，
 * 这样"容器里也会坏"只有一处实现。本类只负责声明自己的系数
 * （见 {@link #decayMultiplier()}）。
 */
public class SealedVaultBlockEntity extends BlockEntity implements Container, MenuProvider {

    /** 9 格，与发射器同构（3×3）。 */
    public static final int SIZE = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public SealedVaultBlockEntity(BlockPos pos, BlockState state) {
        super(EVBlockEntities.SEALED_VAULT.get(), pos, state);
    }

    /**
     * 密封仓内的衰变系数：**×0.5**（总纲 2.4）。
     *
     * <p>总纲的口径是"乘算"，所以这里只返回自己的系数，由调用方与其它修正相乘。
     */
    public double decayMultiplier() {
        return 0.5D;
    }

    // ------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    /** 关界面时补一声沉重的关闭声（打开声在 SealedVaultBlock.useWithoutItem 里）。 */
    @Override
    public void stopOpen(Player player) {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.BLOCKS, 0.65F, 0.85F);
        }
    }

    // ------------------------------------------------------------ 存档

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    // ------------------------------------------------------------ Menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.elemental_verse.sealed_vault");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SealedVaultMenu(containerId, playerInventory, this);
    }
}
