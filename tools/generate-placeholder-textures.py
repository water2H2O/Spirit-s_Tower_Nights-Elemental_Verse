"""生成阶段 01 的占位纹理。

为什么要有这个脚本，而不是直接塞一堆 png 进仓库：
    「元素瓶」和「死寂物质」以后大概率要重画，但**形状**（一个瓶子、一块灰石）
    是稳定的。脚本化之后，改配色只要改这里的表，不用重新画。

输出（16x16，PNG）：
    textures/item/element_vial.png      通用玻璃瓶（空瓶用）
    textures/item/refraction_prism.png  折射棱镜
    textures/item/shadow_bottle.png     聚影瓶
    textures/block/dead_matter.png      死寂物质

用法：
    python tools/generate-placeholder-textures.py
"""

from __future__ import annotations

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse")

TRANSPARENT = (0, 0, 0, 0)

# ---------------- 元素配色（阶段 02 做「按元素着色」时会搬进代码） ----------------

ELEMENT_COLORS = {
    "earth": (140, 104, 68),
    "water": (72, 140, 214),
    "wind": (196, 222, 214),
    "fire": (226, 110, 48),
    "grass": (110, 176, 74),
    "light": (248, 232, 150),
    "shadow": (96, 82, 140),
    "ice": (168, 220, 244),
    "frost": (206, 234, 248),
    "lightning": (250, 226, 96),
    "wood": (146, 108, 70),
    "sand": (218, 202, 150),
    "ember": (176, 74, 40),
    "metal": (196, 196, 206),
    "bud": (168, 214, 128),
    "dawn": (252, 244, 210),
    "shadow_essence": (74, 62, 108),
}

GLASS = (206, 220, 228)
GLASS_DARK = (140, 158, 172)
GLASS_LIGHT = (238, 246, 250)


def new_image() -> Image.Image:
    return Image.new("RGBA", (16, 16), TRANSPARENT)


def put(img: Image.Image, x: int, y: int, color) -> None:
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color)


def draw_vial_body(img: Image.Image, liquid) -> None:
    """画一个圆角玻璃瓶：瓶颈 + 瓶身，瓶内是液体。"""
    # 瓶口与瓶颈
    for y in range(2, 5):
        for x in range(6, 10):
            put(img, x, y, GLASS)
    put(img, 6, 2, GLASS_LIGHT)
    put(img, 9, 3, GLASS_DARK)

    # 瓶肩
    for y in (5,):
        for x in range(5, 11):
            put(img, x, y, GLASS)
    put(img, 5, 5, GLASS_DARK)
    put(img, 10, 5, GLASS_DARK)

    # 瓶身
    for y in range(6, 14):
        for x in range(4, 12):
            put(img, x, y, GLASS if liquid is None else liquid)
    # 瓶身外沿
    for y in range(6, 14):
        put(img, 4, y, GLASS_DARK)
        put(img, 11, y, GLASS_DARK)
    for x in range(4, 12):
        put(img, x, 13, GLASS_DARK)
    # 高光
    for y in range(6, 12):
        put(img, 5, y, GLASS_LIGHT)
    put(img, 6, 6, GLASS_LIGHT)

    # 底部圆角
    put(img, 4, 13, TRANSPARENT)
    put(img, 11, 13, TRANSPARENT)


def make_vial(color=None) -> Image.Image:
    img = new_image()
    draw_vial_body(img, color)
    return img


def make_prism() -> Image.Image:
    """折射棱镜：一个八面体晶体，中心偏白。"""
    img = new_image()
    body = (198, 236, 244)
    edge = (120, 172, 196)
    core = (255, 255, 255)

    # 菱形轮廓
    for y in range(2, 14):
        half = (y - 2) if y <= 8 else (13 - y)
        half = max(1, half // 2 + 1)
        for x in range(8 - half, 8 + half):
            put(img, x, y, body)
    # 棱线
    for y in range(2, 14):
        half = (y - 2) if y <= 8 else (13 - y)
        half = max(1, half // 2 + 1)
        put(img, 8 - half, y, edge)
        put(img, 8 + half - 1, y, edge)
    # 中心高光
    for y in range(6, 10):
        for x in range(6, 10):
            put(img, x, y, core)
    put(img, 7, 7, (236, 252, 255))
    return img


def make_shadow_bottle() -> Image.Image:
    """聚影瓶：深色瓶身 + 遮光材质的高光。"""
    img = new_image()
    draw_vial_body(img, (28, 24, 44))
    # 遮光材质：外沿更暗，像是吸住光
    for y in range(6, 14):
        put(img, 4, y, (18, 16, 30))
        put(img, 11, y, (18, 16, 30))
    for y in range(2, 5):
        put(img, 6, y, (40, 36, 60))
    put(img, 5, 7, (72, 64, 104))
    put(img, 5, 8, (60, 54, 88))
    return img


def make_dead_matter() -> Image.Image:
    """死寂物质：灰白色的斑驳石块，没有粒子感。"""
    img = new_image()
    base = (150, 150, 152)
    light = (184, 184, 186)
    dark = (110, 110, 114)
    speckle = (128, 128, 132)

    for y in range(16):
        for x in range(16):
            put(img, x, y, base)

    # 顶面亮、底面暗
    for x in range(16):
        put(img, x, 0, light)
        put(img, x, 15, dark)
    for y in range(16):
        put(img, 0, y, light)
        put(img, 15, y, dark)

    # 斑驳
    spots = [
        (2, 3), (4, 4), (7, 2), (11, 3), (13, 5),
        (3, 7), (6, 8), (9, 7), (12, 9), (5, 11),
        (8, 12), (11, 13), (2, 12), (14, 10), (9, 4),
    ]
    for x, y in spots:
        put(img, x, y, speckle)
    cracks = [(6, 5, 6, 6), (10, 10, 11, 10), (4, 9, 4, 10)]
    for x1, y1, x2, y2 in cracks:
        put(img, x1, y1, dark)
        put(img, x2, y2, dark)

    return img


def save(img: Image.Image, relative_path: str) -> None:
    full = os.path.join(ASSETS, relative_path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)
    print("  wrote", os.path.relpath(full, ROOT))


def main() -> None:
    print("生成占位纹理 ->", ASSETS)
    save(make_vial(), os.path.join("textures", "item", "element_vial.png"))
    save(make_prism(), os.path.join("textures", "item", "refraction_prism.png"))
    save(make_shadow_bottle(), os.path.join("textures", "item", "shadow_bottle.png"))
    save(make_dead_matter(), os.path.join("textures", "block", "dead_matter.png"))

    # 顺带为每种元素各存一份，阶段 02 做「按元素着色」时可以直接用
    extra = os.path.join("textures", "item", "vial")
    print("生成元素配色样张 ->", extra)
    for name, color in ELEMENT_COLORS.items():
        save(make_vial(color), os.path.join(extra, f"{name}.png"))


if __name__ == "__main__":
    main()
