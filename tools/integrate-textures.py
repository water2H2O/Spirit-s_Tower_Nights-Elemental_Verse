"""把作者发来的素材包接入模组（一步到位）。

踩过的坑，全部固化进这个流程：
    1. 素材包里也带 `models/item/element_vial.json`，直接复制会覆盖我们生成的
       `overrides` 分发段 → 所有元素瓶图标退化成一个，且**没有任何报错**。
    2. 素材包里常带"平面图标版"的 `models/item/<方块>.json`，会把方块的立体渲染
       盖成平面图 → 物品栏里跟别的方块不一样。
    3. 元素瓶原稿顶部贴边、瓶口没塞子 → 每次接入后都要重新应用修图，
       不然新素材一来，之前修的就白做了。

固定顺序：
    解压 → 复制资源 → 载体模型落成 <载体>_base → 重新生成分发
         → 方块物品模型指回方块模型 → 修元素瓶贴图 → 归档整包

用法：
    python tools/integrate-textures.py "<素材包.zip>" v8
    python tools/integrate-textures.py "<素材包.zip>" v8 --keep-art
        （--keep-art：作者已自己画好留白与瓶塞时，跳过修图）
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

NS = "elemental_verse"

# 三种元素载体：作者的模型会被保存成 <载体>_base.json
CARRIERS = {
    "element_vial": f"{NS}:item/element_vial",
    "refraction_prism": f"{NS}:item/refraction_prism",
    "shadow_bottle": f"{NS}:item/shadow_bottle",
}

# 这些方块的物品模型必须 parent 到方块模型（否则物品栏里是平面图标）
BLOCK_ITEMS = ("dead_matter", "sealed_vault")


def write_json(path: str, payload: dict) -> None:
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def run_tool(script: str) -> int:
    # encoding 必须显式给：Windows 上默认是 GBK，读子进程的 UTF-8 中文输出会崩
    result = subprocess.run([sys.executable, os.path.join(ROOT, "tools", script)],
                            capture_output=True, text=True,
                            encoding="utf-8", errors="replace")
    if (result.stdout or "").strip():
        print(result.stdout.strip())
    if result.returncode != 0:
        print((result.stderr or "").strip())
    return result.returncode


def main() -> int:
    fix_art = "--fix-art" in sys.argv
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    if len(args) < 2:
        print(__doc__)
        return 1

    zip_path, version = args[0], args[1]
    if not os.path.exists(zip_path):
        print("找不到 zip:", zip_path)
        return 1

    work = tempfile.mkdtemp(prefix="ev-tex-")
    with zipfile.ZipFile(zip_path) as archive:
        archive.extractall(work)

    roots = [e for e in os.listdir(work) if os.path.isdir(os.path.join(work, e))]
    pack_root = os.path.join(work, roots[0]) if len(roots) == 1 else work
    pack_assets = os.path.join(pack_root, "assets", "elemental_verse")
    if not os.path.isdir(pack_assets):
        print("这个包里没有 assets/elemental_verse")
        return 1

    # ① 复制资源（覆盖同名文件）
    shutil.copytree(pack_assets, ASSETS, dirs_exist_ok=True)
    print("① 资源已复制")

    # ② 作者的载体模型 -> <载体>_base.json
    for carrier, texture in CARRIERS.items():
        write_json(os.path.join(MODELS, f"{carrier}_base.json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": texture},
        })
    print("② 载体基底模型已就位")

    # ③ 重建带 overrides 的分发模型
    if run_tool("generate-vial-models.py") != 0:
        print("生成分发模型失败")
        return 1
    print("③ 分发模型已重建")

    # ④ 方块物品模型指回方块模型
    for block in BLOCK_ITEMS:
        write_json(os.path.join(MODELS, f"{block}.json"), {"parent": f"{NS}:block/{block}"})
    print("④ 方块物品模型已指回方块模型")

    # ⑤ 元素瓶贴图：顶部留白 + 瓶塞
    if not fix_art:
        print("⑤ 保留作者原稿贴图（需要留白+瓶塞时加 --fix-art）")
    elif run_tool("fix-item-icon-margin.py") != 0:
        print("贴图修正失败")
        return 1
    else:
        print("⑤ 元素瓶贴图已修正")

    # ⑥ 归档整包
    archive_dir = os.path.join(ROOT, "docs", "textures", version)
    if os.path.isdir(archive_dir):
        shutil.rmtree(archive_dir)
    shutil.copytree(pack_root, archive_dir)
    print(f"⑥ 已归档到 docs/textures/{version}")

    print("\n接入完成。下一步：python tools/static-check.py，然后 dev.cmd build")
    return 0


if __name__ == "__main__":
    sys.exit(main())
