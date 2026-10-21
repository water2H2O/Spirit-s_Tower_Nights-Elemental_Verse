package com.water2h2o.elementalverse.entity;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.ElementAxis;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.reaction.PendingCasts;
import com.water2h2o.elementalverse.reaction.SpellCaster;
import com.water2h2o.elementalverse.registry.EVEntities;
import com.water2h2o.elementalverse.registry.EVItems;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * 法术弹射物：把一条申明"扔出去"。
 *
 * <h2>为什么做成实体</h2>
 * 以前施法是**射线**：眼睛看向哪，法术就在哪生效 —— 玩家只能看到目标处闪一下，
 * 完全没有"法术从我手上飞出去"的感觉。现在法术是一个真的会飞的物体：
 * <ul>
 *   <li>飞行途中每 tick 拖一条**与元素同色**的粒子尾巴</li>
 *   <li>命中方块或实体时，才结算现象（伤害、点燃、效果……）</li>
 *   <li>手上看的是一瓶会飞的元素，看得出这发是什么</li>
 * </ul>
 *
 * <p>只有服务端结算；客户端只管画。
 */
public class SpellProjectile extends ThrowableItemProjectile {

    /** 这条弹射物携带的申明。客户端不参与结算，所以只在存档里保留。 */
    private Declaration declaration = Declaration.EMPTY;

    /** 是否追踪最近的生物（水元素的正向参数）。 */
    private boolean homing;

    /**
     * 是不是「三级附加」生成的风弹。
     *
     * <p>附加弹命中时**不能再触发一次三级附加** —— 否则同一发法术会在每次命中时
     * 再分裂 2 发，实体数按 2^n 增长，几秒内就能把服务器卡死。
     */
    private boolean bonusProjectile;

    /** 客户端要用来选贴图和粒子颜色，所以必须同步。 */
    private static final net.minecraft.network.syncher.EntityDataAccessor<String> DATA_ELEMENT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(SpellProjectile.class,
                    net.minecraft.network.syncher.EntityDataSerializers.STRING);

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ELEMENT, "");
    }

    /** 这条弹射物是什么元素（空 = 未知）。 */
    public String elementId() {
        return this.entityData.get(DATA_ELEMENT);
    }

    public SpellProjectile(Level level, LivingEntity shooter, Declaration declaration) {
        this(level, shooter, declaration, false);
    }

    public SpellProjectile(Level level, LivingEntity shooter, Declaration declaration,
                           boolean bonusProjectile) {
        super(EVEntities.SPELL_PROJECTILE.get(), shooter, level);
        this.declaration = declaration;
        this.bonusProjectile = bonusProjectile;
        ElementType element = declaration.element();
        this.entityData.set(DATA_ELEMENT, element == null ? "" : element.getSerializedName());
        // 不再渲染物品：外观交给 ElementProjectileRenderer 画元素本体
        this.setItem(ItemStack.EMPTY);
    }

    public boolean isBonusProjectile() {
        return bonusProjectile;
    }

    /** 飞出去的那个东西：对应元素的瓶子，一眼能看出这发是什么。 */
    private static ItemStack iconFor(Declaration declaration) {
        ElementType element = declaration.element();
        if (element == null || element.isEmpty()) {
            return new ItemStack(EVItems.ELEMENT_VIAL.get());
        }
        // 作者：先改回瓶子，等新素材到了一起换
        return ElementVialItem.createStack(element);
    }

    @Override
    protected Item getDefaultItem() {
        return EVItems.ELEMENT_VIAL.get();
    }

    public void setHoming(boolean homing) {
        this.homing = homing;
    }

    /** 追踪：每 tick 把速度往最近的生物身上掰一点。 */
    private void steer() {
        var target = level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                        getBoundingBox().inflate(12.0D),
                        e -> e != getOwner() && e.isAlive())
                .stream()
                .min(java.util.Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return;
        }
        var aim = target.getBoundingBox().getCenter().subtract(position());
        if (aim.lengthSqr() < 1.0E-4D) {
            return;
        }
        var blended = getDeltaMovement().scale(0.55D).add(aim.normalize().scale(0.45D));
        if (blended.lengthSqr() < 1.0E-6D) {
            return;
        }
        setDeltaMovement(blended.normalize().scale(0.55D));
        hurtMarked = true;
    }

    /** 拖尾：与元素同色的粒子，让人看见它飞过去。 */
    @Override
    public void tick() {
        super.tick();
        // 服务端：追踪 + 超时回收
        if (!level().isClientSide()) {
            if (tickCount > 100) {
                discard();
                return;
            }
            if (homing) {
                steer();
            }
            return;
        }
        ElementAxis axis = axis();
        if (axis == null) {
            return;
        }
        int color = SpellCaster.colorOf(axis);
        DustParticleOptions dust = new DustParticleOptions(
                new org.joml.Vector3f(((color >> 16) & 0xFF) / 255.0F,
                        ((color >> 8) & 0xFF) / 255.0F,
                        (color & 0xFF) / 255.0F), 0.9F);
        for (int i = 0; i < 2; i++) {
            level().addParticle(dust, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    private ElementAxis axis() {
        ElementType element = com.water2h2o.elementalverse.element.ElementType.byName(elementId());
        if (element == null || element.isEmpty()) {
            element = ElementVialItem.getElement(getItem());
        }
        if (element == null || element.isEmpty()) {
            element = declaration.element();
        }
        if (element == null || element.isEmpty()) {
            return null;
        }
        return element.value().dominant();
    }

    /** 命中才结算 —— 这是"飞出去"与"贴脸生效"的分界线。 */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide() || result.getType() == HitResult.Type.MISS) {
            return;
        }
        if (getOwner() instanceof Player player) {
            boolean allowBonus = !bonusProjectile;
            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.FUSE)
                    && level() instanceof ServerLevel server) {
                // 火药：先落地，1.5 秒后才结算
                PendingCasts.runLater(server,
                        com.water2h2o.elementalverse.element.Modifiers.FUSE_TICKS,
                        () -> SpellCaster.applyProjectileHit(level(), player, declaration, result,
                                allowBonus));
            } else {
                // 附加风弹命中时不再触发三级附加，避免指数级分裂。
                SpellCaster.applyProjectileHit(level(), player, declaration, result, allowBonus);
            }
        }
        discard();
    }

    // ---------------------------------------------------------------- 存档

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Homing", homing);
        tag.putBoolean("BonusProjectile", bonusProjectile);
        Declaration.CODEC.encodeStart(NbtOps.INSTANCE, declaration)
                .result()
                .ifPresent(encoded -> tag.put("Declaration", encoded));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        homing = tag.getBoolean("Homing");
        bonusProjectile = tag.getBoolean("BonusProjectile");
        Declaration.CODEC.parse(NbtOps.INSTANCE, tag.get("Declaration"))
                .result()
                .ifPresent(parsed -> declaration = parsed);
    }
}
