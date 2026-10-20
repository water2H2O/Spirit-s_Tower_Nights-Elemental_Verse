package com.water2h2o.elementalverse.reaction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.water2h2o.elementalverse.element.Declaration;
import com.water2h2o.elementalverse.element.ElementAxis;
import com.water2h2o.elementalverse.element.ElementCarrier;
import com.water2h2o.elementalverse.element.ElementType;
import com.water2h2o.elementalverse.element.ElementValue;
import com.water2h2o.elementalverse.element.ElementVialItem;
import com.water2h2o.elementalverse.util.EVLang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 直接释放：拿着写好的申明板右键，把申明变成现象。
 *
 * <h2>流程</h2>
 * <ol>
 *   <li>看板子上写了什么（料 + 档位）</li>
 *   <li>按档位从背包扣对应数量的元素容器 —— 不够就不放</li>
 *   <li>拿"料"的元素值去反应表里匹配（先精确配方，再主成分兜底）</li>
 *   <li>执行现象</li>
 * </ol>
 *
 * <p>作者的要求：**底层复杂，玩家感受简单**。所以扣材料时只说"元素不够"，
 * 不报元素值；命中时只看得到火/水/伤害，看不到任何数字。
 */
public final class SpellCaster {

    private static final double RANGE = 6.0D;

    private SpellCaster() {
    }

    /**
     * @return 真的放出来了返回 true
     */
    public static boolean cast(Level level, Player player, ItemStack plate) {
        Declaration declaration = com.water2h2o.elementalverse.element.DeclarationPlateItem
                .getDeclaration(plate);
        if (!declaration.isDefined()) {
            return false;
        }

        // 客户端没有反应表，只做“材料够不够”的预测；真正的匹配在服务端。
        if (level.isClientSide()) {
            return checkMaterials(level, player, declaration);
        }

        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        Optional<Reaction> matched = ReactionManager.match(input);

        // 没有精确配方、也没有主成分兜底 → 不扣材料，直接提示。
        if (matched.isEmpty() && !hasFallback(input)) {
            player.displayClientMessage(Component.translatable(EVLang.MSG_NO_REACTION), true);
            return false;
        }

        if (!checkMaterials(level, player, declaration)) {
            return false;
        }
        consumeMaterials(player, declaration);

        boolean flying = false;
        if (matched.isPresent()) {
            flying = apply(level, player, matched.get().phenomenon(), params, declaration, null);
        } else {
            applyFallback(level, player, input, params);
        }

        // 表现：飞出去的法术自带拖尾，就不用再画一条连线了
        if (flying) {
            playLaunchSound(level, player, input);
        } else {
            playCastFeedback(level, player, input, null);
        }
        return true;
    }

    /**
     * 由**武器**触发：目标已经确定（打中的那个实体），不再做射线。
     *
     * @return 真的放出来了返回 true
     */
    public static boolean castAtEntity(Level level, Player player,
                                       com.water2h2o.elementalverse.element.Declaration declaration,
                                       Entity target) {
        if (!declaration.isDefined()) {
            return false;
        }

        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        var hit = new EntityHitResult(target);
        var matched = ReactionManager.match(input);

        // 武器触发没有“主成分兜底”：凑不出反应就不扣材料、也不触发。
        if (matched.isEmpty()) {
            player.displayClientMessage(Component.translatable(EVLang.MSG_NO_REACTION), true);
            return false;
        }
        if (!checkMaterials(level, player, declaration)) {
            return false;
        }

        consumeMaterials(player, declaration);
        // 武器触发一定是"已经打中了"，所以不走弹射物形态，直接结算。
        dispatch(level, player, matched.get().phenomenon(), params, hit, declaration);
        playCastFeedback(level, player, input, hit);
        return true;
    }

    // ---------------------------------------------------------------- 现象

    /**
     * 按形态分派。
     *
     * <ul>
     *   <li>{@code self}：直接落在施法者身上</li>
     *   <li>{@code projectile}：**发射**，法术变成一颗会飞的弹射物，命中才结算</li>
     *   <li>其它（{@code touch} 等老写法）：沿用射线，看向哪就在哪生效</li>
     * </ul>
     *
     * @return 真的发射了弹射物返回 true（调用方据此跳过连线反馈）
     */
    private static boolean apply(Level level, Player player, Phenomenon phenomenon, SpellParams params,
                                 Declaration declaration, HitResult providedHit) {
        String form = declaration.form();
        // 自生：现象直接落在施法者自己身上（不射线）
        if ("self".equals(form)) {
            HitResult selfHit = new EntityHitResult(player);
            dispatch(level, player, phenomenon, params, selfHit, declaration);
            return false;
        }
        // 发射：飞出去，命中时由 SpellProjectile 回调 applyProjectileHit
        //
        // 老存档里的板子写的是 touch，也让它飞 —— 否则同一个法术，新板子飞、旧板子不飞，
        // 玩家会以为"时灵时不灵"。所以除"自身"以外，一律飞。
        if (providedHit == null) {
            return launchProjectile(level, player, declaration);
        }
        // 触摸：射线命中
        dispatch(level, player, phenomenon, params, providedHit != null
                ? providedHit : player.pick(RANGE, 1.0F, false), declaration);
        return false;
    }

    /**
     * 发射一颗法术弹射物。
     *
     * <p>速度给 1.6：比箭略慢，让人看得清它飞过去 —— 这正是这次要的效果。
     */
    private static boolean launchProjectile(Level level, Player player, Declaration declaration) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        var projectile = new com.water2h2o.elementalverse.entity.SpellProjectile(
                server, player, declaration);
        projectile.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
        // 不受重力：法术应该笔直飞向准星指向的地方，而不是像雪球那样掉下去
        projectile.setNoGravity(true);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.4F);
        server.addFreshEntity(projectile);
        return true;
    }

    /**
     * 弹射物命中时结算现象。
     *
     * <p>只有服务端调用；客户端从 {@code SpellProjectile} 里就被拦下了。
     */
    public static void applyProjectileHit(Level level, Player player, Declaration declaration,
                                          HitResult hit) {
        if (level.isClientSide()) {
            return;
        }
        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        var matched = ReactionManager.match(input);
        if (matched.isPresent()) {
            dispatch(level, player, matched.get().phenomenon(), params, hit, declaration);
        } else {
            applyFallback(level, player, input, params, hit);
        }
        playImpactFeedback(level, input, hit);
    }

    private static void dispatch(Level level, Player player, Phenomenon phenomenon, SpellParams params,
                                 HitResult hit, Declaration declaration) {

        switch (phenomenon.type()) {
            case "ignite" -> ignite(level, hit, phenomenon, params);
            case "douse" -> douse(level, hit, params);
            case "sustained_element_damage" -> strike(level, player, phenomenon, params, hit);
            case "steam" -> steam(level, player, hit, params);
            case "lava" -> lava(level, hit, phenomenon, params);
            case "fire_resistance" -> fireResistance(player, phenomenon, params);
            case "chaos" -> chaos(level, player, hit, phenomenon, params);
            default -> applyPrimitive(level, player, hit, phenomenon, params, declaration);
        }
    }

    /**
     * 洪流：把目标推开，并让它的物品栏短暂"锁住"（不能使用物品、不能丢弃）。
     *
     * <p>打在方块上时只溅水花 —— 不破坏地形。
     */
    private static void lockInventory(Level level, Player player, Phenomenon phenomenon,
                                      SpellParams params, HitResult hit, Declaration declaration) {
        // 持续时间只看洪流的**档位**：1/2/3 档 = 3 / 5 / 10 秒。
        // 不看光态，也不看 JSON 里的 duration —— 这条法术的"多久"由投入量决定。
        int seconds = switch (Math.max(1, Math.min(3, declaration.tier()))) {
            case 3 -> 10;
            case 2 -> 5;
            default -> 3;
        };
        int ticks = seconds * 20;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            // 中等击退：方向由施法者指向目标（以前只对玩家生效，打怪完全没反应）
            var dir = living.position().subtract(player.position());
            if (dir.lengthSqr() > 1.0E-4D) {
                dir = dir.normalize();
                living.push(dir.x * 0.9D * params.intensity(), 0.35D,
                        dir.z * 0.9D * params.intensity());
                living.hurtMarked = true;
            }
            // 先移除再挂 = **重置计时**。直接 addEffect 只会取剩余时间较长的那个，
            // 连打第二发时不但不刷新，还可能完全不生效。
            var boundHands = com.water2h2o.elementalverse.registry.EVMobEffects.BOUND_HANDS;
            living.removeEffect(boundHands);
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(boundHands, ticks, 0),
                    player);
            // 对玩家再额外压一层物品冷却，手感上就是"掏不出东西"
            if (living instanceof Player target) {
                for (int i = 0; i < target.getInventory().getContainerSize(); i++) {
                    ItemStack stack = target.getInventory().getItem(i);
                    if (!stack.isEmpty()) {
                        target.getCooldowns().addCooldown(stack.getItem(), ticks);
                    }
                }
            }
            return;
        }
        if (hit instanceof BlockHitResult blockHit && level instanceof ServerLevel server) {
            var center = blockHit.getLocation();
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                    center.x, center.y + 0.2D, center.z, 24, 0.4D, 0.2D, 0.4D, 0.1D);
        }
    }

    /**
     * 现象持续时间。
     *
     * <p>JSON 的 {@code duration} 是基准（tick）；第三个投入口如果是光系，
     * 会按 2 / 5 / 12 秒的比例把它拉长。这样“写进 JSON 的持续时间”终于真的生效了。
     */
    private static int effectTicks(Phenomenon phenomenon, SpellParams params) {
        int base = Math.max(1, phenomenon.duration());
        double factor = Math.max(1, params.durationSec())
                / (double) Math.max(1, SpellParams.DEFAULT.durationSec());
        return Math.max(1, (int) Math.round(base * factor));
    }

    /** 点燃：命中的实体着火，或者命中方块的面上一格起火。 */
    private static void ignite(Level level, HitResult hit, Phenomenon phenomenon, SpellParams params) {
        int fireTicks = (int) Math.round(effectTicks(phenomenon, params) * params.intensity());

        if (hit instanceof EntityHitResult entityHit) {
            entityHit.getEntity().setRemainingFireTicks(fireTicks);
            return;
        }
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos center = blockHit.getBlockPos().relative(blockHit.getDirection());
            int radius = (int) Math.round(params.radius());
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                    center.offset(radius, 1, radius))) {
                BlockPos spot = pos.immutable();
                if (level.getBlockState(spot).isAir()
                        && center.distSqr(spot) <= (double) radius * radius + 0.5D) {
                    level.setBlockAndUpdate(spot, BaseFireBlock.getState(level, spot));
                }
            }
        }
    }

    /**
     * 通用原语：现象类型直接写原语名。
     *
     * <p>这样加一条反应基本只需要写 JSON —— 只有"全新机制"才需要在这里加分支。
     */
    private static void applyPrimitive(Level level, Player player, HitResult hit,
                                       Phenomenon phenomenon, SpellParams params,
                                       Declaration declaration) {
        String type = phenomenon.type();
        BlockPos center;
        if (hit instanceof BlockHitResult blockHit) {
            center = blockHit.getBlockPos().relative(blockHit.getDirection());
        } else if (hit instanceof EntityHitResult entityHit) {
            center = entityHit.getEntity().blockPosition();
        } else {
            center = player.blockPosition();
        }
        int radius = Math.max(1, (int) Math.round(params.radius()));
        int ticks = Math.max(20, effectTicks(phenomenon, params));

        switch (type) {
            case "area_fire" -> {
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                        center.offset(radius, 1, radius))) {
                    BlockPos spot = pos.immutable();
                    if (level.getBlockState(spot).isAir()
                            && center.distSqr(spot) <= (double) radius * radius + 0.5D) {
                        level.setBlockAndUpdate(spot, BaseFireBlock.getState(level, spot));
                    }
                }
            }
            case "smolder" -> {
                // 阴燃：不着明火，但目标持续受伤
                if (hit instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    living.hurt(level.damageSources().magic(),
                            (float) (phenomenon.amount() * params.intensity()));
                    living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WITHER,
                            (int) Math.round(ticks * 0.5D), 0));
                }
            }
            case "place_stone" -> fillSphere(level, center, radius, Blocks.STONE.defaultBlockState());
            case "place_grass" -> fillSphere(level, center, radius, Blocks.GRASS_BLOCK.defaultBlockState());
            case "grow" -> {
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                        center.offset(radius, 1, radius))) {
                    BlockPos spot = pos.immutable();
                    if (level.getBlockState(spot).isAir()
                            && level.getBlockState(spot.below()).is(Blocks.GRASS_BLOCK)) {
                        level.setBlockAndUpdate(spot, Blocks.SHORT_GRASS.defaultBlockState());
                    }
                }
            }
            case "push" -> {
                if (level instanceof ServerLevel server) {
                    for (Entity entity : server.getEntitiesOfClass(Entity.class,
                            new net.minecraft.world.phys.AABB(center).inflate(radius))) {
                        if (entity == player) {
                            continue;
                        }
                        var dir = entity.position().subtract(player.position()).normalize();
                        entity.push(dir.x * params.intensity(), 0.35D, dir.z * params.intensity());
                    }
                }
            }
            case "blink" -> {
                var look = player.getViewVector(1.0F).normalize()
                        .scale(4.0D + params.radius());
                player.teleportTo(player.getX() + look.x, player.getY(), player.getZ() + look.z);
                player.resetFallDistance();
            }
            case "cleanse" -> {
                player.getActiveEffects().stream()
                        .filter(effect -> !effect.getEffect().value().isBeneficial())
                        .map(effect -> effect.getEffect())
                        .toList()
                        .forEach(player::removeEffect);
                player.heal((float) (2.0D * params.intensity()));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, ticks, 0));
            }
            case "lock_inventory" -> lockInventory(level, player, phenomenon, params, hit, declaration);
            case "freeze" -> {
                if (hit instanceof EntityHitResult entityHit
                        && entityHit.getEntity() instanceof LivingEntity living) {
                    living.setTicksFrozen(living.getTicksRequiredToFreeze() + ticks);
                    living.hurt(level.damageSources().freeze(), (float) params.intensity());
                }
            }
            case "annihilate" -> {
                // 光 + 暗：正负抵消，什么都不发生，但很脏（余热以后接）
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                        SoundSource.PLAYERS, 0.8F, 0.6F);
            }
            case "extend_effects" -> {
                player.getActiveEffects().stream().toList().forEach(effect ->
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                effect.getEffect(),
                                effect.getDuration() * 2,
                                Math.min(3, effect.getAmplifier() + 1))));
            }
            default -> applyNamedEffect(level, player, hit, phenomenon, params);
        }
    }

    private static void fillSphere(Level level, BlockPos center, int radius,
                                   net.minecraft.world.level.block.state.BlockState state) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockPos spot = pos.immutable();
            if (center.distSqr(spot) <= (double) radius * radius + 0.5D
                    && (level.getBlockState(spot).isAir() || level.getBlockState(spot).canBeReplaced())) {
                level.setBlockAndUpdate(spot, state);
            }
        }
    }

    /**
     * 施法反馈。
     *
     * <p>作者反馈："很多时候看不出来有没有施法"。所以这里做三件事：
     * <ol>
     *   <li>沿"玩家 → 目标"的连线撒一条粒子，让人看见法术飞出去</li>
     *   <li>颜色按主成分轴区分（火橙、水蓝、草绿…）</li>
     *   <li>在目标处播放与元素匹配的声音（不是每级都同一个音）</li>
     * </ol>
     */
    private static void playCastFeedback(Level level, Player player, ElementValue input,
                                        HitResult providedHit) {
        var axis = input.dominant();
        int color = colorOf(axis);

        var hit = providedHit != null ? providedHit : player.pick(RANGE, 1.0F, false);
        var target = hit.getType() == HitResult.Type.MISS
                ? player.getEyePosition().add(player.getViewVector(1.0F).scale(RANGE))
                : hit.getLocation();

        if (!(level instanceof ServerLevel server)) {
            return;
        }

        // 连线粒子：从手到目标
        var start = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.6D));
        for (int step = 0; step <= 12; step++) {
            double t = step / 12.0D;
            double x = start.x + (target.x - start.x) * t;
            double y = start.y + (target.y - start.y) * t;
            double z = start.z + (target.z - start.z) * t;
            server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(((color >> 16) & 0xFF) / 255.0F,
                                    ((color >> 8) & 0xFF) / 255.0F,
                                    (color & 0xFF) / 255.0F), 1.2F),
                    x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        // 目标处的爆发
        server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(((color >> 16) & 0xFF) / 255.0F,
                                ((color >> 8) & 0xFF) / 255.0F,
                                (color & 0xFF) / 255.0F), 1.6F),
                target.x, target.y, target.z, 18, 0.35D, 0.35D, 0.35D, 0.02D);

        // 元素对应的声音
        var sound = switch (axis == null ? ElementAxis.FIRE : axis) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.BUCKET_EMPTY;
            case GRASS -> SoundEvents.GRASS_BREAK;
            case EARTH -> SoundEvents.STONE_PLACE;
            case WIND -> SoundEvents.PHANTOM_FLAP;
            case LIGHT -> SoundEvents.AMETHYST_BLOCK_CHIME;
        };
        level.playSound(null, target.x, target.y, target.z, sound, SoundSource.PLAYERS, 0.8F, 1.1F);
        // 再补一声"施法"底噪，保证任何时候都有明确反馈
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.45F, 1.4F);
    }

    /** 元素轴对应的颜色：火橙、水蓝、草绿、地褐、风白、光金。 */
    public static int colorOf(ElementAxis axis) {
        return switch (axis == null ? ElementAxis.FIRE : axis) {
            case FIRE -> 0xFF6A00;
            case WATER -> 0x3FA9F5;
            case GRASS -> 0x4CD137;
            case EARTH -> 0x9B7653;
            case WIND -> 0xD9F0FF;
            case LIGHT -> 0xFFE066;
        };
    }

    /** 发射音：比命中音短促，让人知道"出去了"。 */
    private static void playLaunchSound(Level level, Player player, ElementValue input) {
        ElementAxis axis = input.dominant();
        var sound = switch (axis == null ? ElementAxis.FIRE : axis) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.BOAT_PADDLE_WATER;
            case GRASS -> SoundEvents.GRASS_BREAK;
            case EARTH -> SoundEvents.STONE_PLACE;
            case WIND -> SoundEvents.PHANTOM_FLAP;
            case LIGHT -> SoundEvents.AMETHYST_BLOCK_CHIME;
        };
        level.playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.55F, 1.5F);
    }

    /** 命中反馈：目标处爆一圈同色粒子 + 元素命中音。 */
    private static void playImpactFeedback(Level level, ElementValue input, HitResult hit) {
        if (!(level instanceof ServerLevel server) || hit == null
                || hit.getType() == HitResult.Type.MISS) {
            return;
        }
        ElementAxis axis = input.dominant();
        int color = colorOf(axis);
        var target = hit.getLocation();
        server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(((color >> 16) & 0xFF) / 255.0F,
                                ((color >> 8) & 0xFF) / 255.0F,
                                (color & 0xFF) / 255.0F), 1.6F),
                target.x, target.y, target.z, 18, 0.35D, 0.35D, 0.35D, 0.02D);
        var sound = switch (axis == null ? ElementAxis.FIRE : axis) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case WATER -> SoundEvents.BUCKET_EMPTY;
            case GRASS -> SoundEvents.GRASS_BREAK;
            case EARTH -> SoundEvents.STONE_PLACE;
            case WIND -> SoundEvents.PHANTOM_FLAP;
            case LIGHT -> SoundEvents.AMETHYST_BLOCK_CHIME;
        };
        level.playSound(null, target.x, target.y, target.z, sound, SoundSource.PLAYERS, 0.8F, 1.1F);
    }

    /**
     * 通用状态：现象类型写成 {@code effect:<原版效果id>}。
     *
     * <p>这样加一条"抗性提升"只要写 JSON，不用改 Java。
     */
    private static void applyNamedEffect(Level level, Player player, HitResult hit,
                                         Phenomenon phenomenon, SpellParams params) {
        String type = phenomenon.type();
        if (!type.startsWith("effect:")) {
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.5F, 1.6F);
            return;
        }
        var id = net.minecraft.resources.ResourceLocation.tryParse(type.substring("effect:".length()));
        if (id == null) {
            return;
        }
        var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id);
        if (holder.isEmpty()) {
            return;
        }
        int ticks = Math.max(20, effectTicks(phenomenon, params));

        // 增益（抗性、再生…）默认给自己；减益（强光、凋零、反胃…）只打命中的实体。
        // 减益打空（射线只命中方块）时不落到自己头上——那等于自己吃了自己的法术。
        LivingEntity target;
        if (holder.get().value().isBeneficial()) {
            target = player;
        } else if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            target = living;
        } else {
            return;
        }
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder.get(), ticks, 0));
    }

    /** 暗渊：随机给几个负面状态 + 固定黑暗（打在命中的实体上；没命中才落在施法者身上）。 */
    private static void chaos(Level level, Player player, HitResult hit, Phenomenon phenomenon,
                              SpellParams params) {
        var pool = java.util.List.of(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                net.minecraft.world.effect.MobEffects.WEAKNESS,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
                net.minecraft.world.effect.MobEffects.CONFUSION,
                net.minecraft.world.effect.MobEffects.POISON);
        var random = level.getRandom();
        var picked = new java.util.ArrayList<>(pool);
        java.util.Collections.shuffle(picked, new java.util.Random(random.nextLong()));
        int count = Math.min(5, picked.size());
        int ticks = Math.max(20, effectTicks(phenomenon, params));
        LivingEntity target = player;
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            target = living;
        }
        for (int i = 0; i < count; i++) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(picked.get(i), ticks, 0));
        }
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, ticks, 0));
    }

    /** 蒸汽：小而烫的伤害 + 击退。 */
    private static void steam(Level level, Player player, HitResult hit, SpellParams params) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                living.hurt(level.damageSources().magic(), (float) (2.0D * params.intensity()));
                var away = living.position().subtract(player.position());
                if (away.lengthSqr() < 1.0E-4D) {
                    away = living.getLookAngle();
                }
                living.knockback(0.6D, -away.x, -away.z);
            }
        }
    }

    /** 熔岩：在命中面放岩浆，并给附近的实体减速。 */
    private static void lava(Level level, HitResult hit, Phenomenon phenomenon, SpellParams params) {
        int ticks = Math.max(20, effectTicks(phenomenon, params));
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos place = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (level.getBlockState(place).isAir()) {
                level.setBlockAndUpdate(place, Blocks.LAVA.defaultBlockState());
            }
            return;
        }
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity living) {
            living.setRemainingFireTicks(ticks);
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 0));
        }
    }

    /** 火焰保护：给自己一个抗火。 */
    private static void fireResistance(Player player, Phenomenon phenomenon, SpellParams params) {
        int ticks = Math.max(20, effectTicks(phenomenon, params));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, ticks, 0));
    }

    /** 灭火：把命中点附近的火清掉。 */
    private static void douse(Level level, HitResult hit, SpellParams params) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            living.clearFire();
            return;
        }
        if (hit instanceof BlockHitResult blockHit) {
            BlockPos center = blockHit.getBlockPos();
            int r = Math.max(1, (int) Math.round(params.radius()));
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
                if (level.getBlockState(pos).getBlock() == Blocks.FIRE) {
                    level.removeBlock(pos.immutable(), false);
                }
            }
        }
    }

    /**
     * 元素伤害。
     *
     * <p>本期简化为**一次结算**（duration 用来说明"持续感"，还没有逐 tick 结算）。
     * 逐段结算等有了持续型法术框架再补 —— 现在先把闭环跑通。
     */
    private static void strike(Level level, Player player, Phenomenon phenomenon, SpellParams params,
                               HitResult suppliedHit) {
        HitResult hit = suppliedHit != null && suppliedHit.getType() != HitResult.Type.MISS
                ? suppliedHit
                : player.pick(RANGE, 1.0F, false);
        int ticks = Math.max(20, effectTicks(phenomenon, params));
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target instanceof LivingEntity living) {
                living.hurt(level.damageSources().magic(),
                        (float) (phenomenon.amount() * params.intensity()));
                living.setRemainingFireTicks(ticks);
            }
        } else if (hit instanceof BlockHitResult blockHit) {
            BlockPos place = blockHit.getBlockPos().relative(blockHit.getDirection());
            if (level.getBlockState(place).isAir()) {
                level.setBlockAndUpdate(place, BaseFireBlock.getState(level, place));
            }
        }
    }

    /** 没有精确配方 → 按主成分律给一个通用现象（保证"扔一堆火"总有反应）。 */
    private static void applyFallback(Level level, Player player, ElementValue input, SpellParams params) {
        applyFallback(level, player, input, params, null);
    }

    /** 同上，但命中点已经确定（弹射物飞来时用这个，不再自己射线）。 */
    private static void applyFallback(Level level, Player player, ElementValue input, SpellParams params,
                                      HitResult suppliedHit) {
        var axis = input.dominant();
        if (axis == null) {
            return;
        }
        HitResult target = suppliedHit != null && suppliedHit.getType() != HitResult.Type.MISS
                ? suppliedHit
                : player.pick(RANGE, 1.0F, false);
        switch (axis) {
            case FIRE -> ignite(level, target, new Phenomenon("ignite", axis, 1.0, 40, 0.0), params);
            case WATER -> douse(level, target, params);
            default -> {
                // 目前只为火 / 水实现了通用兜底；其余元素没有现象，调用方会在扣材料前拦下。
            }
        }
    }

    /** 没有精确配方时，主成分是否还有可执行的通用现象。 */
    private static boolean hasFallback(ElementValue input) {
        ElementAxis axis = input.dominant();
        return axis == ElementAxis.FIRE || axis == ElementAxis.WATER;
    }

    // ---------------------------------------------------------------- 材料

    /**
     * 施法的材料判定：**认轴、算值、小瓶优先**。
     *
     * <p>以前是按"瓶子里的元素种类"精确匹配（洪流只收 water），于是霜和冰明明主成分都是水却用不了。
     * 现在改成看**元素值**：
     * <ul>
     *   <li>轴：法术主料的主成分轴（洪流 = 水轴）</li>
     *   <li>值：水 1.0 / 霜 1.0 / 冰 1.5 —— 冰一瓶顶一瓶半水</li>
     *   <li>只认主成分同轴的载体，砂（主成分地）不能给水法术供能</li>
     * </ul>
     *
     * <p>成本按档位 1 / 2 / 3 **点**（写入仍是 1/3/9 瓶，两码事）。
     */

    /** 一次施法需要多少点元素值。 */
    private static double castCost(Declaration declaration) {
        return Math.max(1, declaration.castCount());
    }

    /** 这条申明认的轴。 */
    private static ElementAxis castAxis(Declaration declaration) {
        return declaration.element().value().dominant();
    }

    /** 一个载体能提供多少点"该轴"的值；主成分不同轴或为负则不供能。 */
    private static double pointsOf(ItemStack stack, ElementAxis axis) {
        if (stack.isEmpty() || !ElementCarrier.isCarrier(stack)) {
            return 0.0D;
        }
        ElementType type = ElementVialItem.getElement(stack);
        if (type == null || type.isEmpty()) {
            return 0.0D;
        }
        ElementValue value = type.value();
        if (value.dominant() != axis) {
            return 0.0D;
        }
        double amount = value.get(axis);
        return amount > 0.0D ? amount : 0.0D;
    }

    /** 一个可供能的位置：inner >= 0 表示在小包的第 inner 格。 */
    private record Vial(int slot, int inner, double unit) {
    }

    /** 收集玩家身上所有能供这条轴的元素（含小包存放格，不含燃料格）。 */
    private static List<Vial> vialsFor(Player player, ElementAxis axis) {
        List<Vial> list = new ArrayList<>();
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(com.water2h2o.elementalverse.registry.EVItems.ELEMENT_POUCH.get())) {
                var inside = com.water2h2o.elementalverse.element.PouchContents.get(stack);
                for (int i = 0; i < com.water2h2o.elementalverse.element.PouchContents.STORAGE; i++) {
                    double unit = pointsOf(inside.get(i), axis);
                    if (unit > 0.0D) {
                        list.add(new Vial(slot, i, unit));
                    }
                }
                continue;
            }
            double unit = pointsOf(stack, axis);
            if (unit > 0.0D) {
                list.add(new Vial(slot, -1, unit));
            }
        }
        return list;
    }

    private static int countAt(Player player, Vial vial) {
        if (vial.inner() < 0) {
            return player.getInventory().getItem(vial.slot()).getCount();
        }
        ItemStack pouch = player.getInventory().getItem(vial.slot());
        return com.water2h2o.elementalverse.element.PouchContents.get(pouch).get(vial.inner()).getCount();
    }

    /** 身上这条轴一共有多少元素值。 */
    private static double availableValue(Player player, ElementAxis axis) {
        double total = 0.0D;
        for (Vial vial : vialsFor(player, axis)) {
            total += vial.unit() * countAt(player, vial);
        }
        return total;
    }

    /** 材料够不够；不够时在服务端提示缺哪种元素。 */
    private static boolean checkMaterials(Level level, Player player, Declaration declaration) {
        if (!declaration.isDefined()) {
            return true;
        }
        // 创造小包：不用存元素，想放什么放什么
        if (com.water2h2o.elementalverse.element.PouchItemCreative.carriedBy(player)) {
            return true;
        }
        double need = castCost(declaration);
        double have = availableValue(player, castAxis(declaration));
        if (have + 1.0E-6D < need) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable(
                        EVLang.MSG_NOT_ENOUGH, declaration.element().displayName()), true);
            }
            return false;
        }
        return true;
    }

    /**
     * 按元素值扣材料：**小瓶优先**。
     *
     * <p>需要 1 点时先吃水（1.0），不会拿冰（1.5）去凑。
     * 实在只剩冰，也只能整瓶吃掉 —— 多出的 0.5 点就是"整瓶不可分"的代价。
     */
    private static void consumeMaterials(Player player, Declaration declaration) {
        if (!declaration.isDefined()
                || com.water2h2o.elementalverse.element.PouchItemCreative.carriedBy(player)) {
            return;
        }
        ElementAxis axis = castAxis(declaration);
        double need = castCost(declaration);
        double spent = 0.0D;

        List<Vial> candidates = vialsFor(player, axis);
        candidates.sort(java.util.Comparator.comparingDouble(Vial::unit));

        boolean pouchTouched = false;
        for (Vial vial : candidates) {
            if (spent + 1.0E-6D >= need) {
                break;
            }
            int count = countAt(player, vial);
            while (count > 0 && spent + 1.0E-6D < need) {
                pouchTouched |= takeOne(player, vial);
                spent += vial.unit();
                count--;
            }
        }
        if (pouchTouched) {
            player.getInventory().setChanged();
            if (player.containerMenu != null) {
                player.containerMenu.broadcastChanges();
            }
        }
    }

    /** 消耗一个；返回 true 表示动到的是小包（需要回写组件并同步）。 */
    private static boolean takeOne(Player player, Vial vial) {
        if (vial.inner() < 0) {
            player.getInventory().getItem(vial.slot()).shrink(1);
            return false;
        }
        ItemStack pouch = player.getInventory().getItem(vial.slot());
        var inside = com.water2h2o.elementalverse.element.PouchContents.get(pouch);
        ItemStack item = inside.get(vial.inner());
        if (item.isEmpty()) {
            return false;
        }
        // 写回**新对象**，否则组件里那个对象被原地改动、equality 认为没变
        ItemStack after = item.copy();
        after.setCount(item.getCount() - 1);
        inside.set(vial.inner(), after.getCount() <= 0 ? ItemStack.EMPTY : after);
        com.water2h2o.elementalverse.element.PouchContents.set(pouch, inside);
        return true;
    }

    /** 给"没瞄准任何东西"留个空实现，避免调用方判空。 */
    public static boolean hasTarget(Player player) {
        return player.pick(RANGE, 1.0F, false).getType() != HitResult.Type.MISS;
    }

}
