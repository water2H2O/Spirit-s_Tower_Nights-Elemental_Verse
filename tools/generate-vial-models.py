"""生成三种元素载体的「一物多图标」分发模型。

为什么用脚本：图标按 `ElementType` 的**枚举序号**分发，而序号在 Java 里。
手写 JSON 迟早会跟枚举错位（表现是"图标张冠李戴"，很难查）。
所以这里解析 `ElementType.java`，序号唯一来源是代码。

三种载体（作者 2026-10-17 定的归属）：
    element_vial      地/水/风/火/草 + 砂/冰/电/木/烬/霜/金/芽
    refraction_prism  光 / 晨
    shadow_bottle     暗 / 影

每个载体写一个 `models/item/<载体>.json`：作者给的基础模型 + overrides 分发段。
填充态的图标沿用作者按元素画的 `element_vial_<id>` 模型。

分发机制（1.21.1）：物品带 `custom_model_data` 组件，值 = 枚举序号 + 1，
overrides 用 `predicate: {custom_model_data: N}` 选模型。0 / 缺失 = 基底态。

用法：
    python tools/generate-vial-models.py
"""

from __future__ import annotations

import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse")
MODELS = os.path.join(ASSETS, "models", "item")
ELEMENT_TYPE = os.path.join(
    ROOT, "src", "main", "java", "com", "water2h2o", "elementalverse", "element", "ElementType.java"
)

NS = "elemental_verse"

# 载体 -> 未装东西时的贴图（作者的稿）
CARRIER_BASE = {
    "element_vial": f"{NS}:item/element_vial",
    "refraction_prism": f"{NS}:item/refraction_prism",
    "shadow_bottle": f"{NS}:item/shadow_bottle",
}

# 载体 -> 内容态模型的文件名前缀。
# 元素瓶是 element_vial_<id>，棱镜是 prism_<id>，聚影瓶是 bottle_<id>
# （作者 2026-10-17 补充素材定的命名）。
CARRIER_MODEL_PREFIX = {
    "element_vial": "element_vial_",
    "refraction_prism": "prism_",
    "shadow_bottle": "bottle_",
}

# 与 ElementType.carrier() 保持一致
CARRIER_OF = {
    "light": "refraction_prism",
    "dawn": "refraction_prism",
    "shadow": "shadow_bottle",
    "shadow_essence": "shadow_bottle",
}


def parse_enum_order() -> list[str]:
    """按源码顺序抓出枚举常量的 id（构造器第一个参数）。"""
    ids: list[str] = []
    with open(ELEMENT_TYPE, "r", encoding="utf-8") as handle:
        for line in handle:
            stripped = line.strip()
            if stripped.startswith("//") or stripped.startswith("*") or stripped.startswith("/*"):
                continue
            match = re.match(r'^[A-Z_]+\(\s*"([a-z_]+)"', stripped)
            if match:
                ids.append(match.group(1))
    return ids


def carrier_of(element_id: str) -> str:
    return CARRIER_OF.get(element_id, "element_vial")


def main() -> None:
    order = parse_enum_order()
    if not order or order[0] != "empty":
        raise SystemExit(f"枚举解析失败或顺序意外：{order[:3]}")

    print(f"枚举：{len(order)} 个形态（{order[0]} 为基态）")

    overrides: dict[str, list] = {carrier: [] for carrier in CARRIER_BASE}
    missing: list[str] = []

    for position, element_id in enumerate(order):
        if element_id == "empty":
            continue

        carrier = carrier_of(element_id)
        model_index = position + 1  # 与 ElementType.modelIndex() 保持一致
        model_base = CARRIER_MODEL_PREFIX[carrier] + element_id

        model_file = os.path.join(MODELS, f"{model_base}.json")
        if not os.path.exists(model_file):
            missing.append(os.path.basename(model_file))

        overrides[carrier].append({
            "predicate": {"custom_model_data": model_index},
            "model": f"{NS}:item/{model_base}",
        })

    if missing:
        raise SystemExit("缺少元素模型（请先放入作者的 assets）：" + ", ".join(missing))

    for carrier, base_texture in CARRIER_BASE.items():
        # 关键：生成的模型 parent 指向 <载体>_base.json（作者的稿），
        # 而不是把贴图内联进来。这样以后新素材包覆盖 <载体>.json 时，
        # 我们只要重跑本脚本就能恢复分发，作者的美术也永远在 _base 里不被搅动。
        base_model = os.path.join(MODELS, f"{carrier}_base.json")
        if not os.path.exists(base_model):
            # 首次运行：把作者的模型落成 _base
            with open(base_model, "w", encoding="utf-8") as handle:
                json.dump(
                    {
                        "parent": "minecraft:item/generated",
                        "textures": {"layer0": base_texture},
                    },
                    handle,
                    ensure_ascii=False,
                    indent=2,
                )
                handle.write("\n")
            print(f"  建立基底模型 {os.path.relpath(base_model, ROOT)}")

        payload = {
            "parent": f"{NS}:item/{carrier}_base",
            "overrides": overrides[carrier],
        }
        target = os.path.join(MODELS, f"{carrier}.json")
        with open(target, "w", encoding="utf-8") as handle:
            json.dump(payload, handle, ensure_ascii=False, indent=2)
            handle.write("\n")
        print(f"  {carrier}: {len(overrides[carrier])} 个分发项 -> {os.path.relpath(target, ROOT)}")


if __name__ == "__main__":
    main()
