import pathlib

ROOT = pathlib.Path(r"D:\vibe_coding\elemental-verse")
J = ROOT / "src/main/java/com/water2h2o/elementalverse"
p = J / "entity/SpellProjectile.java"
t = p.read_text(encoding="utf-8")


def rep(old, new, label):
    global t
    assert old in t, f"ANCHOR MISS: {label}"
    t = t.replace(old, new, 1)
    print("ok", label)


rep("""    /** 是否追踪最近的生物（水元素的正向参数）。 */
    private boolean homing;""",
    """    /** 是否追踪最近的生物（水元素的正向参数）。 */
    private boolean homing;

    /** 糖：飞得更远（寿命更长）。 */
    private boolean swiftMode;

    /** 紫颂果：穿墙，只对生物生效。 */
    private boolean phaseMode;""",
    "字段")

rep("""        ElementType element = declaration.element();
        this.entityData.set(DATA_ELEMENT, element == null ? "" : element.getSerializedName());""",
    """        ElementType element = declaration.element();
        this.entityData.set(DATA_ELEMENT, element == null ? "" : element.getSerializedName());
        String modifier = declaration.modifier() == null ? "" : declaration.modifier();
        this.swiftMode = com.water2h2o.elementalverse.element.Modifiers.SWIFT.equals(modifier);
        this.phaseMode = com.water2h2o.elementalverse.element.Modifiers.PHASE.equals(modifier);""",
    "构造器读修饰")

# 生命周期 + 穿透
rep("""        // 服务端：追踪 + 超时回收
        if (!level().isClientSide()) {
            if (tickCount > 100) {
                discard();
                return;
            }
            if (homing) {
                steer();
            }
            return;
        }""",
    """        if (!level().isClientSide()) {
            // 糖：射程延长到 7.5 秒
            if (tickCount > (swiftMode ? 150 : 100)) {
                discard();
                return;
            }
            // 紫颂果：穿墙，只对生物生效
            if (phaseMode) {
                this.noPhysics = true;
                var targets = level().getEntities(this, getBoundingBox().inflate(0.45D),
                        e -> e instanceof LivingEntity living && living.isAlive() && e != getOwner());
                if (!targets.isEmpty()) {
                    onHit(new net.minecraft.world.phys.EntityHitResult(targets.get(0)));
                    return;
                }
            }
            if (homing) {
                steer();
            }
            return;
        }""",
    "穿透与射程")

# 火药：延后结算
rep("""        if (getOwner() instanceof Player player) {
            SpellCaster.applyProjectileHit(level(), player, declaration, result);
        }
        discard();""",
    """        if (getOwner() instanceof Player player) {
            if (com.water2h2o.elementalverse.element.Modifiers.FUSE
                    .equals(declaration.modifier()) && level() instanceof ServerLevel server) {
                // 火药：先落地，1.5 秒后才结算
                PendingCasts.runLater(server,
                        com.water2h2o.elementalverse.element.Modifiers.FUSE_TICKS,
                        () -> SpellCaster.applyProjectileHit(level(), player, declaration, result));
            } else {
                SpellCaster.applyProjectileHit(level(), player, declaration, result);
            }
        }
        discard();""",
    "火药引信")

t = t.replace("import com.water2h2o.elementalverse.reaction.SpellCaster;",
              "import com.water2h2o.elementalverse.reaction.PendingCasts;\n"
              "import com.water2h2o.elementalverse.reaction.SpellCaster;", 1)
p.write_text(t, encoding="utf-8")
print("done")
