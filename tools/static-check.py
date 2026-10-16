"""静态自查 —— 在编译之前把「能自动验的」都验掉。

为什么需要它：写这个模组的环境**没有网络、没有 Gradle 缓存、没有 JAVA_HOME**，
跑不了 `gradlew build`。所以把「编译能发现的那部分错误」提前到这里抓住：

  1. 所有资源 JSON 是否合法（语法错了在游戏里只表现为「资源包加载失败」，很难查）
  2. Java 里用到的翻译键是否都在 zh_cn.json 与 en_us.json 里
  3. 两个语言文件的键是否完全一致（漏一个是常见疏漏）
  4. 模型 JSON 引用的纹理文件是否真的存在（引用了不存在的纹理 → 紫黑格）
  5. blockstate 引用的模型是否存在
  6. 方块/物品的注册名与 blockstate / 模型 / 掉落表文件名是否对得上
  7. Java 源码里的基本配平（大括号、圆括号）——抓不住类型错误，但能抓住手滑

用法：
    python tools/static-check.py

退出码 0 = 全部通过；1 = 有问题（问题会逐条打印）。
"""

from __future__ import annotations

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "src", "main")
JAVA = os.path.join(SRC, "java")
RES = os.path.join(SRC, "resources")
ASSETS = os.path.join(RES, "assets", "elemental_verse")
DATA = os.path.join(RES, "data", "elemental_verse")
NS = "elemental_verse"

problems: list[str] = []
notes: list[str] = []


def fail(msg: str) -> None:
    problems.append(msg)


def ok(msg: str) -> None:
    notes.append(msg)


def read_json(path: str):
    try:
        with open(path, "r", encoding="utf-8") as handle:
            return json.load(handle)
    except json.JSONDecodeError as exc:
        fail(f"JSON 语法错误：{os.path.relpath(path, ROOT)} -> {exc}")
    except OSError as exc:
        fail(f"读不到文件：{os.path.relpath(path, ROOT)} -> {exc}")
    return None


def walk(root: str, suffix: str):
    for base, _, files in os.walk(root):
        for name in files:
            if name.endswith(suffix):
                yield os.path.join(base, name)


def java_files():
    return list(walk(JAVA, ".java"))


# ----------------------------------------------------------------- 1. JSON 合法性

def check_json_syntax() -> None:
    count = 0
    for path in walk(RES, ".json"):
        if read_json(path) is not None:
            count += 1
    ok(f"资源 JSON 全部合法（{count} 个）")


# ----------------------------------------------------------------- 2 & 3. 翻译键

TRANSLATION_CALLS = re.compile(r'"(item|block|itemGroup|tooltip|element|elemental_verse)[\w.]*\.[\w.]+"')
PREFIXED = re.compile(r'"((?:item|block|itemGroup|tooltip|element|elemental_verse)\.[^"]+)"')


def collect_java_keys() -> set[str]:
    """把 Java 里以字面量出现的键收集起来。

    注意：EVLang 里有 `"tooltip.elemental_verse.stage." + id` 这种**前缀**，
    它们不是完整的键（末尾带点），会被过滤掉 —— 那部分交给
    check_translations 里的元素/阶段循环单独验。
    """
    keys: set[str] = set()
    for path in java_files():
        with open(path, "r", encoding="utf-8") as handle:
            text = handle.read()
        for match in PREFIXED.finditer(text):
            key = match.group(1)
            if key.endswith("."):
                continue  # 拼接用的前缀，不是完整键
            keys.add(key)
    return keys


def check_translations() -> None:
    zh = read_json(os.path.join(ASSETS, "lang", "zh_cn.json"))
    en = read_json(os.path.join(ASSETS, "lang", "en_us.json"))
    if zh is None or en is None:
        return

    zh_keys, en_keys = set(zh), set(en)

    missing_en = sorted(zh_keys - en_keys)
    missing_zh = sorted(en_keys - zh_keys)
    for key in missing_en:
        fail(f"en_us.json 缺键：{key}")
    for key in missing_zh:
        fail(f"zh_cn.json 缺键：{key}")
    if not missing_en and not missing_zh:
        ok(f"两个语言文件的键完全一致（各 {len(zh_keys)} 条）")

    # Java 里直接写出来的键必须存在
    for key in sorted(collect_java_keys()):
        if key not in zh:
            fail(f"Java 里用到但 zh_cn.json 没有的键：{key}")

    # 元素与瓶名的键必须齐 —— 由枚举驱动，漏一个就是显示 raw key
    element_ids = collect_element_ids()
    for element in element_ids:
        for key in (f"element.{NS}.{element}", f"item.{NS}.element_vial.{element}"):
            if key not in zh:
                fail(f"元素 {element} 缺翻译键：{key}")

    for stage in ("stable", "derived", "dead", "empty"):
        key = f"tooltip.{NS}.stage.{stage}"
        if key not in zh:
            fail(f"衰变阶段的 tooltip 缺键：{key}")


def collect_element_ids() -> list[str]:
    """从 ElementType.java 里抓出枚举的 id 字符串（第一个构造参数）。"""
    path = os.path.join(JAVA, "com", "water2h2o", "elementalverse", "element", "ElementType.java")
    if not os.path.exists(path):
        return []
    ids: list[str] = []
    with open(path, "r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line.startswith("//") or line.startswith("*") or line.startswith("/*"):
                continue
            match = re.match(r'^[A-Z_]+\(\s*"([a-z_]+)"', line)
            if match:
                ids.append(match.group(1))
    return ids


# ----------------------------------------------------------------- 4 & 5. 模型与纹理引用

def check_model_references() -> None:
    for path in walk(os.path.join(ASSETS, "models"), ".json"):
        model = read_json(path)
        if not isinstance(model, dict):
            continue

        parent = model.get("parent")
        if isinstance(parent, str) and parent.startswith(NS + ":"):
            rel = parent.split(":", 1)[1]
            target = os.path.join(ASSETS, "models", *rel.split("/")) + ".json"
            if not os.path.exists(target):
                fail(f"{os.path.relpath(path, ROOT)} 的 parent 不存在：{parent}")

        for slot, value in (model.get("textures") or {}).items():
            if not isinstance(value, str):
                continue
            if "#" in value or not value.startswith(NS + ":"):
                continue
            rel = value.split(":", 1)[1]
            target = os.path.join(ASSETS, "textures", *rel.split("/")) + ".png"
            if not os.path.exists(target):
                fail(f"{os.path.relpath(path, ROOT)} 的纹理 {slot} 不存在：{value}")

    for path in walk(os.path.join(ASSETS, "blockstates"), ".json"):
        state = read_json(path)
        if not isinstance(state, dict):
            continue
        for variant_name, variant in (state.get("variants") or {}).items():
            entries = variant if isinstance(variant, list) else [variant]
            for entry in entries:
                model_name = (entry or {}).get("model")
                if not isinstance(model_name, str):
                    continue
                if not model_name.startswith(NS + ":"):
                    continue
                rel = model_name.split(":", 1)[1]
                target = os.path.join(ASSETS, "models", *rel.split("/")) + ".json"
                if not os.path.exists(target):
                    fail(f"{os.path.relpath(path, ROOT)} 的变体 {variant_name} 引用了不存在的模型：{model_name}")

    ok("模型 / 纹理 / blockstate 的引用全部存在")


# ----------------------------------------------------------------- 6. 注册名一致性

def collect_deferred_names(java_name: str, pattern: str) -> set[str]:
    path = os.path.join(JAVA, "com", "water2h2o", "elementalverse", "registry", java_name)
    if not os.path.exists(path):
        fail(f"找不到注册类：{java_name}")
        return set()
    with open(path, "r", encoding="utf-8") as handle:
        text = handle.read()
    return set(re.findall(pattern, text))


def check_registry_names() -> None:
    block_names = collect_deferred_names("EVBlocks.java", r'register\(\s*"([a-z_]+)"')
    item_names = collect_deferred_names("EVItems.java", r'register\(\s*"([a-z_]+)"')

    if not block_names:
        fail("EVBlocks.java 里没解析出任何注册名 —— 检查注册写法是否变了")
    if not item_names:
        fail("EVItems.java 里没解析出任何注册名 —— 检查注册写法是否变了")

    for name in sorted(block_names):
        for rel in (("blockstates", f"{name}.json"),
                    ("models", "block", f"{name}.json"),
                    ("models", "item", f"{name}.json"),
                    ("textures", "block", f"{name}.png")):
            target = os.path.join(ASSETS, *rel)
            if not os.path.exists(target):
                fail(f"方块 {name} 缺资源：assets/{'/'.join(rel)}")
        loot = os.path.join(DATA, "loot_table", "blocks", f"{name}.json")
        if not os.path.exists(loot):
            fail(f"方块 {name} 缺掉落表：data/{NS}/loot_table/blocks/{name}.json")

    for name in sorted(item_names):
        target = os.path.join(ASSETS, "models", "item", f"{name}.json")
        if not os.path.exists(target):
            fail(f"物品 {name} 缺模型：assets/{NS}/models/item/{name}.json")

    ok(f"注册名与资源对得上（方块 {len(block_names)} 个，物品 {len(item_names)} 个）")

    # 掉落表里引用的物品必须真的注册过
    for path in walk(os.path.join(DATA, "loot_table"), ".json"):
        table = read_json(path)
        if not isinstance(table, dict):
            continue
        for pool in table.get("pools") or []:
            for entry in pool.get("entries") or []:
                entry_name = entry.get("name") if isinstance(entry, dict) else None
                if isinstance(entry_name, str) and entry_name.startswith(NS + ":"):
                    short = entry_name.split(":", 1)[1]
                    if short not in item_names:
                        fail(f"{os.path.relpath(path, ROOT)} 掉落了未注册的物品：{entry_name}")

    # 配方里引用的物品必须真的注册过
    for path in walk(os.path.join(DATA, "recipe"), ".json"):
        recipe = read_json(path)
        if not isinstance(recipe, dict):
            continue
        result = (recipe.get("result") or {}).get("id")
        if isinstance(result, str) and result.startswith(NS + ":"):
            short = result.split(":", 1)[1]
            if short not in item_names:
                fail(f"{os.path.relpath(path, ROOT)} 产出了未注册的物品：{result}")


# ----------------------------------------------------------------- 7. Java 基本配平

def check_java_balance() -> None:
    for path in java_files():
        with open(path, "r", encoding="utf-8") as handle:
            text = handle.read()
        # 去掉字符串字面量与注释再数括号，避免误报
        stripped = re.sub(r'"""[\s\S]*?"""', '""', text)
        stripped = re.sub(r'"(\\.|[^"\\])*"', '""', stripped)
        stripped = re.sub(r"//[^\n]*", "", stripped)
        stripped = re.sub(r"/\*[\s\S]*?\*/", "", stripped)
        if stripped.count("{") != stripped.count("}"):
            fail(f"{os.path.relpath(path, ROOT)} 大括号不配平：{{={stripped.count('{')} }}={stripped.count('}')}")
        if stripped.count("(") != stripped.count(")"):
            fail(f"{os.path.relpath(path, ROOT)} 圆括号不配平：(={stripped.count('(')} )={stripped.count(')')}")

    ok(f"Java 源码括号配平（{len(java_files())} 个文件）")


# ----------------------------------------------------------------- 8. 常见疏漏

def check_misc() -> None:
    # mods.toml 里 logoFile 指向的文件必须存在
    toml_path = os.path.join(SRC, "templates", "META-INF", "neoforge.mods.toml")
    if os.path.exists(toml_path):
        with open(toml_path, "r", encoding="utf-8") as handle:
            toml = handle.read()
        match = re.search(r'^logoFile\s*=\s*"([^"]+)"', toml, re.MULTILINE)
        if match:
            logo = os.path.join(RES, match.group(1))
            if not os.path.exists(logo):
                fail(f"mods.toml 的 logoFile 指向不存在的文件：{match.group(1)}")
            else:
                ok(f"模组图标存在：{match.group(1)}")

    # gradle.properties 的 mod_id 必须与主类里的一致
    props = os.path.join(ROOT, "gradle.properties")
    if os.path.exists(props):
        with open(props, "r", encoding="utf-8") as handle:
            text = handle.read()
        match = re.search(r"^mod_id=(\S+)$", text, re.MULTILINE)
        if match and match.group(1) != NS:
            fail(f"gradle.properties 的 mod_id={match.group(1)} 与命名空间 {NS} 不一致")


def main() -> int:
    check_json_syntax()
    check_translations()
    check_model_references()
    check_registry_names()
    check_java_balance()
    check_misc()

    print("=" * 68)
    print("静态自查报告")
    print("=" * 68)
    for line in notes:
        print(f"  [OK] {line}")
    if problems:
        print()
        for line in problems:
            print(f"  [!!] {line}")
        print()
        print(f"发现 {len(problems)} 个问题。")
        return 1
    print()
    print("全部通过。可以提交，交给 CI / 本地编译了。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
