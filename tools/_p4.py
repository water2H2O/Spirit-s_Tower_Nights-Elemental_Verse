import pathlib, json

ROOT = pathlib.Path(r"D:\vibe_coding\elemental-verse")
J = ROOT / "src/main/java/com/water2h2o/elementalverse"
RES = ROOT / "src/main/resources"


def rep(rel, old, new, label):
    p = J / rel
    t = p.read_text(encoding="utf-8")
    assert old in t, f"ANCHOR MISS: {label}"
    p.write_text(t.replace(old, new, 1), encoding="utf-8")
    print("ok", label)


# ---------- 1) 元素瓶大名字恢复白色（数值行仍分色） ----------
rep("element/ElementVialItem.java",
"""            return Component.translatable(EVLang.vial(type)).withStyle(type.color());""",
"""            return Component.translatable(EVLang.vial(type));""",
    "瓶名恢复白色")

# ---------- 2) 弹射物不再画瓶子，只留元素粒子 ----------
rep("entity/SpellProjectile.java",
"""        return ElementVialItem.createStack(element);""",
"""        // 作者：不要把瓶子本身扔出去 —— 飞出去的应该就是"元素"
        return ItemStack.EMPTY;""",
    "弹射物不画瓶子")

rep("entity/SpellProjectile.java",
"""        DustParticleOptions dust = new DustParticleOptions(
                new org.joml.Vector3f(((color >> 16) & 0xFF) / 255.0F,
                        ((color >> 8) & 0xFF) / 255.0F,
                        (color & 0xFF) / 255.0F), 0.9F);
        for (int i = 0; i < 2; i++) {""",
"""        DustParticleOptions dust = new DustParticleOptions(
                new org.joml.Vector3f(((color >> 16) & 0xFF) / 255.0F,
                        ((color >> 8) & 0xFF) / 255.0F,
                        (color & 0xFF) / 255.0F), 1.6F);
        // 没有瓶子可看，粒子就是它的"身体"，所以给密一点
        for (int i = 0; i < 4; i++) {""",
    "粒子加强")

# ---------- 3) 风的三级附加改成真·风弹 ----------
rep("reaction/SpellCaster.java",
"""            case WIND -> {
                // 追加 2 发风弹
                if (level instanceof ServerLevel server) {
                    for (int i = 0; i < 2; i++) {
                        var extra = new com.water2h2o.elementalverse.entity.SpellProjectile(
                                server, player, declaration);
                        extra.setPos(player.getX(), player.getEyeY() - 0.15D, player.getZ());
                        extra.setNoGravity(true);
                        float spread = i == 0 ? -14.0F : 14.0F;
                        extra.shootFromRotation(player, player.getXRot(), player.getYRot() + spread,
                                0.0F, 1.6F, 1.5F);
                        server.addFreshEntity(extra);
                    }
                }
            }""",
"""            case WIND -> {
                // 追加 2 发**原版风弹**。
                // 不能再用 SpellProjectile —— 那等于把本法术又放了两遍，
                // 玩家看到的就是"莫名其妙多出两个法术"。
                if (level instanceof ServerLevel server) {
                    for (int i = 0; i < 2; i++) {
                        var charge = new net.minecraft.world.entity.projectile.windcharge.WindCharge(
                                player, server, player.getX(), player.getEyeY() - 0.2D, player.getZ());
                        float spread = i == 0 ? -12.0F : 12.0F;
                        charge.shootFromRotation(player, player.getXRot(), player.getYRot() + spread,
                                0.0F, 1.6F, 1.0F);
                        server.addFreshEntity(charge);
                    }
                }
            }""",
    "风附加改真风弹")

# ---------- 4) 锻造产物起个名字 ----------
rep("recipe/DeclarationUpgradeRecipe.java",
"""        Declaration declaration = DeclarationPlateItem.getDeclaration(input.template()).withForm(form);
        out.set(EVDataComponents.DECLARATION.get(), declaration);
        return out;""",
"""        Declaration declaration = DeclarationPlateItem.getDeclaration(input.template()).withForm(form);
        out.set(EVDataComponents.DECLARATION.get(), declaration);

        // 刻了申明的剑 / 书必须一眼能认出来，否则和普通物品没差别
        String key = "projectile".equals(form)
                ? "item.elemental_verse.declaration_tome"
                : "item.elemental_verse.declaration_sword";
        net.minecraft.network.chat.Component magicName = declaration.magic().isEmpty()
                ? declaration.element().displayName()
                : net.minecraft.network.chat.Component.literal(declaration.magic());
        out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.translatable(key, magicName)
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
        return out;""",
    "锻造产物命名")

# ---------- 5) 语言 ----------
lang = RES / "assets/elemental_verse/lang"
add = {
    "zh_cn.json": {
        "item.elemental_verse.declaration_sword": "申明剑 · %s",
        "item.elemental_verse.declaration_tome": "申明书 · %s",
    },
    "en_us.json": {
        "item.elemental_verse.declaration_sword": "Inscribed Sword · %s",
        "item.elemental_verse.declaration_tome": "Inscribed Tome · %s",
    },
}
for name, extra in add.items():
    f = lang / name
    d = json.loads(f.read_text(encoding="utf-8"))
    d.update(extra)
    f.write_text(json.dumps(d, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("ok lang")
