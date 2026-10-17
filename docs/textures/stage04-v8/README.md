# 阶段 04 · 申明板与仪式台 · v8 细化版

按 2026-10-18 的《素材需求清单》补齐当前所需 6 张贴图。本次细化全部六张：申明板增加厚度、倒角与刻纹凹刻层次；仪式台增加角片、浮雕、底座及投入口明暗；GUI 增加内凹工作面、角片和分隔压线。槽位、文字留白、顶面分区与染色区域坐标全部沿用上一版。音效不在本次范围；可后补的粒子、密封仓/小包界面、建材变体也不包含。

## 内容

- `assets/elemental_verse/textures/item/declaration_plate.png`：空申明板，16×16。
- `assets/elemental_verse/textures/item/declaration_plate_written.png`：已写申明板，16×16。
- `assets/elemental_verse/textures/block/ritual_altar_side.png`：仪式台侧面，16×16。
- `assets/elemental_verse/textures/block/ritual_altar_top.png`：仪式台顶面，16×16。
- `assets/elemental_verse/textures/block/ritual_altar_front.png`：仪式台正面，16×16。
- `assets/elemental_verse/textures/gui/ritual_altar.png`：仪式台界面，原生 **176×166**，不是 256×256。

全部为 RGBA PNG，透明度仅 0/255，无抗锯齿、平滑缩放或随机噪声。物品和方块直接在 16×16 网格绘制；GUI 在原生 176×166 画布按像素绘制。

## 已写板染色

已写板采用通用刻纹底版，中央 `[x=6,y=6,width=4,height=4]` 的 16 像素为中性灰，仅此区域参与元素染色。掩膜为 `source/masks/declaration_plate_tint.png`，完整坐标和 RGB 乘色说明见 `source/declaration_tint_region.json`。

PNG 本身不会自动染色。代码应只处理指定区域；如果使用分层物品模型，把中央区域拆出为独立 layer 再设置 tintindex，避免将整个石板染色。没有生成 17 张元素版本。

## 顶面

中央长槽放申明板，后侧左/右和前侧中各一个小坑接收元素。顶面前方为 y=15，后方为 y=0；分区见 `source/ritual_altar_top_layout.json`。图中的槽为视觉凹陷，未改变方块几何或命中检测。

## GUI 坐标

元素内容原点 `(62,17)`、`(80,17)`、`(98,17)`；申明板 `(62,35)`；产出 `(98,35)`。所有槽内容为 16×16，对应凹框从 `(x-1,y-1)` 开始，大小 18×18。

按 176×166 标准容器布局绘制 27 格背包：起点 `(8,84)`、间距 18；快捷栏起点 `(8,142)`、间距 18。这是清单中“背包与原版箱子一致”的具体布局假设。

标题留白 `[8,5,160,9]`；“形：释放 · 发射”留白 `[8,58,160,12]`；背包标签留白 `[8,72,160,9]`。最终 PNG 不含任何文字、物品、数字或槽内图标。预览中有一张填充示例，示例内容不会进入游戏贴图。

**绘制时纹理宽高参数须使用 176、166**；不能继续按默认 256、256 的 UV 图集采样。所有 41 个槽框与留白坐标见 `source/ritual_altar_gui_layout.json`。

## 接入与编辑

ZIP 内以 `assets/elemental_verse/` 为根资源路径，方便按项目已有集成工具导入。本包只交贴图，不包含会覆盖项目关键文件的模型、方块状态、分发或语言 JSON；未执行清单中的仓库集成命令。

`source/pixels.json` 保存所有原生像素网格，包含 GUI。编辑后用 `source/render_pixels.py` 导出，依赖 Python 与 Pillow。`source/stage04_art.py` 为绘图源，`source/build_stage04.py` 可重建原稿、坐标与预览；重建会覆盖字符源与 PNG。

参考采用先前认可的石仓/符文柜材质色阶、农夫乐事容器界面的像素槽框，以及新生魔艺装置和通用机械平板的材质分层方式。实际查看的参考路径见 `source/reference_sources.json`。未复制参考 PNG；代码以整数坐标重新绘制，遵循 https://blockbench.net/wiki/guides/minecraft-style-guide/ 和用户提供的 `minecraft-compat-pixel-art` skill。

验证范围为 PNG 尺寸、二值透明度、染色掩膜、全部槽内容与凹框的精确位置、文字区域留白、源文件可重建性和实际视觉预览。未运行 Minecraft 客户端，也未测试目标仓库的导入脚本。
