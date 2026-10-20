"""状态图标生成器（18×18，Minecraft 状态图标规格）。

用法：
    python tools/gen_effect_icons.py

输出：
    docs/textures/effect-icons/<名称>.png
    docs/textures/effect-icons/preview-8x.png
"""

import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/textures/effect-icons"

PALETTES = {
    # 缚手：水蓝绳结
    "bound_hands": ["#0C2A55", "#1F5C9E", "#49A6DE", "#C6EEFF"],
    # 易伤：裂纹护甲片
    "vulnerable": ["#3A0A0A", "#7E1414", "#B83232", "#FF9A8A"],
}

SPRITES = {
    # 缚手：一只手的轮廓，被水带横向缠住（带子最亮，重点在读得出"绑"）
    "bound_hands": [
        "..................",
        "..................",
        "......2222........",
        ".....233332.......",
        "....23333332......",
        "....23344332......",
        "....23443332......",
        "...2233333322.....",
        "..222222222222....",
        "..244444444442....",
        "..233333333332....",
        "..233333333332....",
        "..222222222222....",
        "....23333332......",
        "....23333332......",
        ".....222222.......",
        "..................",
        "..................",
    ],
    # 易伤：护甲片从中间裂开
    "vulnerable": [
        "..................",
        "....22222222......",
        "...2333333332.....",
        "..233444444332....",
        "..234443344432....",
        "..234432223432....",
        "..234322112432....",
        "..234322112432....",
        "..234432113432....",
        "..234443114432....",
        "..234444114432....",
        "..233444114332....",
        "..233344114332....",
        "..223334112332....",
        "...2233112232.....",
        "....22111222......",
        ".....111122.......",
        "..................",
    ],
}


def render(rows, palette):
    assert len(rows) == 18, f"需要 18 行，实际 {len(rows)}"
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == 18, f"第 {y} 行长度 {len(row)} != 18"
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            c = palette[int(ch) - 1]
            px[x, y] = (int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16), 255)
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    imgs = {}
    for name, rows in SPRITES.items():
        img = render(rows, PALETTES[name])
        img.save(OUT / f"{name}.png")
        imgs[name] = img
        print("ok", name)

    scale, pad = 8, 8
    cell = 18 * scale + pad * 2
    board = Image.new("RGBA", (cell * len(imgs), cell), (24, 24, 28, 255))
    for i, img in enumerate(imgs.values()):
        big = img.resize((18 * scale, 18 * scale), Image.Resampling.NEAREST)
        board.alpha_composite(big, (i * cell + pad, pad))
    board.save(OUT / "preview-8x.png")
    print("preview ->", OUT / "preview-8x.png")


if __name__ == "__main__":
    main()
