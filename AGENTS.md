# AGENTS.md

本文件是给 Codex / Agent 使用的仓库长期工作规则，用于约束 QuickShulker-NeoForged 的代码移植、目录边界、构建验证、文档维护和回复格式。

本文件只记录长期协作规则，不维护临时任务进度。项目当前状态以源码、`docs/` 当前文档、发布文档和维护者本次要求为准。

---

## 1. 简短任务提示词默认契约

维护者给出的单次任务提示词可以很短。只要提示词包含：

```text
请先阅读并遵守仓库根目录 AGENTS.md。
```

Agent 就必须把本文件作为默认长期约束，不需要单次提示词重复写出所有通用规则。

单次任务提示词主要补充本次任务特有信息，例如：

* 当前任务目标
* 已观察到的问题现象
* 本次明确不应破坏的既有行为
* 本次重点查看的源码、日志、截图、文档或参考实现
* 本次需要新增或更新的阶段报告 / 发布文档
* 本次特殊验证命令
* 维护者已经确认的人工测试结果或限制条件

当单次任务提示词没有重复说明时，默认仍然必须遵守：

* 不扩大当前任务范围
* 不修改无关文件
* 不做无关重构、依赖升级或全项目格式化
* 不修改 `references/` 目录
* 不把未验证内容写成已确认
* 涉及功能改动时，在 `docs/` 下新增或更新对应阶段报告
* 涉及发布前准备时，生成或更新 `docs/releases/` 下对应发布文档
* 源码或构建逻辑变更后执行相应 Gradle 验证
* 仅文档变更时至少执行 `git diff --check`
* 完成回复按本文件“回复要求”和“待人工确认项规则”说明结果

如果单次任务提示词与本文件冲突，优先遵守维护者本次明确要求；但如果会影响数据安全、跨版本稳定、许可证来源、仓库边界或已明确暂缓功能，必须在回复中说明冲突和处理方式，不要静默绕过。

---

## 2. 当前状态与文档边界

判断当前项目状态时，优先级为：

1. 源码
2. 当前发布文档，例如 `docs/releases/release-0.1.0.md`
3. 当前仍有效的测试 / 维护文档
4. 维护者本次明确要求和最新实机反馈
5. `docs/archive/` 下归档文档仅作为历史参考

文档职责：

* `AGENTS.md`：给 Codex / Agent 使用，记录长期协作规则、工程边界和开发约束
* 根目录 `README.md`：给模组使用者、服务器管理员和发布页面访客阅读
* `docs/README.md`：如果存在，只作为 `docs/` 目录说明
* `docs/stage-*.md`：当前版本阶段报告
* `docs/releases/`：当前发布文档
* `docs/archive/stages/`：历史阶段报告、旧排坑记录和旧路线文档

文档维护规则：

* `README.md` 不维护临时开发阶段进度。
* `AGENTS.md` 不记录临时任务进度。
* `docs/` 根目录只保留当前仍有效、维护者当前仍需要阅读的文档。
* 阶段报告文件名格式：`stage-<版本号>-p<阶段>.<小阶段>-<标题>.md`。
* 阶段报告只记录本阶段修改、验证、风险和待确认项，不替代发布文档。
* 当前版本所有更改完成后，维护者会手动将阶段文档迁移到 `docs/archive/stages/`；除非维护者要求，Agent 不主动移动。
* 功能行为发生变化时，应同步更新当前文档；不要只新增阶段报告而不修正旧文档。
* 如果文档与源码不一致，应在完成说明中指出，并优先修正文档或标记待确认项。

---

## 3. 项目目标与固定命名

本项目是 QuickShulker 的 Forge / NeoForge 移植版。目标不是重制、魔改或新增玩法，而是在 Forge 1.20.1 和 NeoForge 1.21.1 上尽量完整、稳定地移植 MoRanpcy/quickshulker 的核心功能和适合目标版本的现有功能。

原作功能完整移植完成之前，不添加无关新功能。

固定命名，不要擅自修改：

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

不要保留或继续使用原作 / fork 的旧包名，例如 `net.kyrptonaught.quickshulker`，除非是在许可证、NOTICE、README 或来源说明中引用。

目标版本矩阵：

```text
Forge 目标：Minecraft 1.20.1 / Forge 47.x / Java 17 / net.neoforged.moddev.legacyforge
NeoForge 目标：Minecraft 1.21.1 / NeoForge 21.x / Java 21 / net.neoforged.moddev
Mappings: Mojang official mappings
```

Forge 47.x 和 NeoForge 21.x 的最低兼容版本可以在实际移植和测试过程中确认，不要在没有验证前写死最低版本。

Forge 1.20.1 和 NeoForge 1.21.1 初版均按 client + server 双端安装设计，不要默认实现成纯客户端模组。

---

## 4. 移植范围与行为优先级

当前开发目标仍是尽量完整移植 MoRanpcy/quickshulker 的现有功能，但不是盲目复制所有高风险行为。

核心范围至少包含：

* 背包内打开潜影盒
* 背包内打开末影箱
* 快捷键打开
* 鼠标悬停 / 右键 / 按键行为
* 配置项迁移
* 多人服务器可用性

实现时按以下优先级判断：

1. 优先保持 MoRanpcy/quickshulker 的核心目标行为。
2. 其次兼容对应 Minecraft / Forge / NeoForge 版本限制。
3. 最后才考虑代码结构美观。

不要为了“代码更好看”擅自改变玩家可见行为。如果必须因版本差异调整行为，需要在回复中说明原因、玩家影响和风险。

当前项目优先保证：

* 数据安全
* 无复制 / 无丢失 / 无幽灵物品
* 多人服务端可用
* Forge 1.20.1 / NeoForge 1.21.1 双平台稳定

### 暂不移植的高风险原作功能

`rightClickClose` / “右键关闭当前盒子界面”当前不作为默认移植目标。

原因：原作 `1.21+` 相关行为已知存在关闭界面导致断开连接等高风险问题；当前项目已经采用“quick-open 菜单打开时，不接受对当前宿主的右键或快捷键重复触发”的更安全策略。

除非维护者明确重新开启该任务，否则 Agent 不应实现、恢复或重新接入该功能。如果源码中仍残留 `rightClickClose` 配置项或历史文档，请在对应任务中单独清理；不要在无关任务里顺手恢复。

---

## 5. 参考源码与第三方来源

参考源码位于 `references/`，只读，不允许直接修改。

参考目录：

```text
references/quickshulker-1.20
references/quickshulker-1.21.1
references/quickshulker-26.1-neo
```

参考用途：

* Forge 1.20.1：以 `quickshulker-1.20` 为版本地基，回灌 MoRanpcy 的 bugfix 和适合 1.20.1 的功能。
* NeoForge 1.21.1：以 MoRanpcy 1.21.1 为行为基线，参考 `quickshulker-26.1-neo` 的 NeoForge 平台接入方式。

禁止：

* 修改 `references/` 下的任何文件
* 把 `references/` 当作项目源码目录
* 在 `references/` 中修 bug
* 直接把构建输出写入 `references/`
* 在没有说明来源的情况下整段复制参考源码

允许阅读和对比参考实现，并将适合的实现移植到本项目源码目录，按本项目包名、结构和许可证要求整理。

许可证文件已经由维护者处理。除非任务明确要求，默认不要修改：

* `LICENSE`
* `THIRD_PARTY_NOTICES.md`
* README 中的许可证说明

如果任务涉及从原作、MoRanpcy/quickshulker 或 26.1-neo 复制、移植、大段改写或删除第三方来源实现，必须提醒维护者检查许可证和来源说明是否需要更新。不能确认时写入“待人工确认项”。

---

## 6. 项目结构与跨版本边界

初版使用多模块结构：

```text
common
versions/forge-1.20.1
versions/neoforge-1.21.1
```

不要擅自改成多分支维护、preprocessor 结构、Architectury 结构、MultiLoader Template 或其他多加载器框架。初版不引入 Architectury、Fabric Loom、preprocessor 或其他与当前目标无关的框架。

模块边界：

* `common` 只放平台无关逻辑，例如常量、配置模型、通用判断、物品类型判断、NBT / DataComponent 抽象接口、平台无关工具类。
* `common` 不得直接依赖 Forge API、NeoForge API、平台 event bus、platform networking、platform config、平台 key mapping、平台专属 menu / screen hook 或 access transformer。
* `versions/forge-1.20.1` 只放 Forge 1.20.1 专属实现，包括 Forge 入口、事件、网络、配置、按键、菜单、AT、NBT / CompoundTag 适配、资源和构建配置。
* `versions/neoforge-1.21.1` 只放 NeoForge 1.21.1 专属实现，包括 NeoForge 入口、事件、网络、配置、按键、菜单、AT、DataComponent 适配、资源和构建配置。

跨版本差异处理：

* Forge 1.20.1 以 NBT / CompoundTag 逻辑为主。
* NeoForge 1.21.1 的 DataComponent 变化明显。
* 不要在业务逻辑里到处散落版本判断。
* 不要用运行期字符串版本号判断绕过结构设计。
* 平台差异应落在平台模块或明确的平台适配层中。
* 可抽象容器内容读取、物品内容写入、网络同步、菜单打开、配置访问等差异。
* 如果某个抽象暂时无法优雅设计，优先保证行为正确，再在回复中说明后续可重构点。

---

## 7. 构建、Gradle 与 Jar 命名

默认使用仓库内 Gradle Wrapper 执行构建和验证。

Windows 下优先使用：

```powershell
.\gradlew.bat
```

Linux / macOS 下使用：

```bash
./gradlew
```

Java 版本应通过 Gradle Java Toolchain 管理。不要把本机 JDK 绝对路径写入仓库级 `gradle.properties`、`build.gradle` 或其他提交文件。

如需确认 Toolchain 状态，可运行：

```powershell
.\gradlew.bat -q javaToolchains
```

默认验证规则：

* 只修改文档：运行 `git diff --check`。
* 修改源码、资源、构建脚本或平台接入逻辑：先运行 `git diff --check`，再运行受影响平台的 Gradle 编译。
* 同时影响 Forge 和 NeoForge，或无法确认只影响单平台：运行双平台编译：

```powershell
.\gradlew.bat :forge-1.20.1:compileJava
.\gradlew.bat :neoforge-1.21.1:compileJava
```

* 只影响 Forge 1.20.1：运行 `.\gradlew.bat :forge-1.20.1:compileJava`。
* 只影响 NeoForge 1.21.1：运行 `.\gradlew.bat :neoforge-1.21.1:compileJava`。
* 修改发布产物、打包逻辑或资源加载时，应根据实际影响追加更完整的 Gradle 任务，并在回复中说明原因。

如果本地环境导致验证无法运行或失败，应明确说明失败命令、失败原因和未验证风险。不要把未运行的人工游戏内测试写成已通过。

Gradle 内部 `archiveBaseName` 使用小写、脚本友好的名称：

```text
quickshulker-neoforged
```

最终发布 jar 文件名使用维护者指定格式：

```text
QuickShulker-Forge-v{mod_version}-mc{minecraft_version}.jar
QuickShulker-NeoForge-v{mod_version}-mc{minecraft_version}.jar
```

不要擅自改成其他命名格式。

---

## 8. 配置、网络与多人规则

需要迁移 MoRanpcy/quickshulker 的配置项和行为。迁移时优先保证：

* 配置项语义一致
* 默认值一致或有明确理由调整
* 玩家可见行为一致
* 客户端和服务端多人环境下同步逻辑正确

本项目要求多人服务器可用性。涉及打开潜影盒、末影箱、菜单、快捷键、物品内容读取 / 写入、同步保存等功能时，必须优先考虑：

* 客户端请求不能可信，服务端必须重新校验玩家当前真实状态
* 物品是否仍在玩家背包中，打开的容器是否与当前物品绑定
* 玩家死亡、掉线、切维度、关闭菜单时是否正确保存
* 满背包、物品移动、创造模式、生存模式的差异
* 客户端和服务端配置不一致时的处理方式

不要只做单人客户端逻辑。

---

## 9. Quick-open 数据安全规则

Quick-open 相关实现必须把“客户端发起打开意图、服务端重校验并决定是否真正打开”作为固定安全边界。

必须遵守：

* 客户端只发送打开意图、`HostSlotRef` 和目标类型，不把客户端看到的 `ItemStack` 当作可信真值上传后直接使用。
* 服务端收到请求后必须按当前玩家真实状态重新定位并校验宿主 `ItemStack`。
* 只有服务端确认宿主仍然存在、类型匹配、数量和打开条件合法时，才允许创建 quick-open session。
* `HostSlotRef` 映射不能混淆 screen slot、menu slot 和玩家背包逻辑槽位。
* 同一宿主重复打开必须拒绝，不能为同一宿主创建两个互不相通的容器副本。
* 已有 quick-open session 时，如果目标是不同宿主，必须先安全收尾当前 session，再决定是否打开新宿主。
* 切换打开前的收尾必须复用既有关闭路径，不能绕过保存 / 丢弃判断直接覆盖 session。
* 潜影盒在正常关闭且宿主仍有效时，必须写回实时容器内容。
* 宿主失效、被替换、数量变化或已无法确认仍对应原目标时，不得把内容写回错误目标。
* 末影箱必须使用玩家自己的 `EnderChestInventory`，不应写入宿主 `ItemStack`。
* 不要把 `dirty` 重新作为是否保存的唯一硬条件；关闭保存应以“是否为合法收尾且宿主仍有效”为主。
* `Esc` / `E` / 正常关闭路径应统一收口。

实现或评审时，优先检查是否存在复制、丢失、幽灵物品、把 A 内容写回 B、把失效宿主错误写回、切换打开产生两个容器副本等风险。

---

## 10. Quick-open 输入、锁定与菜单切换

当 quick-open 菜单已经打开时，当前宿主槽位必须视为锁定资源。

必须拦截会移动当前宿主的操作，包括左键、右键、`shift-click`、数字键交换、`Q`、`PICKUP_ALL`、`QUICK_CRAFT`、会移动副手宿主的 offhand swap 等。

同时必须遵守：

* 锁定目标是“当前宿主”本身，不要无条件禁止普通物品切换到副手。
* 如果某次 `SWAP` / `F` 只影响普通物品、不会移动当前宿主，应尽量保留原版行为。
* 副手宿主不能被换走，但这不等于整个菜单周期内一律禁用所有副手交换。
* 拦截危险操作后要及时做服务端与客户端同步，避免幽灵物品、假复制或残留显示状态。
* 不要只拦 visible slot 点击路径，还要检查双击收集、拖拽分发、数字键热栏交换和副手交换等特殊路径。

Quick-open 输入规则必须同时覆盖“可打开”和“不抢原版”的边界：

* 支持无界面手持快捷键、无界面手持右键、界面内悬停快捷键、界面内悬停右键。
* 无界面手持右键不能抢原版方块放置、方块交互或实体交互。
* 只有在 QuickShulker 已明确判定“本次输入将发送合法打开请求”时，才应取消对应原版输入。
* 如果打开请求不合法、宿主映射失败、配置关闭、宿主类型不匹配或服务端前置条件明显不满足，不应取消原版行为。
* 已打开 quick-open 菜单时，输入规则仍要区分“同一宿主重复打开”和“切换到不同宿主打开”。
* 鼠标位置恢复只作用于本模组发起的 quick-open 菜单切换，不要扩展到普通原版开关菜单或其他模组菜单。

Quick-open 菜单切换必须保持单 session、单真实目标：不同宿主切换前先安全收尾当前 session，旧 session 收尾后服务端重新校验新宿主，切换过程中不得把旧宿主内容写到新宿主，也不得把新宿主内容污染回旧宿主。

---

## 11. UI、日志与测试边界

玩家可见行为应优先对齐 MoRanpcy/quickshulker，但数据安全优先级高于界面外观一致性。

UI 调整原则：

* 不要为了轻微 UI 差异大改菜单系统。
* 不要为了代码结构统一而牺牲已验证可用的槽位绑定、保存链路或同步链路。
* 如果 UI 对齐可能影响宿主锁定、容器副本、写回目标或网络同步，优先保留当前安全实现，并在回复中说明差异。

日志规则：

* 日志不要刷屏，高频路径不要每 tick 输出。
* 用户操作失败、网络包拒绝、配置读取失败、菜单保存失败等关键路径可以记录 debug / warn。
* 不要在正常玩家操作中大量输出 info。
* 不要把玩家隐私、服务器敏感路径或无关环境信息写入日志。
* 如果新增日志是为了临时排查，任务完成后说明是否建议保留。

测试边界：

* Agent 可以生成测试清单、补充日志、修复反馈问题、编译项目、运行 Gradle 检查并说明无法验证的内容。
* Agent 不负责最终人工游戏内验收。
* 除非确实运行了对应环境并有明确证据，不应声称已完成真实多人测试、已在 Minecraft 客户端中完整验证或已确认所有功能可用。

核心人工测试维度应按改动选择覆盖：

* Forge 1.20.1 / NeoForge 1.21.1 单人启动、本地服务端启动、双端安装
* 普通潜影盒、染色潜影盒、末影箱打开与关闭保存
* 快捷键、悬停右键、无界面手持右键
* `Esc` / `E` / 正常关闭保存
* quick-open 菜单内切换宿主、拒绝同一宿主重复打开
* 宿主槽位锁定、热栏数字键、`Q`、拖拽、双击收集、副手交换
* 生存模式幽灵物品、创造模式复制 / 丢失、满背包、宿主被移走或替换
* 末影箱使用玩家真实 `EnderChestInventory`
* 客户端 / 服务端配置不一致、多人同时打开、死亡 / 掉线 / 切维度

---

## 12. 默认不要修改与修改原则

除非任务明确要求，默认不要修改：

* `AGENTS.md`
* `LICENSE`
* `README.md`
* `README_en.md`
* `THIRD_PARTY_NOTICES.md`
* `.gitignore`
* `references/` 目录内容
* 发布平台元数据
* 与当前任务无关的构建脚本
* 与当前任务无关的 README 内容
* 与当前任务无关的格式化结果

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

## 13. 回复要求与待人工确认项

完成任务后，回复中必须说明：

* 修改了哪些文件
* 为什么这样改
* 运行了哪些验证命令
* 验证结果如何
* 哪些内容未验证，以及原因
* 待人工确认项；如果没有，就写“无”
* 是否发现与 `docs/`、现有源码或本 AGENTS.md 不一致的地方

如果任务涉及 Forge / NeoForge 双平台，需要分别说明两个平台的影响。如果只完成了部分平台，也必须明确写出未完成的平台和原因。

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
