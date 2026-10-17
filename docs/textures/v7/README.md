# 阶段 01 · 锥形元素瓶与层叠灰渣

24 张原生 16×16 静态 PNG：空瓶与 17 种元素状态、折射棱镜、聚影瓶、死寂物质物品，以及死寂物质方块的侧/顶/底面。

## 本版外观

- 18 种元素瓶状态改用锥形瓶：短直瓶颈、连续斜肩、宽平底；玻璃仍采用三种颜色和少量反光。
- 元素内容延续第一版的配色与形态。水、晨、影的底部略作延展以贴合锥形瓶底；其余内容物保留原像素结构。
- 死寂物质物品采用用户选定的“更像灰渣：薄片堆叠、边缘破碎”：三片薄灰渣错开叠放，用五种哑灰色区分片面与薄断边。
- 死寂物质方块保留第一版三张原图；折射棱镜和聚影瓶也沿用第一版。
- 所有图像直接在 16×16 网格绘制；RGBA 透明度仅 0/255；没有抗锯齿、平滑缩放或随机噪声。预览为最近邻整数倍放大。

## 使用

把 `assets/elemental_verse/` 按路径合并到模组的 `src/main/resources/`。

默认瓶子为 `textures/item/element_vial.png`，装载元素为 `element_vial_<组件值>.png`。这是同一物品的 18 种状态，模型映射见 `element_state_models.json`。

包内含传统物品模型、死寂物质方块模型与方块状态。按自定义数据组件选择物品模型仍需按目标 Minecraft 版本在客户端接入；仅复制素材不会自动切换元素图标。本包不附带猜测版本的 `pack.mcmeta`。

## 编辑

`source/pixels.json` 是 24 张贴图的完整逐像素源；`.` 表示透明，每张图的字符网格均为 16×16。编辑后运行 `source/render_pixels.py` 导出 PNG，依赖 Python 和 Pillow。

`source/draw_originals.py` 是本版绘图与预览生成器；运行它会还原本版原稿并覆盖像素源和 PNG。

`source/layers/` 包含锥形瓶玻璃层和 17 个元素内容层。先绘制内容层，再叠加玻璃层即可重建图标；对应关系和拟合说明见 `source/layer_composition.json`。

## 参考与验证

遵循 https://blockbench.net/wiki/guides/minecraft-style-guide/ ，并按用户提供的 `minecraft-compat-pixel-art` skill 使用原生整数像素绘制。原始参考库中实际查看过的贴图路径保存在 `source/reference_sources.json`；本包没有复制原模组 PNG。

已检查原生尺寸、二值透明度、模型纹理路径、像素源导出一致性、分层合成、容器边界和改动范围。已查看原尺寸、最近邻放大图和整组预览。没有运行 Minecraft 客户端。检查结果见 `validation.json`。
