import pathlib, json

ROOT = pathlib.Path(r"D:\vibe_coding\elemental-verse")
J = ROOT / "src/main/java/com/water2h2o/elementalverse"
RES = ROOT / "src/main/resources"


def rep(rel, old, new, label, count=1):
    p = J / rel
    t = p.read_text(encoding="utf-8")
    assert old in t, f"ANCHOR MISS: {label}"
    p.write_text(t.replace(old, new, count), encoding="utf-8")
    print("ok", label)


SC = "reaction/SpellCaster.java"

# ---------- 1) 冷却检查与写入 ----------
rep(SC,
"""        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        Optional<Reaction> matched = ReactionManager.match(input, bothSlotsSame(declaration));""",
"""        // 冷却：每条申明各自计时（存在它自己的数据里）
        long now = level.getGameTime();
        if (declaration.readyAt() > now) {
            long left = (declaration.readyAt() - now + 19) / 20;
            player.displayClientMessage(
                    Component.translatable(EVLang.MSG_COOLDOWN, left), true);
            return false;
        }

        ElementValue input = declaration.inputValue();
        SpellParams params = SpellParams.of(declaration);
        Optional<Reaction> matched = ReactionManager.match(input, bothSlotsSame(declaration));""",
    "冷却检查")

rep(SC,
"""        if (!checkMaterials(level, player, declaration)) {
            return false;
        }
        consumeMaterials(player, declaration);""",
"""        if (!checkMaterials(level, player, declaration)) {
            return false;
        }
        consumeMaterials(player, declaration);
        // 成功放出才进冷却
        com.water2h2o.elementalverse.element.DeclarationPlateItem.setDeclaration(plate,
                declaration.withReadyAt(now + cooldownTicks(declaration)));""",
    "写入冷却")

rep(SC,
"""    /** 一次施法需要多少点元素值：1/2/3 档 = 4 / 8 / 16。 */""",
"""    /**
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

    /** 一次施法需要多少点元素值：1/2/3 档 = 4 / 8 / 16。 */""",
    "冷却计算")

# ---------- 2) 修饰判定改成集合判断 ----------
t = (J / SC).read_text(encoding="utf-8")
t = t.replace(
"""            if (com.water2h2o.elementalverse.element.Modifiers.DELAY
                    .equals(declaration.modifier())) {""",
"""            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.DELAY)) {""")
t = t.replace(
"""            if (com.water2h2o.elementalverse.element.Modifiers.ECHO
                    .equals(declaration.modifier()) && level instanceof ServerLevel server) {""",
"""            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.ECHO)
                    && level instanceof ServerLevel server) {""")
t = t.replace(
"""            if (com.water2h2o.elementalverse.element.Modifiers.INSTANT
                    .equals(declaration.modifier())) {""",
"""            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.INSTANT)) {""")
t = t.replace(
"""            boolean swift = com.water2h2o.elementalverse.element.Modifiers.SWIFT
                    .equals(declaration.modifier());""",
"""            boolean swift = com.water2h2o.elementalverse.element.Modifiers.has(
                    declaration.modifier(), com.water2h2o.elementalverse.element.Modifiers.SWIFT);""")
t = t.replace(
"""        if (com.water2h2o.elementalverse.element.Modifiers.GLOW.equals(declaration.modifier())""",
"""        if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                com.water2h2o.elementalverse.element.Modifiers.GLOW)""")
(J / SC).write_text(t, encoding="utf-8")
print("ok 修饰判定统一")

# ---------- 3) 龙息：改成"对范围内单位施加本法术效果" ----------
rep(SC,
"""        // 龙息：命中点留下 5 秒元素雾
        if (com.water2h2o.elementalverse.element.Modifiers.BREATH.equals(declaration.modifier())
                && level instanceof ServerLevel breathServer) {
            var at = hit.getLocation();
            var source = player;
            for (int i = 1; i <= 10; i++) {
                PendingCasts.runLater(breathServer, i * 10, () -> {
                    for (LivingEntity living : breathServer.getEntitiesOfClass(LivingEntity.class,
                            new net.minecraft.world.phys.AABB(at, at).inflate(2.5D),
                            e -> e != source)) {
                        living.hurt(breathServer.damageSources().magic(), 1.0F);
                    }
                    breathServer.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                            at.x, at.y + 0.4D, at.z, 8, 1.1D, 0.2D, 1.1D, 0.02D);
                });
            }
        }""",
"""        // 龙息：留下一滩魔法烟雾，持续对范围内的单位施加**本法术的效果**
        if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                com.water2h2o.elementalverse.element.Modifiers.BREATH)
                && level instanceof ServerLevel breathServer) {
            var at = hit.getLocation();
            var source = player;
            var pulse = matched.map(com.water2h2o.elementalverse.reaction.Reaction::phenomenon)
                    .orElse(null);
            for (int i = 1; i <= 10; i++) {
                PendingCasts.runLater(breathServer, i * 10, () -> {
                    if (pulse != null) {
                        for (LivingEntity living : breathServer.getEntitiesOfClass(LivingEntity.class,
                                new net.minecraft.world.phys.AABB(at, at).inflate(2.5D),
                                e -> e != source)) {
                            dispatch(breathServer, source, pulse, params,
                                    new EntityHitResult(living), declaration, false);
                        }
                    }
                    breathServer.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH,
                            at.x, at.y + 0.4D, at.z, 8, 1.1D, 0.2D, 1.1D, 0.02D);
                });
            }
        }""",
    "龙息改为施加法术效果")

# ---------- 4) 弹射物里的修饰判定 ----------
PJ = "entity/SpellProjectile.java"
t = (J / PJ).read_text(encoding="utf-8")
t = t.replace(
"""        this.swiftMode = com.water2h2o.elementalverse.element.Modifiers.SWIFT.equals(modifier);
        this.phaseMode = com.water2h2o.elementalverse.element.Modifiers.PHASE.equals(modifier);""",
"""        this.swiftMode = com.water2h2o.elementalverse.element.Modifiers.has(modifier,
                com.water2h2o.elementalverse.element.Modifiers.SWIFT);
        this.phaseMode = com.water2h2o.elementalverse.element.Modifiers.has(modifier,
                com.water2h2o.elementalverse.element.Modifiers.PHASE);""")
t = t.replace(
"""            if (com.water2h2o.elementalverse.element.Modifiers.FUSE
                    .equals(declaration.modifier()) && level() instanceof ServerLevel server) {""",
"""            if (com.water2h2o.elementalverse.element.Modifiers.has(declaration.modifier(),
                    com.water2h2o.elementalverse.element.Modifiers.FUSE)
                    && level() instanceof ServerLevel server) {""")
(J / PJ).write_text(t, encoding="utf-8")
print("ok 弹射物修饰判定")

# ---------- 5) 语言 / 标签 ----------
EVL = J / "util/EVLang.java"
t = EVL.read_text(encoding="utf-8")
if "MSG_COOLDOWN" not in t:
    t = t.replace('    public static final String MSG_TOO_COMPLEX',
                  '    public static final String MSG_COOLDOWN = "message.elemental_verse.cooldown";\n'
                  '    public static final String MSG_TOO_COMPLEX', 1)
    EVL.write_text(t, encoding="utf-8")
    print("ok EVLang 冷却")

lang = RES / "assets/elemental_verse/lang"
add = {
    "zh_cn.json": {"message.elemental_verse.cooldown": "冷却中：还要 %s 秒"},
    "en_us.json": {"message.elemental_verse.cooldown": "On cooldown: %s s"},
}
for name, extra in add.items():
    f = lang / name
    d = json.loads(f.read_text(encoding="utf-8"))
    d.update(extra)
    f.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

tag = RES / "data/elemental_verse/tags/item/modifiers.json"
d = json.loads(tag.read_text(encoding="utf-8"))
if "minecraft:gold_ingot" not in d["values"]:
    d["values"].append("minecraft:gold_ingot")
    tag.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("ok 标签加金锭")

print("done")
