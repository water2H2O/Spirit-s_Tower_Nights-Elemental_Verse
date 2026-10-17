# 素材补充 · 聚影瓶内容细化

按 2026-10-17 的《素材需求清单》制作。本次仅调整 `bottle_shadow.png` 与 `bottle_shadow_essence.png` 的内容效果。暗为连续的紫色悬浮雾团，影为低位蓝灰凝影沉积面；减少零碎高光。空聚影瓶继续沿用原图，瓶塞、瓶颈、外轮廓与前景玻璃反光均保留。其余 10 张贴图沿用已细化的小包与仓储版本。素材为原生 16×16 RGBA，二值透明，无抗锯齿、平滑缩放和随机噪声。

## 新增素材

| 类型 | 文件 | 数量 |
| --- | --- | --- |
| 物品 | `dead_dust.png` | 1 |
| 折射棱镜内容状态 | `prism_light.png`、`prism_dawn.png` | 2 |
| 聚影瓶内容状态 | `bottle_shadow.png`、`bottle_shadow_essence.png` | 2 |
| 便携容器 | `element_pouch.png` | 1 |
| 石制密封仓 | `sealed_vault_side.png`、`sealed_vault_top.png`、`sealed_vault_front.png` | 3 |
| 符文柜 | `rune_cabinet_side.png`、`rune_cabinet_top.png`、`rune_cabinet_front.png` | 3 |

前六张位于 `assets/elemental_verse/textures/item/`，后六张位于 `assets/elemental_verse/textures/block/`。

死寂尘是浅色松散颗粒堆，与之前的死寂物质薄片图标是两份不同素材。棱镜和聚影瓶状态沿用已选定的容器轮廓。小包露出瓶口与浅蓝冷却剂，卷边袋口遮住瓶子下半部分；本轮细化：石仓增加石盖倒角、压紧夹片、连续密封槽、凹入的石板门和八角铅封压印。符文柜增加柜门厚度、铰链、锁扣、铜护角与内凹顶板。符文储能区域仍为原来的 25 个像素，掩膜文件与上一版完全一致。

## 接入

将 `assets/elemental_verse/` 按路径合并到模组的 `src/main/resources/`。本包只包含新增素材，配合前一版使用，不覆盖已有锥形元素瓶、空棱镜、空聚影瓶和死寂物质方块。

6 个新增物品模型使用传统 `minecraft:item/generated`。两个仓储方块附 `minecraft:block/orientable` 模型与四向 `facing` 方块状态示例。仅需三张纹理，底面复用顶面，背面复用侧面。

光/晨在棱镜中的模型映射为 `elemental_verse:item/prism_light` / `prism_dawn`；暗/影在聚影瓶中对应 `elemental_verse:item/bottle_shadow` / `bottle_shadow_essence`。按数据组件切换模型以及新版物品定义需按实际 MC 版本接入。

符文柜的储能区域掩膜见 `source/masks/rune_cabinet_charge.png`，坐标与说明见 `source/rune_charge_region.json`。只调整掩膜内像素即可表达亮度；普通 PNG 与该掩膜不会自动产生动态发光或改变光照，运行时逻辑由模组实现。预览里的 4 档亮度只是效果示意。

## 编辑与验证

`source/pixels.json` 是 12 张图的逐像素源，编辑后用 `source/render_pixels.py` 导出。`source/supplement_art.py` 是绘图逻辑，`source/build_supplement.py` 可重建原稿、模型和预览；需要 Python 和 Pillow。重建会覆盖字符源与最终贴图。容器底图保存在 `source/bases/`，只用于源文件重建。

遵循 https://blockbench.net/wiki/guides/minecraft-style-guide/ ，按用户提供的 `minecraft-compat-pixel-art` skill 以整数坐标绘制。本次实际查看过的参考包括 Ender IO 袋子、Create 箱体和农夫乐事柜门；只提取轮廓组织、材质色阶和结构方式，新增图标均重新绘制。参考路径见 `source/reference_sources.json`。

已检查尺寸、透明度、模型路径、像素源重建、棱镜与聚影瓶的轮廓一致性和符文掩膜边界，并实际查看原尺寸及放大预览。未运行 Minecraft 客户端。结果见 `validation.json`。
