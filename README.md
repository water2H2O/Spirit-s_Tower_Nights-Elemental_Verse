# Spirit's Tower Nights: Elemental Verse
# 水塔夜谈 · 元素之诗

> 你的箱子里，正安静地发生一场灾难。

土会干成砂，水会凝成冰，光会在你睡着的时候悄悄熄灭。
不是被谁偷走 —— 是它们本来就留不住。

元素会衰变，而且**不可逆**：稳定元素 → 衍生元素 → 死寂物质。
亮 → 暗 → 灰。走到最后，只剩一块什么都不是的石头。

于是你第一次开始算计：这一箱该现在用掉，还是塞进密封仓里再放几天。
**你的节奏，就是你的仓库。**

而这个世界还有下半句 ——
**你用的每一分魔法，都在让世界变脏。**

```
模组 ID    elemental_verse
NeoForge   21.1.262
Minecraft  1.21.1
Java       21
作者       Water2H2O
```

---

## 这是什么

《水塔夜谈》世界观的 Minecraft 模组。以**新物品 + 新生态**为主，
三条主系统按顺序登场，形成压力递进：

| 阶段 | 系统 | 玩家感受 |
| --- | --- | --- |
| 前期 | **衰变** | 我存的东西在坏 —— 得管好仓储与节奏 |
| 中期 | **瘴素** | 我住的地方在变脏 —— 得学会抑制与共存 |
| 中期后半 | **晶裔** | 有邻居搬来了 —— 它不打我，但它会占我的地 |
| 后期 | **神器 / 征服** | 我攒够了 —— 可以从"忍"翻成"赢" |

**当前进度：阶段 01（衰变系统）。** 元素会坏、会变成死寂物质，仓储与节奏已经成立；
施法、污染、晶裔还在路上。

**范围红线**：不做人物、不做势力、不做任务线。世界只由物品、方块、生物群系与生态构成。

**完整设计见** [`docs/策划案-总纲.md`](docs/策划案-总纲.md)，
分步施工见 [`docs/开发路线图.md`](docs/开发路线图.md)。

---

## 环境要求

| 需要 | 版本 | 备注 |
| --- | --- | --- |
| **JDK** | **21** | 1.21.1 必须 Java 21。见下方"没有 JDK 怎么办" |
| 网络 | 首次编译需要 | 要下载 NeoForge 与 Minecraft 依赖，约 1–2 GB |
| 磁盘 | 约 3 GB | Gradle 缓存 + 构建产物 |

### 没有 JDK 怎么办

**这台机器上 `.minecraft` 里已经自带一个完整的 JDK 21**（微软 OpenJDK 21.0.7，含 `javac`）：

```
C:\Users\<你>\AppData\Roaming\.minecraft\runtime\java-runtime-delta
```

`dev.cmd` 会自动找到它，**不需要另外安装 Java**。

---

## 编译与运行

全程用 `dev.cmd`（在仓库根目录）：

```cmd
dev.cmd build        :: 编译，产物在 build\libs\elemental_verse-<版本>.jar
dev.cmd runClient    :: 启动带模组的客户端（第一次会下载依赖，比较久）
dev.cmd runServer    :: 启动带模组的服务端
dev.cmd data         :: 跑数据生成
dev.cmd clean        :: 清理构建产物
```

`dev.cmd` 做了三件事：

1. 把 **JDK 指到 `.minecraft` 里那个**（本机没有装 Java，也没有 `JAVA_HOME`）
2. 把 **Gradle 缓存放进工作区** `..\.dsh-cache\gradle` —— 因为沙箱只允许写工作区，
   `C:\Users\<你>\.gradle` 是写不进去的，不重定向就会编译失败
3. 关掉无关的遥测与交互提示

> 网络不通时，第一次 `build` 会在下载依赖处失败。**必须先能访问外网**（或挂代理）才能编译。
> 之后依赖有缓存，可以离线编译。

### 不想用 dev.cmd

自己设好环境变量也行：

```cmd
set JAVA_HOME=C:\Users\<你>\AppData\Roaming\.minecraft\runtime\java-runtime-delta
set GRADLE_USER_HOME=D:\vibe_coding\.dsh-cache\gradle
gradlew.bat build
```

---

## 自动编译（CI）

`.github/workflows/build.yml` 已经配好：**每次 push 都会在 GitHub 上自动编译一遍**。

- 编译失败 → 仓库的 **Actions** 页面能看到完整日志
- 编译成功 → 在 Actions 那次运行的 **Artifacts** 里能下载出 jar

本地没有环境时，这是最省事的验证方式。

---

## 目录结构

```
elemental-verse/
├── docs/                              ← 设计文档（先看这里）
│   ├── 策划案-总纲.md                  设定 + 玩法 + 数值
│   ├── 开发路线图.md                    分阶段施工计划
│   ├── 构建与验证.md                    编译、报错、实测怎么做
│   └── 分阶段/                          每个阶段开工前的小策划案
├── dev.cmd                            ← 一键编译 / 启动
├── gradle.properties                  ← MC 版本、NeoForge 版本、模组元信息
├── build.gradle                       ← 构建脚本
└── src/main/
    ├── java/com/water2h2o/elementalverse/
    ├── resources/
    │   ├── assets/elemental_verse/lang/   中英文文案
    │   └── data/elemental_verse/          配方、掉落、世界生成
    └── templates/META-INF/neoforge.mods.toml
```

**改版本号 / 显示名 / 许可证** → `gradle.properties`（不要在 `.toml` 里改，那是模板，会被覆盖）。

---

## 开发流程

**本模组按"阶段"施工，每一阶段开工前先定一份小策划案。** 详见 `docs/开发路线图.md`。

简化版：

1. 读 `docs/分阶段/NN-xxx.md`，改到满意
2. 照它写代码
3. `dev.cmd build` → `dev.cmd runClient` → 照验收清单点一遍
4. 有问题把日志贴回来

**约定**：
- 数值改动**先改文档再改代码**
- 新增翻译键必须 `zh_cn.json` 与 `en_us.json` **两份一起加**
- 注册一律走 `registry/` 下的集中注册类，不在各处散着写

---

## 许可证

见 `gradle.properties` 的 `mod_license`（当前：All Rights Reserved）。
本仓库基于 NeoForge MDK 模板，模板自身的许可见 `TEMPLATE_LICENSE.txt`。

---

## 致谢

- 模组框架：[NeoForge](https://neoforged.net/) / [ModDevGradle](https://github.com/neoforged/ModDevGradle)
- 世界观与美术：《水塔夜谈》水水（[water2H2O](https://github.com/water2H2O)）
