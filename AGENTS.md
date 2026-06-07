# AGENTS.md

本文件是给 Codex / Agent 使用的仓库工作规则，用于约束 QuickShulker-NeoForged 的代码移植、目录边界、构建验证和回复格式。

本文件只记录协作方式、修改边界和工程规则，不维护临时任务进度。项目当前状态以源码、`docs/` 文档和用户本次任务要求为准。

---

## 文档维护规则

`README.md` 面向模组使用者、服务器管理员和发布页面访客，不维护临时开发阶段进度。

`AGENTS.md` 只记录长期协作规则、工程边界和开发约束，不记录临时阶段进度。

`docs/README.md` 如果存在，只作为 `docs/` 目录说明，不承担玩家使用说明、当前行为合同或 Agent 长期规则。

`docs/` 根目录只保留当前仍有效、维护者当前仍需要阅读的文档，不堆放阶段性开发报告或过期排坑记录。

阶段性开发报告、历史实现记录、旧测试计划、旧排坑记录应统一放在 `docs/archive/stages/` 下。

当前发布文档统一放在 `docs/releases/` 下，例如 `docs/releases/release-0.1.0.md`。

`docs/archive/` 下的文档是历史背景，不应作为当前实现状态的唯一依据。

判断当前项目状态时，优先级为：

1. 源码
2. 当前发布文档，例如 `docs/releases/release-0.1.0.md`
3. 当前仍有效的测试 / 维护文档
4. 用户本次明确要求和最新实机反馈
5. 归档文档仅作为历史参考

当功能行为发生变化时，应同步更新当前文档；不要只新增阶段报告而不修正旧文档。

当阶段报告不再代表当前行为时，应归档并在顶部标注历史说明。

发布前应确保 `docs/` 根目录没有明显过期、互相矛盾或容易误导用户 / Agent 的文档。

如果文档与源码不一致，应在完成说明中指出，并优先修正文档或标记待确认项。

---

## 阶段报告规则

Codex / Agent 完成阶段性开发任务时，应在 `docs/` 下生成阶段报告。

阶段报告文件名应能看出版本和阶段，例如：

* `stage-0.2.0-p2-reopen-inventory.md`

阶段报告只记录该阶段修改、验证、风险和待确认项，不替代当前发布文档。

如果阶段报告已过期，不应继续作为当前行为依据；必要时应在顶部明确标注历史说明。

---

## 发布文档规则

发布文档统一放在 `docs/releases/`。

进入发布前准备阶段时，应生成或更新对应发布文档，例如 `docs/releases/release-0.2.0.md`。

发布文档面向维护者准备，不应堆积已经过期的阶段 TODO。

如果发布文档与源码不一致，应优先修正文档或标记待确认项。

---

## 项目目标

本项目是 QuickShulker 的 Forge / NeoForge 移植版。

目标不是重制、魔改或新增玩法，而是在 Forge 1.20.1 和 NeoForge 1.21.1 上尽量完整移植 MoRanpcy/quickshulker 的现有功能。

在原作 / 参考 fork 功能完整移植完成之前，不添加无关新功能。

---

## 固定命名

项目命名规则如下，不要擅自修改：

```text
Repository: QuickShulker-NeoForged
Display Name: QuickShulker
Mod ID: quickshulker_neoforged
Package: com.ice2974.quickshulkerneoforged
Main Class: QuickShulkerNeoForged
Modrinth Name: QuickShulker NeoForged
```

新代码必须统一使用包名：

```java
com.ice2974.quickshulkerneoforged
```

不要保留或继续使用原作 / fork 的旧包名，例如：

```java
net.kyrptonaught.quickshulker
```

除非是在许可证、NOTICE、README 或来源说明中引用。

---

## 目标版本矩阵

### Forge 目标

```text
Minecraft: 1.20.1
Loader: Forge 47.x
Java: 17
Build Plugin: net.neoforged.moddev.legacyforge
Mappings: Mojang official mappings
```

Forge 47.x 的最低兼容版本可以在实际移植和测试过程中确认，不要在没有验证前写死最低版本。

### NeoForge 目标

```text
Minecraft: 1.21.1
Loader: NeoForge 21.x
Java: 21
Build Plugin: net.neoforged.moddev
Mappings: Mojang official mappings
```

NeoForge 21.x 的最低兼容版本可以在实际移植和测试过程中确认，不要在没有验证前写死最低版本。

---

## 安装侧设计

Forge 1.20.1 和 NeoForge 1.21.1 初版均按 client + server 双端安装设计。

不要默认把本项目实现成纯客户端模组。
如果后续确认部分功能可以 client-only，再由维护者单独决定并更新 README / 发布说明。

---

## 移植范围

当前开发目标仍是尽量完整移植 MoRanpcy/quickshulker 的现有功能，但不是盲目复制所有高风险行为。

至少包含：

* 背包内打开潜影盒
* 背包内打开末影箱
* 快捷键打开
* 鼠标悬停 / 右键 / 按键行为
* 配置项迁移
* 多人服务器可用性

优先移植原作核心功能和稳定功能。

对原作中已知在目标版本存在严重问题的功能，应先评估风险，再决定是否移植。

若维护者已决定暂缓、删除或不再接入某功能，Agent 不应擅自恢复。

当前项目优先保证：

* 数据安全
* 无复制 / 无丢失 / 无幽灵物品
* 多人服务端可用
* Forge 1.20.1 / NeoForge 1.21.1 双平台稳定

原作功能完整移植完成之前，不添加新玩法、新机制或与移植目标无关的增强功能。

---

## 暂不移植的高风险原作功能

`rightClickClose` / “右键关闭当前盒子界面”当前不作为默认移植目标。

当前决策原因：

* 原作 `1.21+` 相关行为已知存在关闭界面导致断开连接等高风险问题
* 当前项目已经采用“quick-open 菜单打开时，不接受对当前宿主的右键或快捷键重复触发”的更安全策略
* 重新接入 `rightClickClose` 会显著增加关闭路径、保存路径、session 收尾和 `reopen inventory` 流程复杂度

除非维护者明确重新开启该任务，否则 Agent 不应实现、恢复或重新接入该功能。

如果源码中仍残留 `rightClickClose` 配置项或历史文档，请在对应任务中单独清理；不要在无关任务里顺手恢复该功能。

---

## 行为优先级

实现时按以下优先级判断：

1. 优先保持 MoRanpcy/quickshulker 现有行为。
2. 其次兼容对应 Minecraft / Forge / NeoForge 版本限制。
3. 最后才考虑代码结构美观。

不要为了“代码更好看”擅自改变玩家可见行为。
如果必须因 Forge / NeoForge / Minecraft 版本差异调整行为，需要在回复中说明原因和影响。

---

## 参考源码来源

本项目需要参考的源码已经存放在项目目录下的 `references/` 目录中。

参考目录：

```text
references/quickshulker-1.20
references/quickshulker-1.21.1
references/quickshulker-26.1-neo
```

用途说明：

### Forge 1.20.1

以原作 / 参考源码的 1.20 版本作为版本地基，回灌 MoRanpcy 的 bugfix 和适合 1.20.1 的功能。

主要参考：

```text
references/quickshulker-1.20
references/quickshulker-1.21.1
```

### NeoForge 1.21.1

以 MoRanpcy 1.21.1 为行为基线，参考 26.1-neo 的 NeoForge 平台接入方式。

主要参考：

```text
references/quickshulker-1.21.1
references/quickshulker-26.1-neo
```

---

## references 目录规则

`references/` 目录只作为源码参考，不允许直接修改。

禁止：

* 修改 `references/` 下的任何文件
* 把 `references/` 当作项目源码目录
* 在 `references/` 中修 bug
* 直接把构建输出写入 `references/`
* 在没有说明来源的情况下整段复制参考源码

允许：

* 阅读参考实现
* 对比行为差异
* 参考类结构、事件接入、网络包、配置项、菜单逻辑
* 将适合的实现移植到本项目源码目录，并按本项目包名、结构和许可证要求整理

如果任务涉及大段移植、复制或改写第三方代码，需要同步检查许可证和来源说明是否需要更新。

---

## 项目结构

初版使用多模块结构：

```text
common
versions/forge-1.20.1
versions/neoforge-1.21.1
```

不要擅自改成多分支维护、preprocessor 结构、Architectury 结构或其他 MultiLoader Template。

---

## common 模块边界

`common` 模块只能放平台无关逻辑。

允许放入：

* 常量
* 配置模型
* 通用逻辑
* 容器判断逻辑
* 物品类型判断
* NBT / DataComponent 抽象接口
* 平台无关工具类
* 与 Forge / NeoForge API 无关的业务判断
* 可被 Forge 和 NeoForge 共同复用的纯 Java / Minecraft 通用逻辑

禁止直接依赖：

* Forge API
* NeoForge API
* Forge event bus
* NeoForge event bus
* 平台 networking API
* 平台 config API
* 平台 key mapping 注册 API
* 平台专属 menu / screen hook
* 平台专属 access transformer 配置

如果某段逻辑需要调用 Forge 或 NeoForge API，应通过平台模块实现适配，不要把平台 API 塞进 common。

---

## Forge 平台模块边界

`forge-1.20.1` 模块负责 Forge 1.20.1 专属实现。

包括但不限于：

* Forge mod 入口
* Forge event 注册
* Forge networking
* Forge config
* Forge key mapping
* Forge menu / screen hook
* Forge access transformer
* Forge 1.20.1 专属 NBT / CompoundTag 适配
* Forge 1.20.1 专属构建、资源和 mods.toml

Forge 模块目标：

```text
Minecraft 1.20.1
Forge 47.x
Java 17
```

不要把 NeoForge 1.21.1 的 API 或 DataComponent 专属实现写进 Forge 模块。

---

## NeoForge 平台模块边界

`neoforge-1.21.1` 模块负责 NeoForge 1.21.1 专属实现。

包括但不限于：

* NeoForge mod 入口
* NeoForge event 注册
* NeoForge networking
* NeoForge config
* NeoForge key mapping
* NeoForge menu / screen hook
* NeoForge access transformer
* NeoForge 1.21.1 专属 DataComponent 适配
* NeoForge 1.21.1 专属构建、资源和 neoforge.mods.toml

NeoForge 模块目标：

```text
Minecraft 1.21.1
NeoForge 21.x
Java 21
```

不要把 Forge 1.20.1 的 API 或 NBT-only 实现写进 NeoForge 模块。

---

## 跨版本差异处理

Forge 1.20.1 和 NeoForge 1.21.1 存在明显差异，尤其是：

```text
Forge 1.20.1: NBT / CompoundTag 逻辑为主
NeoForge 1.21.1: DataComponent 变化明显
```

具体抽象方式由负责编码的 Agent 根据源码实际情况设计，但必须遵守以下原则：

* 不要在业务逻辑里到处散落版本判断。
* 不要用运行期字符串版本号判断来绕过结构设计。
* 不要让 common 直接依赖 Forge / NeoForge API。
* 优先抽象平台差异，例如容器内容读取、物品内容写入、网络同步、菜单打开、配置访问等。
* 平台差异应落在平台模块或明确的平台适配层中。
* 如果某个抽象暂时无法优雅设计，优先保证行为正确，再在回复中说明后续可重构点。

可考虑的抽象方向包括但不限于：

```java
ShulkerContentAccess
ContainerItemAccess
PlatformItemContentApi
PlatformMenuApi
PlatformNetworkApi
PlatformConfigApi
```

具体名称不强制，按实际代码结构决定。

---

## 不引入额外框架

初版不引入：

* Architectury
* MultiLoader Template
* Fabric Loom
* preprocessor
* 其他与当前目标无关的多加载器框架

初版只使用：

```text
common
forge-1.20.1
neoforge-1.21.1
```

以及已确定的 Gradle 插件：

```text
net.neoforged.moddev.legacyforge
net.neoforged.moddev
```

不要为了“以后可能支持更多版本”提前引入复杂框架。

---

## 构建与 Gradle 规则

默认使用仓库内 Gradle Wrapper 执行构建和验证。

Windows 下优先使用：

```powershell
.\gradlew.bat
```

Linux / macOS 下使用：

```bash
./gradlew
```

Java 版本应通过 Gradle Java Toolchain 管理。
不要把本机 JDK 绝对路径写入仓库级 `gradle.properties`、`build.gradle` 或其他提交文件。

Forge 模块应使用 Java 17。
NeoForge 模块应使用 Java 21。

如果需要确认 Toolchain 状态，可运行：

```powershell
.\gradlew.bat -q javaToolchains
```

或：

```bash
./gradlew -q javaToolchains
```

---

## Jar 命名规则

Gradle 内部 `archiveBaseName` 使用小写、脚本友好的名称：

```text
quickshulker-neoforged
```

最终发布 jar 文件名使用维护者指定格式：

```text
QuickShulker-Forge-v{mod_version}-mc{minecraft_version}.jar
QuickShulker-NeoForge-v{mod_version}-mc{minecraft_version}.jar
```

示例：

```text
QuickShulker-Forge-v1.0.0-mc1.20.1.jar
QuickShulker-NeoForge-v1.0.0-mc1.21.1.jar
```

不要擅自改成其他命名格式。

---

## 配置迁移规则

需要迁移 MoRanpcy/quickshulker 的配置项和行为。

迁移时应优先保证：

* 配置项语义一致
* 默认值一致或有明确理由调整
* 玩家可见行为一致
* 客户端和服务端多人环境下同步逻辑正确

Forge 和 NeoForge 的配置实现可以不同，但 common 中应尽量保留平台无关的配置模型或配置语义。

如果某个配置项因平台差异不能完全复刻，需要在回复中说明。

---

## 网络与多人规则

本项目初版要求多人服务器可用性。

涉及打开潜影盒、末影箱、菜单、快捷键、物品内容读取/写入、同步保存等功能时，需要优先考虑：

* 客户端请求是否可信
* 服务端是否重新校验玩家当前物品
* 物品是否仍在玩家背包中
* 打开的容器是否与当前物品绑定
* 玩家死亡、掉线、切维度、关闭菜单时是否正确保存
* 满背包、物品移动、创造模式、生存模式的差异
* 客户端和服务端配置不一致时的处理方式

不要只做单人客户端逻辑。

---

## Quick-open 会话与数据安全规则

Quick-open 相关实现必须把“客户端发起打开意图、服务端重校验并决定是否真正打开”作为固定安全边界。

必须遵守：

* 客户端只发送打开意图、`HostSlotRef` 和目标类型，不把客户端看到的 `ItemStack` 当作可信真值上传后直接使用
* 服务端收到请求后必须按当前玩家真实状态重新定位并校验宿主 `ItemStack`
* 只有服务端重新确认宿主仍然存在、类型匹配、数量和打开条件合法时，才允许创建 quick-open session
* 同一宿主的重复打开必须拒绝，不能为同一宿主创建两个互不相通的容器副本
* 已有 quick-open session 时，如果目标是不同宿主，必须先安全收尾当前 session，再决定是否打开新宿主
* 切换打开前的收尾必须复用既有关闭路径，不能绕过保存 / 丢弃判断直接覆盖 session
* 潜影盒在正常关闭且宿主仍有效时，必须写回实时容器内容
* 宿主失效、被替换、数量变化或已无法确认仍对应原目标时，不得把内容写回错误目标
* 不要把 `dirty` 重新作为是否保存的唯一硬条件；关闭保存应以“是否为合法收尾且宿主仍有效”为主

实现或评审时，必须优先检查：

* 客户端是否只表达“我想打开哪个宿主”
* 服务端是否真的重校验了当前真实宿主
* 切换打开时旧 session 是否先安全收尾
* 是否存在把 A 的内容写回 B、或把失效宿主错误写回的风险

---

## 宿主槽位锁定规则

当 quick-open 菜单已经打开时，当前宿主槽位必须视为锁定资源。

必须拦截会移动当前宿主的操作，包括但不限于：

* 左键
* 右键
* `shift-click`
* 数字键交换
* `Q`
* `PICKUP_ALL`
* `QUICK_CRAFT`
* 会移动副手宿主的 offhand swap

同时必须遵守以下边界：

* 锁定目标是“当前宿主”本身，不要无条件禁止普通物品切换到副手
* 如果某次 `SWAP` / `F` 只会影响普通物品、不会移动当前宿主，应尽量保留原版行为
* 副手宿主不能被换走，但这不等于整个菜单周期内一律禁用所有副手交换
* 拦截危险操作后要及时做服务端与客户端同步，避免幽灵物品、假复制或残留显示状态

设计或修复时，不要只拦 visible slot 点击路径，还要同时检查双击收集、拖拽分发、数字键热栏交换和副手交换等特殊路径。

---

## 输入与原版交互优先级

Quick-open 输入规则必须同时覆盖“可打开”和“不抢原版”的两类边界。

当前长期要求至少支持：

* 无界面手持快捷键
* 无界面手持右键
* 界面内悬停快捷键
* 界面内悬停右键

同时必须遵守：

* 无界面手持右键不能抢原版方块放置
* 无界面手持右键不能抢原版方块交互
* 无界面手持右键不能抢原版实体交互
* 只有在 QuickShulker 已明确判定“本次输入将发送合法打开请求”时，才应取消对应原版输入
* 如果打开请求不合法、宿主映射失败、配置关闭、宿主类型不匹配或服务端前置条件明显不满足，不应取消原版行为
* 已打开 quick-open 菜单时，输入规则仍要区分“同一宿主重复打开”和“切换到不同宿主打开”，不要一刀切禁止全部输入

实现输入入口时，应优先保证：

* 快捷键与右键共享一致的宿主识别和合法性判断
* 输入事件取消发生在“确定要发起 QuickShulker 打开”之后，而不是更早
* 回落到原版行为时，不留下额外副作用

---

## Quick-open 菜单切换规则

Quick-open 菜单已经打开时，允许在合法条件下切换打开另一个宿主，但必须保持单 session、单真实目标。

必须遵守：

* 已打开 quick-open 菜单时，可以切换打开另一个合法宿主
* 同一宿主重复打开必须拒绝
* 不同宿主切换前必须先安全收尾当前 session
* 切换打开不能创建两个互不相通的容器副本
* 旧 session 收尾后，服务端必须重新校验新宿主是否仍然有效，再决定是否真正打开
* 切换过程中不得把旧宿主内容写到新宿主，也不得把新宿主内容污染回旧宿主
* 鼠标位置恢复只作用于本模组发起的 quick-open 菜单切换，不要扩展到普通原版开关菜单或其他模组菜单

如果某平台在菜单切换上需要做额外 UI 补偿，应坚持“最小必要补偿”，不要为了切换体验重写整套菜单生命周期。

---

## UI 与玩家可见行为边界

玩家可见行为应优先对齐 MoRanpcy/quickshulker。

必须遵守：

* 不要为了轻微 UI 差异大改菜单系统
* 不要为了代码结构统一而牺牲已验证可用的槽位绑定、保存链路或同步链路
* 如果只是视觉层或交互细节差异，应先判断是否真的影响玩家可见行为和数据安全
* 只有在确认原作确实统一 UI，且改动不会影响槽位映射、关闭保存、会话收尾或同步逻辑时，才做最小 UI 对齐
* 如果 UI 对齐可能影响宿主锁定、容器副本、写回目标或网络同步，优先保留当前安全实现，并在回复中说明差异

评估 UI 改动时，行为一致性和数据安全优先级高于界面外观一致性。

---

## 日志规则

Agent 可以根据需要补充调试日志，但应遵守：

* 日志不要刷屏
* 高频路径不要每 tick 输出
* 用户操作失败、网络包拒绝、配置读取失败、菜单保存失败等关键路径可以记录 debug / warn
* 不要在正常玩家操作中大量输出 info
* 不要把玩家隐私、服务器敏感路径或无关环境信息写入日志

如果新增日志是为了临时排查，任务完成后应说明是否建议保留。

---

## 测试边界

Agent 不负责最终人工游戏内验收。

Agent 可以负责：

* 生成测试清单
* 补充日志输出
* 修复维护者测试后反馈的问题
* 编译项目
* 修复编译错误
* 运行 Gradle 可执行的检查任务
* 说明无法验证的内容

Agent 不应声称：

* 已完成真实多人测试
* 已在 Minecraft 客户端中完整验证
* 已确认所有功能可用

除非确实运行了对应环境并有明确证据。

最终游戏内测试由维护者人工完成。

---

## 建议人工测试清单

当实现或修改核心功能时，Agent 应根据改动生成对应测试清单。

基础测试至少覆盖：

### Forge 1.20.1

* 单人存档启动
* 本地 Forge 服务端启动
* 客户端 + 服务端双端安装
* 背包内打开普通潜影盒
* 背包内打开染色潜影盒
* 背包内打开末影箱
* 快捷键打开
* 鼠标悬停 / 右键 / 按键行为
* 配置项生效
* 关闭菜单后物品保存
* 取空后关闭菜单仍正确保存为空内容
* `Esc` 关闭保存
* `E` 关闭保存
* 已打开 quick-open 菜单时切换打开另一个合法宿主
* 对同一宿主重复打开会被拒绝
* 普通物品切换到副手仍保留原版行为
* 副手宿主不能被换走
* 生存模式下拦截危险操作后不出现幽灵物品
* 创造模式下不出现复制或异常容器副本
* 无界面手持右键不会抢原版方块放置 / 方块交互 / 实体交互
* 末影箱 quick-open 使用玩家真实 `EnderChestInventory`
* 玩家死亡 / 掉线 / 切维度后的保存行为

### NeoForge 1.21.1

* 单人存档启动
* 本地 NeoForge 服务端启动
* 客户端 + 服务端双端安装
* 背包内打开普通潜影盒
* 背包内打开染色潜影盒
* 背包内打开末影箱
* 快捷键打开
* 鼠标悬停 / 右键 / 按键行为
* 配置项生效
* 关闭菜单后物品保存
* 取空后关闭菜单仍正确保存为空内容
* `Esc` 关闭保存
* `E` 关闭保存
* 已打开 quick-open 菜单时切换打开另一个合法宿主
* 对同一宿主重复打开会被拒绝
* 普通物品切换到副手仍保留原版行为
* 副手宿主不能被换走
* 生存模式下拦截危险操作后不出现幽灵物品
* 创造模式下不出现复制或异常容器副本
* 无界面手持右键不会抢原版方块放置 / 方块交互 / 实体交互
* 末影箱 quick-open 使用玩家真实 `EnderChestInventory`
* 玩家死亡 / 掉线 / 切维度后的保存行为

特殊场景可按改动追加：

* 满背包
* 创造模式
* 生存模式
* 旁观者模式
* 物品移动中打开
* 快速关闭菜单
* 多人同时打开
* quick-open 菜单内从 A 宿主切换打开到 B 宿主
* 对当前同一宿主反复右键 / 快捷键打开
* 宿主被外部操作移走、替换、堆叠数量变化后的收尾
* 副手宿主相关的 `F` / offhand swap 边界
* 客户端和服务端配置不一致
* 客户端缺少模组
* 服务端缺少模组

---

## 许可证与第三方来源

许可证文件已经由维护者处理，不要擅自改动。

除非任务明确要求，默认不要修改：

* `LICENSE`
* `THIRD_PARTY_NOTICES.md`
* README 中的许可证说明

但如果任务涉及：

* 从原作复制代码
* 从 MoRanpcy/quickshulker 移植代码
* 参考 26.1-neo 平台接入
* 大段改写第三方实现
* 删除或替换第三方来源实现

则必须提醒维护者检查许可证和来源说明是否需要更新。

不能确认许可证、NOTICE 或第三方来源声明是否足够时，不要擅自删除来源说明，写入“待人工确认项”。

---

## 默认不要修改

除非任务明确要求，默认不要修改：

* `AGENTS.md`
* `LICENSE`
* `THIRD_PARTY_NOTICES.md`
* `.gitignore`
* `references/` 目录内容
* 发布平台元数据
* 与当前任务无关的构建脚本
* 与当前任务无关的 README 内容
* 与当前任务无关的格式化结果

不要因为打开了文件就顺手格式化整个项目。

---

## 修改原则

执行任务时遵守：

* 优先做最小必要修改
* 不做无关重构
* 不做无关依赖升级
* 不做无关格式化
* 不擅自扩大功能范围
* 不擅自改变玩家可见行为
* 不擅自改变 jar 命名、mod_id、包名、主类名
* 不擅自把 common 改成平台依赖模块

如果发现更大的结构问题，可以在回复中提出建议，但不要未经要求直接大改。

---

## 回复要求

完成任务后，回复中必须说明：

* 修改了哪些文件
* 为什么这样改
* 运行了哪些验证命令
* 验证结果如何
* 哪些内容未验证，以及原因
* 待人工确认项；如果没有，就写“无”
* 是否发现与 `docs/`、现有源码或本 AGENTS.md 不一致的地方

如果任务涉及 Forge / NeoForge 双平台，需要分别说明两个平台的影响。

如果只完成了部分平台，也必须明确写出未完成的平台和原因。

---

## 待人工确认项规则

以下情况必须写入“待人工确认项”：

* 无法确认最低 Forge / NeoForge loader 版本
* 无法确认某功能是否与参考实现行为完全一致
* 无法进行 Minecraft 游戏内测试
* 无法进行多人服务器测试
* 无法确认许可证 / NOTICE 是否需要更新
* 无法确认 Modrinth 发布元数据
* 无法确认某个跨版本抽象是否适合长期维护
* 由于本地环境、依赖下载或 Gradle 问题无法完成构建验证

不要把未验证内容写成已确认结论。
