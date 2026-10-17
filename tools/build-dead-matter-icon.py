"""用死寂物质方块的三张面贴图，合成它的**物品图标**（等轴测小方块）。

为什么需要：作者提供的 `textures/item/dead_matter.png` 是**尘**的图标，
方块本身只有 side / top / bottom 三张面贴图。物品栏里要显示一个"立体的小方块"，
所以这里把三张面贴图按等轴测比例拼成 16×16 图标。

等轴测布局（与 Minecraft 方块图标相同的比例）：
        顶点 (8,0)
    左 (0,4)   右 (16,4)
        底 (8,8)        左右两侧面一直到 y=16

用法：
    python tools/build-dead-matter-icon.py
"""

from __future__ import annotations

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCK = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse", "textures", "block")
ITEM = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse", "textures", "item")

SIZE = 16
TRANSPARENT = (0, 0, 0, 0)

# 三个面的明暗（左面略暗、右面最暗，制造体积感）
SHADE_LEFT = 0.92
SHADE_RIGHT = 0.80


def load(name: str) -> Image.Image:
    return Image.open(os.path.join(BLOCK, name)).convert("RGBA")


def sample(face: Image.Image, u: float, v: float) -> tuple[int, int, int, int]:
    x = min(15, max(0, int(u)))
    y = min(15, max(0, int(v)))
    return face.getpixel((x, y))


def shade(color: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
    r, g, b, a = color
    return (int(r * factor), int(g * factor), int(b * factor), a)


def main() -> None:
    top = load("dead_matter_top.png")
    side = load("dead_matter.png")

    canvas = Image.new("RGBA", (SIZE, SIZE), TRANSPARENT)

    for y in range(SIZE):
        for x in range(SIZE):
            px, py = x + 0.5, y + 0.5

            # ---- 顶面：菱形 |x-8|/8 + |y-4|/4 <= 1 ----
            if abs(px - 8) / 8.0 + abs(py - 4) / 4.0 <= 1.0:
                u = 2 * py + (px - 8)
                v = 2 * py - (px - 8)
                canvas.putpixel((x, y), sample(top, u, v))
                continue

            # ---- 左面：上沿从 (0,4) 斜到 (8,8)，再往下 8 像素 ----
            if px < 8 and py >= 4:
                u = 2 * px
                v = 2 * (py - 4 - px * 0.5)
                if 0 <= u < 16 and 0 <= v < 16:
                    canvas.putpixel((x, y), shade(sample(side, u, v), SHADE_LEFT))
                    continue

            # ---- 右面：上沿从 (8,8) 斜到 (16,4) ----
            if px >= 8 and py >= 4:
                u = 2 * (px - 8)
                v = 2 * (py - 8 + (px - 8) * 0.5)
                if 0 <= u < 16 and 0 <= v < 16:
                    canvas.putpixel((x, y), shade(sample(side, u, v), SHADE_RIGHT))
                    continue

    out = os.path.join(ITEM, "dead_matter_block.png")
    canvas.save(out)
    print("wrote", os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main()
