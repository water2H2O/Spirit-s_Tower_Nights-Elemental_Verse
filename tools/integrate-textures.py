"""把作者发来的素材包接入模组（一步到位，避免踩重复的坑）。

踩过的坑：素材包里也带 `models/item/element_vial.json` 这类文件，
直接复制会把我们生成的 `overrides` 分发段覆盖掉 ——
表现是**所有元素瓶图标变成一个**，而且没有任何报错。

所以接入要按这个顺序做：
    1. 解压
    2. 把 `assets/elemental_verse/**` 复制进模组
    3. 把三个**载体模型**改名成 `<载体>_base.json`（作者的稿归它们）
    4. 重跑 generate-vial-models.py，重新生成带 overrides 的分发模型
    5. 归档整包到 docs/textures/<版本>/

用法：
    python tools/integrate-textures.py "C:/Users/.../elemental_verse_textures_v8.zip" v8
"""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "elemental_verse")
MODELS = os.path.join(ASSETS, "models", "item")
CARRIERS = ("element_vial", "refraction_prism", "shadow_bottle")

# 这些方块的物品模型必须 parent 到方块模型（否则物品栏里是平面图标，和原版方块不一致）
BLOCK_ITEMS = ("dead_matter", "sealed_vault")

NS = "elemental_verse"
CARRIER_TEXTURE = {
    "element_vial": f"{NS}:item/element_vial",
    "refraction_prism": f"{NS}:item/refraction_prism",
    "shadow_bottle": f"{NS}:item/shadow_bottle",
}


def write_json(path: str, payload: dict) -> None:
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 1

    zip_path, version = sys.argv[1], sys.argv[2]
    if not os.path.exists(zip_path):
        print("找不到 zip:", zip_path)
        return 1

    work = tempfile.mkdtemp(prefix="ev-tex-")
    with zipfile.ZipFile(zip_path) as archive:
        archive.extractall(work)

    # 包里通常套一层目录
    roots = [entry for entry in os.listdir(work) if os.path.isdir(os.path.join(work, entry))]
    pack_root = os.path.join(work, roots[0]) if len(roots) == 1 else work
    pack_assets = os.path.join(pack_root, "assets", "elemental_verse")
    if not os.path.isdir(pack_assets):
        print("这个包里没有 assets/elemental_verse")
        return 1

    # 1) 复制资源（会覆盖同名文件）
    shutil.copytree(pack_assets, ASSETS, dirs_exist_ok=True)
    print("已复制 assets")

    # 2) 作者的载体模型 -> <载体>_base.json（保住它们，不被分发覆盖）
    for carrier in CARRIERS:
        source = os.path.join(MODELS, f"{carrier}.json")
        if os.path.exists(source):
            write_json(os.path.join(MODELS, f"{carrier}_base.json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": CARRIER_TEXTURE[carrier]},
            })
            print(f"  {carrier}_base.json 就位")

    # 2.5) 方块物品模型必须指回方块模型（和原版方块一样的立体渲染）
    #      素材包里常带一份"平面图标版"的 models/item/<方块>.json，会把这里盖掉，
    #      表现是方块在物品栏里变成一个平面块、跟其它方块不一样。
    for block in BLOCK_ITEMS:
        write_json(os.path.join(MODELS, f"{block}.json"), {
            "parent": f"{NS}:block/{block}",
        })
        print(f"  {block}.json 指回方块模型")

    # 3) 重新生成分发模型
    subprocess.run([sys.executable, os.path.join(ROOT, "tools", "generate-vial-models.py")], check=True)

    # 4) 归档整包
    archive_dir = os.path.join(ROOT, "docs", "textures", version)
    if os.path.isdir(archive_dir):
        shutil.rmtree(archive_dir)
    shutil.copytree(pack_root, archive_dir)
    print(f"已归档到 docs/textures/{version}")

    print("\n接入完成。下一步：python tools/static-check.py 然后 dev.cmd build")
    return 0


if __name__ == "__main__":
    sys.exit(main())
