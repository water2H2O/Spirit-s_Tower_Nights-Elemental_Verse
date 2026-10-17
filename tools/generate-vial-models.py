"""生成元素瓶的「一物多图标」模型。

为什么用脚本：图标要按 `ElementType` 的**枚举序号**分发，而序号在 Java 里。
手写一遍 JSON 迟早会跟枚举错位（错位的表现是"图标张冠李戴"，很难查）。
所以这里直接解析 `ElementType.java`，序号唯一来源就是代码。

产物：
    models/item/element_vial.json        基础模型 + 按 custom_model_data 的 overrides
    models/item/vial/<id>.json           每个元素一个薄模型（只差贴图）

分发机制（1.21.1）：物品带 `custom_model_data` 组件，值 = 枚举序号 + 1，
物品模型的 `overrides` 用 `predicate: {custom_model_data: N}` 选模型。
值为 0 / 不存在 → 基础模型（空瓶）。**不需要任何客户端代码。**

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

# 光暗与它们的衍生（晨、影）用「特殊容器」的贴图，其余用各自的瓶内液体色。
# 作者 2026-10-17 的要求：光暗这一支应该一眼看出是"另一种容器"。
SPECIAL_TEXTURES = {
    "light": f"{NS}:item/refraction_prism",
    "dawn": f"{NS}:item/refraction_prism",          # 晨来自光
    "shadow": f"{NS}:item/shadow_bottle",
    "shadow_essence": f"{NS}:item/shadow_bottle",   # 影来自暗
}

BASE_TEXTURE = f"{NS}:item/element_vial"


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


def texture_for(element_id: str) -> str:
    if element_id in SPECIAL_TEXTURES:
        return SPECIAL_TEXTURES[element_id]
    return f"{NS}:item/vial/{element_id}"


def write_json(path: str, payload: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print("  wrote", os.path.relpath(path, ROOT))


def main() -> None:
    order = parse_enum_order()
    if not order or order[0] != "empty":
        raise SystemExit(f"枚举解析失败或顺序意外：{order[:3]}")

    print(f"枚举：{len(order)} 个形态（{order[0]} 为基态）")

    overrides = []
    for position, element_id in enumerate(order):
        if element_id == "empty":
            continue

        model_index = position + 1  # 与 ElementType.modelIndex() 保持一致
        model_name = f"{NS}:item/vial/{element_id}"

        write_json(
            os.path.join(MODELS, "vial", f"{element_id}.json"),
            {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": texture_for(element_id)},
            },
        )

        overrides.append({
            "predicate": {"custom_model_data": model_index},
            "model": model_name,
        })

    write_json(
        os.path.join(MODELS, "element_vial.json"),
        {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": BASE_TEXTURE},
            "overrides": overrides,
        },
    )

    print(f"共 {len(overrides)} 个图标分发项")


if __name__ == "__main__":
    main()
