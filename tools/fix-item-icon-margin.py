"""修元素瓶贴图的顶部贴边问题。

现象：作者的元素瓶贴图第 0 行就有像素（瓶口顶到格子最上沿），
在物品栏里看起来"顶着边框"，和原版物品留白不一致。

做法：整张图**下移 1 像素**，第 0 行留空。
不改任何形状/颜色，只挪位置，所以美术原稿完整保留。
（棱镜、聚影瓶本来就从第 2 行开始，不需要动。）

另附：生成一份"加瓶塞"的对比预览（**不写入模组**，仅供作者挑选）。

用法：
    python tools/fix-item-icon-margin.py          # 只修边
    python tools/fix-item-icon-margin.py --cork-preview   # 另出对比图
"""

from __future__ import annotations

import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse", "textures", "item")

# 元素瓶这一族（空瓶 + 17 种元素）
VIAL_PREFIX = "element_vial"

# 瓶塞配色（取自原版软木的暖灰调，避免抢玻璃的冷色）
CORK_MAIN = (126, 100, 74, 255)
CORK_DARK = (96, 74, 54, 255)
CORK_LIGHT = (150, 122, 92, 255)


def shift_down(image: Image.Image, pixels: int = 1) -> Image.Image:
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    out.paste(image, (0, pixels))
    return out


def add_cork(image: Image.Image) -> Image.Image:
    """在最上面画一个 4×2 的软木塞（只用于预览）。"""
    out = image.copy()
    px = out.load()
    # 找到瓶口所在的列范围（第 0 行非透明像素）
    cols = [x for x in range(16) if px[x, 0][3] > 0]
    if not cols:
        return out
    center = (min(cols) + max(cols)) // 2
    left, right = center - 2, center + 1
    for x in range(left, right + 1):
        px[x, 0] = CORK_LIGHT if x == left else CORK_MAIN
        px[x, 1] = CORK_DARK if x == left else CORK_MAIN
    return out


def fix_all() -> int:
    changed = 0
    for name in sorted(os.listdir(ITEM)):
        if not name.startswith(VIAL_PREFIX) or not name.endswith(".png"):
            continue
        path = os.path.join(ITEM, name)
        image = Image.open(path).convert("RGBA")
        px = image.load()
        if any(px[x, 0][3] > 0 for x in range(16)):
            shift_down(image).save(path)
            changed += 1
            print(f"  下移 1px: {name}")
    return changed


def cork_preview(count: int = 6) -> None:
    """把前几张瓶图拼一张对比图（上排原样，下排加瓶塞），放大 6 倍。"""
    names = [n for n in sorted(os.listdir(ITEM)) if n.startswith(VIAL_PREFIX) and n.endswith(".png")][:count]
    if not names:
        return
    scale, pad = 6, 4
    cell = 16 * scale
    width = len(names) * (cell + pad) + pad
    height = 2 * (cell + pad) + pad
    canvas = Image.new("RGBA", (width, height), (60, 60, 60, 255))

    for column, name in enumerate(names):
        image = Image.open(os.path.join(ITEM, name)).convert("RGBA")
        x = pad + column * (cell + pad)
        canvas.alpha_composite(image.resize((cell, cell), Image.NEAREST), (x, pad))
        with_cork = add_cork(shift_down(image, 1))
        canvas.alpha_composite(with_cork.resize((cell, cell), Image.NEAREST), (x, pad + cell + pad))

    out = os.path.join(ROOT, "docs", "vial-cork-preview.png")
    canvas.save(out)
    print("对比图（上：现状  下：加瓶塞）->", os.path.relpath(out, ROOT))


def main() -> int:
    changed = fix_all()
    print(f"共下移 {changed} 张")
    if "--cork-preview" in sys.argv:
        cork_preview()
    return 0


if __name__ == "__main__":
    sys.exit(main())
