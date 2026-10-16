"""生成模组图标 pack.png（模组列表里显示的那个）。

取元素瓶的 16x16 纹理放大 8 倍，垫一层水塔夜谈的签名青色底。
放大用 NEAREST，保住像素风的硬边，不要糊。

用法：
    python tools/generate-mod-icon.py
"""

from __future__ import annotations

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse")
RESOURCES = os.path.join(ROOT, "src", "main", "resources")

SIZE = 128
SCALE = 8  # 16 * 8 = 128
BACKGROUND = (18, 26, 32, 255)   # 深色底，模组列表里偏暗的背景上能看清
ACCENT = (175, 255, 255, 255)    # 淼渺的主题色 #AFFFFF


def main() -> None:
    vial_path = os.path.join(ASSETS, "textures", "item", "element_vial.png")
    vial = Image.open(vial_path).convert("RGBA")
    vial = vial.resize((16 * SCALE, 16 * SCALE), Image.NEAREST)

    canvas = Image.new("RGBA", (SIZE, SIZE), BACKGROUND)

    # 一圈主题色描边，让图标在列表里有轮廓
    for i in range(SIZE):
        canvas.putpixel((i, 0), ACCENT)
        canvas.putpixel((i, SIZE - 1), ACCENT)
        canvas.putpixel((0, i), ACCENT)
        canvas.putpixel((SIZE - 1, i), ACCENT)

    canvas.alpha_composite(vial, (0, 0))

    out = os.path.join(RESOURCES, "pack.png")
    canvas.save(out)
    print("wrote", os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main()
