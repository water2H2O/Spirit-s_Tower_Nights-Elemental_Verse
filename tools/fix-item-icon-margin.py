"""元素瓶贴图的两处修正：顶部留白 + 瓶塞。

作者原稿的问题（2026-10-17 反馈）：
    1. 第 0 行就是瓶颈 → 物品栏里"顶着边框"，和原版物品的留白不一致
    2. 瓶口是敞开的 → 看着诡异

几何关系（已核对原稿）：
    原稿内容占 **0–14 行**，第 15 行本来就是空的；瓶颈最窄 2px、第 2 行拓宽到 4px。

做法（不用缩放、不改颜色，只重排 + 补 8 个像素的塞子）：
    原图 2–14 行  →  画布 3–15 行     （顶部空出 0–2 行）
    画布 1–2 行   ←  4px 宽的软木塞   （正好对上原第 2 行的瓶颈宽度）
    画布 0 行     ←  空                （不再顶边框）

结果：顶部 1 行留白、底部 1 行留白、瓶口有塞子。

用法：
    python tools/fix-item-icon-margin.py              # 修正（可重复执行）
    python tools/fix-item-icon-margin.py --preview    # 另出对比图
"""

from __future__ import annotations

import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse", "textures", "item")

VIAL_PREFIX = "element_vial"

# 软木塞配色：暖灰调，和玻璃的冷青灰拉开
CORK_MAIN = (126, 100, 74, 255)
CORK_DARK = (96, 74, 54, 255)
CORK_LIGHT = (150, 122, 92, 255)

TRANSPARENT = (0, 0, 0, 0)


def is_fixed(image: Image.Image) -> bool:
    """第 0 行空 + 第 1 行有颜色 → 已经修过。"""
    px = image.load()
    return all(px[x, 0][3] == 0 for x in range(16)) and any(px[x, 1][3] > 0 for x in range(16))


def fix_one(image: Image.Image) -> Image.Image:
    """按上面的方案重排并加塞。"""
    out = Image.new("RGBA", (16, 16), TRANSPARENT)

    # 原图 2–14 行 -> 新图 3–15 行
    out.paste(image.crop((0, 2, 16, 15)), (0, 3))

    # 瓶颈中心：取原图第 2 行（拓宽的那行）的列范围
    px = image.load()
    cols = [x for x in range(16) if px[x, 2][3] > 0]
    if not cols:
        cols = [7, 8]
    center = (min(cols) + max(cols)) // 2
    left = center - 1  # 4px 宽：left..left+3

    out_px = out.load()
    for x in range(left, left + 4):
        out_px[x, 1] = CORK_LIGHT if x == left else CORK_MAIN
        out_px[x, 2] = CORK_DARK if x == left else CORK_MAIN

    return out


def fix_all() -> int:
    changed = 0
    for name in sorted(os.listdir(ITEM)):
        if not (name.startswith(VIAL_PREFIX) and name.endswith(".png")):
            continue
        path = os.path.join(ITEM, name)
        image = Image.open(path).convert("RGBA")
        if is_fixed(image):
            # 幂等：已经修过就不再叠一次
            out = image
        else:
            out = fix_one(image)
            changed += 1
        out.save(path)
    return changed


def preview() -> None:
    names = [n for n in sorted(os.listdir(ITEM))
             if n.startswith(VIAL_PREFIX) and n.endswith(".png")][:6]
    if not names:
        return

    scale, pad = 6, 4
    cell = 16 * scale
    width = len(names) * (cell + pad) + pad
    height = 2 * (cell + pad) + pad
    canvas = Image.new("RGBA", (width, height), (58, 58, 62, 255))

    source = os.path.join(ROOT, "docs", "textures", "v7", "assets", "elemental_verse", "textures", "item")
    for column, name in enumerate(names):
        x = pad + column * (cell + pad)
        original_path = os.path.join(source, name)
        if os.path.exists(original_path):
            original = Image.open(original_path).convert("RGBA")
        else:
            original = Image.open(os.path.join(ITEM, name)).convert("RGBA")
        canvas.alpha_composite(original.resize((cell, cell), Image.NEAREST), (x, pad))
        fixed = fix_one(original)
        canvas.alpha_composite(fixed.resize((cell, cell), Image.NEAREST), (x, pad + cell + pad))

    out = os.path.join(ROOT, "docs", "vial-cork-preview.png")
    canvas.save(out)
    print("对比图（上：作者原稿  下：留白+瓶塞）->", os.path.relpath(out, ROOT))


def main() -> int:
    changed = fix_all()
    print(f"本次处理 {changed} 张（已是目标的会跳过）")
    if "--preview" in sys.argv:
        preview()
    return 0


if __name__ == "__main__":
    sys.exit(main())
