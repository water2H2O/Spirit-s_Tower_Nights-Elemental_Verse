"""生成元素瓶的「一物多图标」分发模型。

为什么用脚本：图标要按 `ElementType` 的**枚举序号**分发，而序号在 Java 里。
手写一遍 JSON 迟早会跟枚举错位（错位表现是"图标张冠李戴"，很难查）。
所以这里直接解析 `ElementType.java`，序号唯一来源就是代码。

贴图与单元素模型由作者提供（见 `docs/textures/v4`）：
    models/item/element_vial.json            空瓶（基础模型）
    models/item/element_vial_<id>.json       每个元素一个薄模型
    textures/item/element_vial_<id>.png      对应贴图

本脚本**只写一个文件**：覆盖 `models/item/element_vial.json`，
在作者给的基础模型上追加 `overrides` 分发段。

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


def main() -> None:
    order = parse_enum_order()
    if not order or order[0] != "empty":
        raise SystemExit(f"枚举解析失败或顺序意外：{order[:3]}")

    print(f"枚举：{len(order)} 个形态（{order[0]} 为基态）")

    overrides = []
    missing = []

    for position, element_id in enumerate(order):
        if element_id == "empty":
            continue

        model_index = position + 1  # 与 ElementType.modelIndex() 保持一致
        model_name = f"{NS}:item/element_vial_{element_id}"

        # 作者的模型必须存在，否则游戏里会退化成空瓶图标（静默失败）
        model_file = os.path.join(MODELS, f"element_vial_{element_id}.json")
        if not os.path.exists(model_file):
            missing.append(os.path.basename(model_file))

        overrides.append({
            "predicate": {"custom_model_data": model_index},
            "model": model_name,
        })

    if missing:
        raise SystemExit("缺少元素模型（请先放入作者的 assets）：" + ", ".join(missing))

    payload = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": BASE_TEXTURE},
        "overrides": overrides,
    }

    target = os.path.join(MODELS, "element_vial.json")
    with open(target, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    print(f"  wrote {os.path.relpath(target, ROOT)}")
    print(f"共 {len(overrides)} 个图标分发项")


if __name__ == "__main__":
    main()
