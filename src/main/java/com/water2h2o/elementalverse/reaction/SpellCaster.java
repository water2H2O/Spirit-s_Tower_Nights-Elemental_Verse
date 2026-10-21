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

        // 冷却：每条申明各自计时（存在它自己的数据里）
        long now = level.getGameTime();
        if (declaration.readyAt() > now) {
            long left = (declaration.readyAt() - now + 19) / 20;
            player.displayClientMessage(
                    Component.translatable(EVLang.MSG_COOLDOWN, left), true);
            return false;
        }

        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        Optional<Reaction> matched = ReactionManager.match(input, bothSlotsSame(declaration));

        // 没有精确配方、也没有主成分兜底 → 不扣材料，直接提示。
        if (matched.isEmpty() && !hasFallback(input)) {
            player.displayClientMessage(Component.translatable(EVLang.MSG_NO_REACTION), true);
            return false;
        }

        // 没有反应、也没有兜底 → 不扣材料
        if (ReactionManager.match(input, bothSlotsSame(declaration)).isEmpty()
                && !hasFallback(input)) {
            player.displayClientMessage(Component.translatable(EVLang.MSG_NO_REACTION), true);
            return false;
        }
        if (!checkMaterials(level, player, declaration)) {
            return false;
        }
        consumeMaterials(player, declaration);
        // 成功放出才进冷却
        com.water2h2o.elementalverse.element.DeclarationPlateItem.setDeclaration(plate,
                declaration.withReadyAt(now + cooldownTicks(declaration)));

        // 拼贴板：整条链按顺序放（两段、三段、四段都支持）
        if (declaration.hasSecond()) {
            runChain(level, player, declaration, 0);
            // 回响碎片：整条链 2 秒后再放一次
            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.ECHO)
                    && level instanceof ServerLevel server) {
                PendingCasts.runLater(server,
                        com.water2h2o.elementalverse.element.Modifiers.ECHO_TICKS,
                        () -> runChain(level, player, declaration, 0));
            }
            return true;
        }

        return executeNow(level, player, declaration);
    }

    /**
     * 顺序执行整条法术链。
     *
     * <p>链是链表结构（每段自己还能带下一段），所以这里递归走完；
     * 红石粉的延迟作用在**它所在那一段之后**。
     *
     * @param depth 防御性上限，避免异常数据造成死循环
     */
    private static void runChain(Level level, Player player, Declaration declaration, int depth) {
        if (declaration == null || !declaration.isDefined() || depth > 8) {
            return;
        }
        executeNow(level, player, declaration);
        if (!declaration.hasSecond()) {
            return;
        }
        Declaration next = declaration.second();
        if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                com.water2h2o.elementalverse.element.Modifiers.DELAY)
                && level instanceof ServerLevel server) {
            PendingCasts.runLater(server,
                    com.water2h2o.elementalverse.element.Modifiers.DELAY_TICKS,
                    () -> runChain(level, player, next, depth + 1));
        } else {
            runChain(level, player, next, depth + 1);
        }
    }

    /**
     * 修饰材料在「命中之后」的附加效果：荧石粉发光、龙息留下烟雾。
     *
     * <p>以前这两条只写在弹射物命中里，所以剑（接触）和自身形态都不生效。
     * 现在三种形态统一走这里。
     *
     * @param phenomenon 本法术的现象，龙息脉冲会重复施加它；null 表示没有匹配到反应
     */
    private static void applyModifierAftereffects(Level level, Player player, Declaration declaration,
                                                  HitResult hit, SpellParams params,
                                                  Phenomenon phenomenon) {
        if (hit == null || hit.getType() == HitResult.Type.MISS || level.isClientSide()) {
            return;
        }
        String modifiers = declaration.modifier();

        // 荧石粉：命中目标发光
        if (com.water2h2o.elementalverse.element.Modifiers.has(modifiers,
                com.water2h2o.elementalverse.element.Modifiers.GLOW)
                && hit instanceof EntityHitResult glowHit
                && glowHit.getEntity() instanceof LivingEntity glowing) {
            glowing.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, 600, 0));
        }

        // 龙息：留下一滩魔法烟雾，持续对范围内单位施加本法术的效果
        if (com.water2h2o.elementalverse.element.Modifiers.has(modifiers,
                com.water2h2o.elementalverse.element.Modifiers.BREATH)
                && level instanceof ServerLevel server) {
            var at = hit.getLocation();
            var source = player;
            var pulse = phenomenon;
            for (int i = 1; i <= 10; i++) {
                PendingCasts.runLater(server, i * 10, () -> {
                    if (pulse != null) {
                        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class,
                                new net.minecraft.world.phys.AABB(at, at).inflate(2.5D),
                                e -> e != source)) {
                            dispatch(server, source, pulse, params,
                                    new EntityHitResult(living), declaration, false);
                        }
                    }
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                            at.x, at.y + 0.4D, at.z, 8, 1.1D, 0.2D, 1.1D, 0.02D);
                });
            }
        }
    }

    /**
     * 执行一段申明（匹配 → 现象）。材料由调用方负责扣。
     *
     * <p>拼贴板会把两段依次交给这里；延时的那一段由 {@link PendingCasts} 稍后调用。
     */
    public static boolean executeNow(Level level, Player player, Declaration declaration) {
        if (!declaration.isDefined()) {
            return false;
        }
        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        var matched = ReactionManager.match(input, bothSlotsSame(declaration));

        boolean flying = false;
        if (matched.isPresent()) {
            flying = apply(level, player, matched.get().phenomenon(), params, declaration, null);
        } else {
            applyFallback(level, player, input, params);
        }

        // 修饰材料的命中后效果（发光 / 龙息烟雾）—— 三种形态都走这里
        applyModifierAftereffects(level, player, declaration,
                new EntityHitResult(player), params,
                matched.map(com.water2h2o.elementalverse.reaction.Reaction::phenomenon)
                        .orElse(null));

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
        var matched = ReactionManager.match(input, bothSlotsSame(declaration));

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
        dispatch(level, player, matched.get().phenomenon(), params, hit, declaration, true);
        applyModifierAftereffects(level, player, declaration, hit, params,
                matched.get().phenomenon());
        playCastFeedback(level, player, input, hit);
        return true;
    }

    /**
     * 地元素决定"能不能动方块"。
     *
     * <p>作者 2026-10-20：主轴不是地的时候，**正数的地**才允许改方块，**负数的地**一律不碰 ——
     * 所以"火 + 风 + 负地"的爆燃只烧人，不引燃地面。
     */
    private static boolean canTouchBlocks(Declaration declaration) {
        // 注意要把**态**也算进来：木 / 芽 当态时是负地，
        // 只算 inputValue（主料+辅料）会漏掉它，于是"火 3 档 + 木态"照样拆方块。
        double earth = declaration.inputValue().get(ElementAxis.EARTH);
        ElementType state = declaration.stateElement();
        if (state != null && !state.isEmpty() && declaration.stateTier() > 0) {
            earth += state.value().get(ElementAxis.EARTH) * declaration.stateTier();
        }
        return earth >= -ElementValue.EPSILON;
    }

    /** 主轴有且只有地：这时候地法术不是造物，而是**挖掘**。 */
    private static boolean isPureEarth(ElementValue value) {
        for (var entry : value.asMap().entrySet()) {
            if (entry.getKey() != ElementAxis.EARTH
                    && Math.abs(entry.getValue()) > ElementValue.EPSILON) {
                return false;
            }
        }
        return value.get(ElementAxis.EARTH) > ElementValue.EPSILON;
    }

    /**
     * 挖掘：清掉边长 n 的正方体。
     *
     * <p>硬度 ≥ 50（黑曜石）或 &lt; 0（基岩）的方块不动 —— 作者要求"破坏强度黑曜石以下"。
     */
    private static void digCube(Level level, BlockPos center, int n) {
        // 1 级 = 1x1x1，2 级 = 2x2x2，3 级 = 3x3x3。
        // 注意不能再用 (n - 1) / 2：n = 1/2 时它都是 0，会让 1/2 级挖到 0 格。
        int low = Math.max(0, (n - 1) / 2);
        int high = Math.max(low, n / 2);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-low, -low, -low),
                center.offset(high, high, high))) {
            BlockPos spot = pos.immutable();
            var state = level.getBlockState(spot);
            if (state.isAir()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, spot);
            if (hardness < 0.0F || hardness >= 50.0F) {
                continue;
            }
            // 挖掘应该掉方块（否则挖矿/挖箱子等于销毁资源）
            level.destroyBlock(spot, true);
        }
    }

    /** 主料与辅料是不是同一种元素（单元素魔法的书写前提）。 */
    private static boolean bothSlotsSame(Declaration declaration) {
        return declaration.reagentElement() != null
                && !declaration.reagentElement().isEmpty()
                && declaration.reagentElement() == declaration.element();
    }

    /** 档位 1 / 2 / 3。 */
    private static int tierOf(Declaration declaration) {
        return Math.max(1, Math.min(3, declaration.tier()));
    }

    /** 投入向量里有两条以上轴 = 双元素法术（才吃"三级附加"）。 */
    private static boolean isDualInput(Declaration declaration) {
        return declaration.inputValue().asMap().size() > 1;
    }

    /** 给目标挂缚手（统一走抗性规则）。 */
    private static void boundHands(LivingEntity target, Player source, int seconds) {
        var effect = com.water2h2o.elementalverse.registry.EVMobEffects.BOUND_HANDS;
        int ticks = boundHandsTicks(target, seconds);
        // 先移除再挂 = 重置计时，否则只会取剩余时间长的那个
        target.removeEffect(effect);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, ticks, 0), source);
    }

    /** 命中点（打空时用视线前方）。 */
    private static net.minecraft.world.phys.Vec3 hitPoint(Player player, HitResult hit) {
        return hit != null && hit.getType() != HitResult.Type.MISS
                ? hit.getLocation()
                : player.getEyePosition().add(player.getViewVector(1.0F).scale(2.5D));
    }

    /** 命中到的活体（没有就是 null）。 */
    private static LivingEntity hitLiving(HitResult hit) {
        return hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living ? living : null;
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
            dispatch(level, player, phenomenon, params, selfHit, declaration, true);
            return false;
        }
        // 发射：飞出去，命中时由 SpellProjectile 回调 applyProjectileHit
        //
        // 老存档里的板子写的是 touch，也让它飞 —— 否则同一个法术，新板子飞、旧板子不飞，
        // 玩家会以为"时灵时不灵"。所以除"自身"以外，一律飞。
        if (providedHit == null) {
            // 末影珍珠：没有飞行时间，直接命中准星指的地方
            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.INSTANT)) {
                HitResult aim = player.pick(RANGE * 4.0D, 1.0F, false);
                dispatch(level, player, phenomenon, params, aim, declaration, true);
                return false;
            }
            return launchProjectile(level, player, declaration);
        }
        // 触摸：射线命中
        dispatch(level, player, phenomenon, params, providedHit != null
                ? providedHit : player.pick(RANGE, 1.0F, false), declaration, true);
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
        SpellParams params = SpellParams.of(declaration);
        int shots = Math.max(1, Math.min(4, params.shots()));

        for (int i = 0; i < shots; i++) {
            var projectile = new com.water2h2o.elementalverse.entity.SpellProjectile(
                    server, player, declaration);
            projectile.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
            // 不受重力：法术应该笔直飞向准星指向的地方，而不是像雪球那样掉下去
            projectile.setNoGravity(true);
            // 正水：自动追生物；负水：多发散射
            projectile.setHoming(params.homing());

            float spread = shots == 1 ? 0.0F : (i - (shots - 1) / 2.0F) * 9.0F;
            boolean swift = com.water2h2o.elementalverse.element.Modifiers.has(
                    declaration.modifier(), com.water2h2o.elementalverse.element.Modifiers.SWIFT);
            // 糖：速度翻倍、射程更远，代价是准头
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot() + spread,
                    0.0F, swift ? 3.0F : 1.6F,
                    swift ? 5.0F : (shots == 1 ? 0.4F : 2.0F));
            server.addFreshEntity(projectile);
        }
        return true;
    }

    /**
     * 弹射物命中时结算现象。
     *
     * <p>只有服务端调用；客户端从 {@code SpellProjectile} 里就被拦下了。
     */
    public static void applyProjectileHit(Level level, Player player, Declaration declaration,
                                          HitResult hit) {
        applyProjectileHit(level, player, declaration, hit, true);
    }

    /**
     * @param allowTierBonus 是否允许再触发一次三级附加。
     *                       三级附加生成的风弹命中时必须传 false，否则会指数级分裂。
     */
    public static void applyProjectileHit(Level level, Player player, Declaration declaration,
                                          HitResult hit, boolean allowTierBonus) {
        if (level.isClientSide()) {
            return;
        }
        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        var matched = ReactionManager.match(input, bothSlotsSame(declaration));
        if (matched.isPresent()) {
            dispatch(level, player, matched.get().phenomenon(), params, hit, declaration, allowTierBonus);
        } else {
            applyFallback(level, player, input, params, hit);
        }
        applyModifierAftereffects(level, player, declaration, hit, params,
                matched.map(com.water2h2o.elementalverse.reaction.Reaction::phenomenon)
                        .orElse(null));
        playImpactFeedback(level, input, hit);
    }

    private static void dispatch(Level level, Player player, Phenomenon phenomenon, SpellParams params,
                                 HitResult hit, Declaration declaration, boolean allowTierBonus) {

        // 水的附属效果：任何带水的法术都顺带灭火（作者：灭火不再单独成招）
        if (declaration.inputValue().get(ElementAxis.WATER) > 0.0D
                && !"douse".equals(phenomenon.type())) {
            douse(level, hit, params);
        }

        switch (phenomenon.type()) {
            case "ignite" -> ignite(level, hit, phenomenon, params, declaration);
            case "douse" -> douse(level, hit, params);
            case "sustained_element_damage" -> strike(level, player, phenomenon, params, hit);
            case "steam" -> steam(level, player, hit, params);
            case "lava" -> lava(level, hit, phenomenon, params);
            case "fire_resistance" -> fireResistance(player, phenomenon, params);
            case "chaos" -> chaos(level, player, hit, phenomenon, params, declaration);
            case "explode" -> explode(level, player, params, hit, declaration);
            case "flash" -> flash(level, player, hit, phenomenon, params, declaration);
            case "encase" -> encase(level, player, hit, phenomenon, params, declaration);
            default -> applyPrimitive(level, player, hit, phenomenon, params, declaration);
        }

        // 双元素法术的三级附加（主料决定附加什么）。
        // 附加风弹自己命中时 allowTierBonus = false，避免 2^n 分裂。
        if (allowTierBonus) {
            applyTierThreeBonus(level, player, declaration, params, hit, phenomenon);
        }
    }

    /**
     * 洪流：把目标推开，并让它的物品栏短暂"锁住"（不能使用物品、不能丢弃）。
     *
     * <p>打在方块上时只溅水花 —— 不破坏地形。
     */
    private static void lockInventory(Level level, Player player, Phenomenon phenomenon,
                                      SpellParams params, HitResult hit, Declaration declaration) {
        // 持续时间只看洪流的**档位**：1/2/3 档 = 3 / 5 / 8 秒。
        // 不看光态，也不看 JSON 里的 duration —— 这条法术的"多久"由投入量决定。
        int seconds = switch (Math.max(1, Math.min(3, declaration.tier()))) {
            case 3 -> 8;
            case 2 -> 5;
            default -> 3;
        };
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            int ticks = boundHandsTicks(living, seconds);
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

    /** 爆炸：火轴投满（> 2 点）就是把势能一次性放掉。 */
    private static void explode(Level level, Player player, SpellParams params, HitResult hit,
                                Declaration declaration) {
        // 三级火专属：伤害压到 3.0（原本 4.0 会把自己一起炸死）。
        // 拆不拆方块由**地**决定：带负地（木 / 芽）时只炸人不炸地形。
        explodeAt(level, player, hit, 3.0F, canTouchBlocks(declaration));
    }

    private static void explodeAt(Level level, Player player, HitResult hit, float power, boolean destroy) {
        // 打实体时用**身体中心**，不要用命中点 —— 命中点常在脚底，
        // 爆炸粒子会趴在地上（作者反馈"看着不舒服"）
        var at = hit instanceof EntityHitResult entityHit
                ? entityHit.getEntity().getBoundingBox().getCenter()
                : hitPoint(player, hit);
        if (!level.isClientSide()) {
            level.explode(player, at.x, at.y, at.z, power,
                    destroy ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);

        }
    }

    /** 强光：白得看不见，同时发光——在黑里也跑不掉。 */
    private static void flash(Level level, Player player, HitResult hit,
                              Phenomenon phenomenon, SpellParams params, Declaration declaration) {
        // 1 级：失明 6 秒；2 级：失明 + 发光 10 秒；3 级：15 秒 + 缚手 3 秒
        int tier = tierOf(declaration);
        int ticks = (tier == 3 ? 15 : tier == 2 ? 10 : 6) * 20;
        boolean glow = tier >= 2;
        var at = hitPoint(player, hit);

        LivingEntity single = hitLiving(hit);
        if (single != null) {
            blind(single, ticks, glow);
            if (tier == 3) {
                boundHands(single, player, 3);
            }
        } else if (level instanceof ServerLevel server) {
            int radius = Math.max(1, (int) Math.round(params.radius()));
            for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(at, at).inflate(radius + 1.0D))) {
                blind(living, ticks, glow);
            }
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    at.x, at.y, at.z, 50, 0.7D, 0.7D, 0.7D, 0.3D);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                    at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void blind(LivingEntity living, int ticks, boolean glow) {
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.BLINDNESS, ticks, 0));
        if (glow) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.GLOWING, ticks, 0));
        }
    }

    /** 冻结：把目标冻在原地——走不动、挖不动、身上结满冰壳。 */
    private static void encase(Level level, Player player, HitResult hit,
                               Phenomenon phenomenon, SpellParams params, Declaration declaration) {
        // 1/2/3 级 = 3 / 5 / 7 秒；三级额外送 3 秒缚手
        int tier = tierOf(declaration);
        int ticks = (tier == 3 ? 7 : tier == 2 ? 5 : 3) * 20;
        LivingEntity living = hitLiving(hit);
        if (living == null) {
            return;
        }
        if (tier == 3) {
            boundHands(living, player, 3);
        }
        living.setTicksFrozen(living.getTicksRequiredToFreeze() + ticks);
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 5));
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, ticks, 2));
        living.setDeltaMovement(0.0D, 0.0D, 0.0D);
        living.hurtMarked = true;
        if (level instanceof ServerLevel server) {
            server.sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.66F, 0.86F, 1.0F), 1.4F),
                    living.getX(), living.getY() + 0.8D, living.getZ(),
                    30, 0.4D, 0.7D, 0.4D, 0.02D);
        }
    }

    /**
     * 双元素法术的**三级附加**（作者 2026-10-20 定稿）。
     *
     * <p>同一对元素，主料是谁就附加谁的效果 —— 这是三级才有的"质变"，
     * 也是 16 点消耗的回报。
     */
    private static void applyTierThreeBonus(Level level, Player player, Declaration declaration,
                                            SpellParams params, HitResult hit, Phenomenon phenomenon) {
        if (tierOf(declaration) < 3 || !isDualInput(declaration)) {
            return;
        }
        LivingEntity target = hitLiving(hit);
        var at = hitPoint(player, hit);

        switch (declaration.element()) {
            case FIRE -> // 爆炸 power 1.5；带负地时不破坏地形
                    explodeAt(level, player, hit, 1.5F, canTouchBlocks(declaration));
            case WATER -> {
                // 冻结 3 秒 + 缚手 3 秒
                if (target != null) {
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + 60);
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
                    boundHands(target, player, 3);
                }
            }
            case EARTH -> {
                // 缓慢 + 缚手
                if (target != null) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                    boundHands(target, player, 3);
                }
            }
            case WIND -> {
                // 作者 2026-10-21：改成"命中后立即追加一次同等效果的法术"。
                // 同一个目标再结算一遍，但 allowTierBonus = false，否则会无限叠。
                dispatch(level, player, phenomenon, params, hit, declaration, false);
            }
            case GRASS -> {
                // 本法术的状态：持续 ×1.5、等级 +1
                if (!phenomenon.type().startsWith("effect:")) {
                    break;
                }
                var id = net.minecraft.resources.ResourceLocation.tryParse(
                        phenomenon.type().substring("effect:".length()));
                if (id == null) {
                    break;
                }
                var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id);
                if (holder.isEmpty()) {
                    break;
                }
                LivingEntity who = holder.get().value().isBeneficial() ? player : target;
                if (who != null) {
                    int ticks = (int) Math.round(Math.max(20, effectTicks(phenomenon, params)) * 1.5D);
                    who.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            holder.get(), ticks, 1));
                }
            }
            case LIGHT -> {
                // 失明 5 秒 + 范围发光
                if (level instanceof ServerLevel server) {
                    for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class,
                            new net.minecraft.world.phys.AABB(at, at).inflate(6.0D))) {
                        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.BLINDNESS, 100, 0));
                        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                net.minecraft.world.effect.MobEffects.GLOWING, 200, 0));
                    }
                }
            }
            case SHADOW -> {
                // 5 个负面 + 黑暗 5 秒
                if (target != null) {
                    for (var effect : java.util.List.of(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                            net.minecraft.world.effect.MobEffects.WEAKNESS,
                            net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
                            net.minecraft.world.effect.MobEffects.CONFUSION,
                            net.minecraft.world.effect.MobEffects.POISON)) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 100, 0));
                    }
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.DARKNESS, 100, 0));
                }
            }
            default -> {
            }
        }
    }

    /**
     * 缚手的实际时长 = 基础秒数 × 首领折扣 × 重复命中折扣。
     *
     * <p>作者 2026-10-20：12 秒的完全封锁会让玩家把首领当木桩打，必须削弱。
     * 两条递减规则：
     * <ul>
     *   <li>**首领**（生命 ≥100，或凋灵 / 末影龙）时长为四折</li>
     *   <li>**反复命中**：20 秒内每多糊一次就再减半，最多累计四次（约 1/16）</li>
     * </ul>
     * 结果是：杂鱼第一次 8 秒，首领 3.2 秒，连糊首领第三次只剩 0.8 秒 —— 秒不掉也得走位。
     */
    private static int boundHandsTicks(LivingEntity target, int baseSeconds) {
        double factor = isBossLike(target) ? 0.4D : 1.0D;

        var data = target.getPersistentData();
        long now = target.level().getGameTime();
        int streak = now - data.getLong("ev_bound_last") < 400L
                ? Math.min(4, data.getInt("ev_bound_streak") + 1)
                : 0;
        data.putLong("ev_bound_last", now);
        data.putInt("ev_bound_streak", streak);
        factor *= Math.pow(0.5D, streak);

        int ticks = (int) Math.round(baseSeconds * 20.0D * factor);
        return Math.max(20, Math.min(ticks, 8 * 20));
    }

    /** 算不算"首领级"目标。铁傀儡 100 血，也算。 */
    private static boolean isBossLike(LivingEntity target) {
        return target.getMaxHealth() >= 100.0F
                || target instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
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
    private static void ignite(Level level, HitResult hit, Phenomenon phenomenon, SpellParams params,
                               Declaration declaration) {
        // 1 级：着火 5 秒；2 级：额外 5 点火焰伤害；3 级已经不走到这里（变成爆炸）
        int fireTicks = 100;
        int tier = tierOf(declaration);

        if (hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof LivingEntity living && tier >= 2) {
                living.hurt(level.damageSources().magic(), 5.0F);
            }
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
                // 爆燃 = 一次恶魂火球大小的爆炸，然后点燃周围
                if (level instanceof ServerLevel server) {
                    var at = new net.minecraft.world.phys.Vec3(
                            center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
                    // 不破坏方块（只冲击 + 点燃），避免一发法术把家拆了
                    server.explode(player, at.x, at.y, at.z, 1.0F,
                            Level.ExplosionInteraction.NONE);
                }
                if (canTouchBlocks(declaration)) {
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
            case "place_stone" -> {
                if (!canTouchBlocks(declaration)) {
                    break;
                }
                // 纯地 = 挖掘：边长 n 的正方体，硬度低于黑曜石才能破。
                // 注意中心要用**被命中的方块**，不能用它外侧的空气格 —— 否则 1/2 级会挖空气。
                if (isPureEarth(declaration.inputValue())) {
                    BlockPos target = hit instanceof BlockHitResult blockHit
                            ? blockHit.getBlockPos() : center;
                    digCube(level, target, Math.max(1, Math.min(3, declaration.tier())));
                } else {
                    fillSphere(level, center, radius, Blocks.STONE.defaultBlockState());
                }
            }
            case "place_grass" -> {
                if (canTouchBlocks(declaration)) {
                    fillSphere(level, center, radius, Blocks.GRASS_BLOCK.defaultBlockState());
                }
            }
            case "bonemeal" -> {
                // 浇灌：等于撒了一把骨粉 —— 走原版的 BonemealableBlock 接口，
                // 作物、树苗、草方块、水草全都按原版规则长
                if (level instanceof ServerLevel server && canTouchBlocks(declaration)) {
                    var random = level.getRandom();
                    for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                            center.offset(radius, 1, radius))) {
                        BlockPos spot = pos.immutable();
                        var state = level.getBlockState(spot);
                        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock growable)) {
                            continue;
                        }
                        if (!growable.isValidBonemealTarget(level, spot, state)) {
                            continue;
                        }
                        if (!growable.isBonemealSuccess(level, random, spot, state)) {
                            continue;
                        }
                        growable.performBonemeal(server, random, spot, state);
                    }
                }
            }
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
                // 风弹式：从命中点向外炸开，而不是用 intensity（风态给的是 radius，旧版几乎推不动）
                if (level instanceof ServerLevel server) {
                    var origin = new net.minecraft.world.phys.Vec3(
                            center.getX() + 0.5D, center.getY() + 0.5D, center.getZ() + 0.5D);
                    // 1/2/3 级 = 中等 / 大力 / 超大力
                    int tier = tierOf(declaration);
                    double strength = switch (tier) {
                        case 3 -> 3.2D;
                        case 2 -> 2.4D;
                        default -> 1.7D;
                    } + params.radius() * 0.2D;
                    double lift = tier >= 3 ? 0.95D : 0.55D;
                    for (Entity entity : server.getEntitiesOfClass(Entity.class,
                            new net.minecraft.world.phys.AABB(origin, origin).inflate(radius + 1.0D))) {
                        if (entity == player) {
                            continue;
                        }
                        var dir = entity.position().subtract(origin);
                        if (dir.lengthSqr() < 1.0E-4D) {
                            dir = player.getViewVector(1.0F);
                        }
                        dir = dir.normalize();
                        entity.push(dir.x * strength, lift, dir.z * strength);
                        // 不加这一行，客户端看不到击退
                        entity.hurtMarked = true;
                    }
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.GUST,
                            origin.x, origin.y, origin.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                            origin.x, origin.y, origin.z, 20, 0.8D, 0.4D, 0.8D, 0.15D);
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
                // 净值归零：什么都不发生，但很脏
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                        SoundSource.PLAYERS, 0.8F, 0.6F);
                // 1/2/3 级 = 少量 / 更多 / 最多的余热（"推进一阶"要等阶段 06）
                if (!level.isClientSide()) {
                    com.water2h2o.elementalverse.heat.ChunkHeat.add(
                            level, player.blockPosition(), 20.0D * tierOf(declaration));
                }
            }
            case "extend_effects" -> {
                // 1 级 ×1.2；2 级 ×1.5 且等级 +1；3 级 ×2、等级 +2、再瞬回 4 点
                int tier = tierOf(declaration);
                double mul = tier == 3 ? 2.0D : tier == 2 ? 1.5D : 1.2D;
                int addLevel = tier == 3 ? 2 : tier == 2 ? 1 : 0;
                player.getActiveEffects().stream().toList().forEach(effect ->
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                effect.getEffect(),
                                (int) Math.round(effect.getDuration() * mul),
                                Math.min(3, effect.getAmplifier() + addLevel))));
                if (tier == 3) {
                    player.heal(4.0F);
                }
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
                              SpellParams params, Declaration declaration) {
        var pool = java.util.List.of(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                net.minecraft.world.effect.MobEffects.WEAKNESS,
                net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
                net.minecraft.world.effect.MobEffects.CONFUSION,
                net.minecraft.world.effect.MobEffects.POISON);
        var random = level.getRandom();
        var picked = new java.util.ArrayList<>(pool);
        java.util.Collections.shuffle(picked, new java.util.Random(random.nextLong()));
        int tier = tierOf(declaration);
        // 1 级 3 个负面；2 级起 5 个；3 级再加缚手与易伤
        int count = Math.min(tier == 1 ? 3 : 5, picked.size());
        int ticks = 100;
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
        if (tier == 3) {
            boundHands(target, player, 3);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.water2h2o.elementalverse.registry.EVMobEffects.VULNERABLE, 200, 0));
        }
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
        // 负主成分不兜底：冰/霜/砂这些克制剂不该放出对应轴的法术
        if (axis == null || input.dominantSign() <= 0) {
            return;
        }
        HitResult target = suppliedHit != null && suppliedHit.getType() != HitResult.Type.MISS
                ? suppliedHit
                : player.pick(RANGE, 1.0F, false);
        switch (axis) {
            case FIRE -> ignite(level, target, new Phenomenon("ignite", axis, 1.0, 40, 0.0), params,
                    Declaration.EMPTY);
            case WATER -> douse(level, target, params);
            default -> {
                // 目前只为火 / 水实现了通用兜底；其余元素没有现象，调用方会在扣材料前拦下。
            }
        }
    }

    /** 没有精确配方时，主成分是否还有可执行的通用现象。 */
    private static boolean hasFallback(ElementValue input) {
        ElementAxis axis = input.dominant();
        // 必须是**正的**主成分。冰是纯负火，主成分仍然是火 ——
        // 以前只看轴不看符号，于是冰元素申明板会点火（作者 2026-10-21 问到的那一幕）。
        return input.dominantSign() > 0
                && (axis == ElementAxis.FIRE || axis == ElementAxis.WATER);
    }

    // ---------------------------------------------------------------- 材料

    /**
     * 施法的材料判定：**认轴、算值、小瓶优先**。
     *
     * <p>以前是按"瓶子里的元素种类"精确匹配（洪流只收 water），于是霜和冰明明主成分都是水却用不了。
     * 现在改成看**元素值**：
     * <ul>
     *   <li>轴：法术主料的主成分轴（洪流 = 水轴）</li>
     *   <li>值：只认**主成分同轴、且该轴为正**的载体；水 1.0 供 1 点</li>
     *   <li>二级元素是纯负值（霜/冰 = 负火，砂/金 = 负水），只做克制剂，
     *       不能给同轴法术供能 —— 2026-10-21 的简化口径</li>
     * </ul>
     *
     * <p>成本按档位 1 / 2 / 3 **点**（写入仍是 1/3/9 瓶，两码事）。
     */

    /**
     * 一次施法需要多少**点元素值**：1/2/3 档 = 4 / 8 / 16。
     *
     * <p>成本只按**主轴**收；辅料/态在写板时已经付过，释放不按次扣。
     */
    /**
     * 这条申明的冷却：基础 3 秒，每高一级 +1 秒，拼贴 +1 秒；金锭每层 -30%（最多两层）。
     */
    public static int cooldownTicks(Declaration declaration) {
        int base = 60 + 20 * (Math.max(1, Math.min(3, declaration.tier())) - 1);
        if (declaration.hasSecond()) {
            base += 20;
        }
        int gold = Math.min(com.water2h2o.elementalverse.element.Modifiers.COOLDOWN_MAX_STACK,
                com.water2h2o.elementalverse.element.Modifiers.count(
                        declaration.modifier(),
                        com.water2h2o.elementalverse.element.Modifiers.COOLDOWN));
        double mul = 1.0D - com.water2h2o.elementalverse.element.Modifiers.COOLDOWN_PER_STACK * gold;
        return Math.max(10, (int) Math.round(base * mul));
    }

    private static double castCost(Declaration declaration) {
        return switch (Math.max(1, Math.min(3, declaration.tier()))) {
            case 3 -> 16.0D;
            case 2 -> 8.0D;
            default -> 4.0D;
        };
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
     * 按元素值扣材料：**只认主轴正向载体，小瓶优先**。
     *
     * <p>二级元素是纯负值，不参与供能；需要 1 点时只吃主轴正向的元素瓶。
     * 整瓶不可分，所以最后可能略超需求量 —— 这是有意的代价。
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
