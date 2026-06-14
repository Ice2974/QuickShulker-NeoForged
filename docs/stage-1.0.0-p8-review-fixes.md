# Stage 1.0.0 P8 Review Fixes

本阶段核实多个外部模型的 1.0.0 发布前代码审查报告，并修复确认存在的问题。

## 审查报告来源

- review-deepseek.md
- review-qwen.md
- review-kimi.md
- review-gemini.md

## 核实与分类结果

### 确认存在，已修复

1. **NeoForge 客户端网络包线程安全（Gemini P1）**
   - 问题：`NeoForgeQuickShulkerNetwork` 的三个 `playToClient` 处理器直接在 Netty 线程调用 `invokeClientHandler`，未使用 `context.enqueueWork()` 排队到主线程。
   - 风险：并发修改客户端状态可能导致 `ConcurrentModificationException`、内存损坏或数据同步错误。
   - 修复：三个 `playToClient` 处理器均包裹 `context.enqueueWork(() -> invokeClientHandler(...))`，与 Forge 端 `consumerMainThread` 行为对齐。
   - 涉及文件：`NeoForgeQuickShulkerNetwork.java`

2. **NeoForge resolveDragSession NPE 防护（Kimi P1）**
   - 问题：创造模式下 `cursorStack.copy()` 未对 null 做防护，而 Forge 端有 `(cursorStack == null ? ItemStack.EMPTY : cursorStack.copy())`。`ItemStack.OPTIONAL_STREAM_CODEC` 反序列化空堆叠时可能返回 null。
   - 风险：创造模式玩家在 drag session 清除或特定 intent 处理时可能触发 `NullPointerException`。
   - 修复：重构 `resolveDragSession` 为与 Forge 完全一致的结构，分别处理新建 session 和已有 session 两种路径，均加入 null 防护。
   - 涉及文件：`NeoForgeShulkerBundlingHandler.java`

3. **NeoForge 创造模式 carried 校验逻辑与 Forge 不一致（DeepSeek P2）**
   - 问题：NeoForge `resolveDragSession` 在已有 session 时仍从客户端 `cursorStack` 读取 carried，而 Forge 会优先使用 `session.creativeCursor()`。这导致双平台对同一操作的 carried 校验决策不同。
   - 修复：合并到上述第 2 项重构中，现有 session 路径使用 `session.hasCreativeCursor() ? session.creativeCursor() : ...` 与 Forge 对齐。
   - 涉及文件：`NeoForgeShulkerBundlingHandler.java`

4. **阶段文档标题编号错误（Qwen P2）**
   - 问题：`docs/stage-1.0.0-p7-bundling-config-page.md` 标题写成 `P6.3`。
   - 修复：改为 `P7`。
   - 涉及文件：`docs/stage-1.0.0-p7-bundling-config-page.md`

5. **README_en 版本号过时（Qwen P2）**
   - 问题：`README_en.md` 仍写 `Version 0.1.0`，与 `gradle.properties` 的 `mod_version=1.0.0` 不符。
   - 修复：更新为 1.0.0 描述。
   - 涉及文件：`README_en.md`

6. **ConfigPage 枚举常量命名残留旧词（Qwen P2）**
   - 问题：枚举常量名为 `SHULKER_BUNDLING`，其 tabKey / titleKey 已是 `bundling`。
   - 修复：重命名为 `BUNDLING`。
   - 涉及文件：`ForgeQuickShulkerConfigScreen.java`、`NeoForgeQuickShulkerConfigScreen.java`

7. **缺少 release-1.0.0.md 发布文档（Qwen P3）**
   - 修复：新建 `docs/releases/release-1.0.0.md`，补齐 1.0.0 支持范围、末影箱 / 潜影盒 bundling、mouse dragged、明确不包含的功能与最终人工测试清单。

### 确认存在，发布后优化

1. **ENDER_CHEST_INSERT 在主 switch 为死代码（Kimi P3）**
   - `ENDER_CHEST_INSERT` 被前置安全拦截提前 return，主 switch 中的 case 永远不会走到。不影响功能，仅为代码清洁问题。未修复，留待后续版本清理。

### 误报

1. **末影箱 extract 路径未走 ENDER_CHEST_SERVICE（DeepSeek）**
   - 核实：NeoForge `handleEnderChestExtract` 已正确使用 `EnderChestBundlingRules.extractLastStackFromPlayerEnderChest`，与 Forge 一致。误报。

### 待人工确认

1. 无法进行 Minecraft 游戏内测试，无法确认修复后的 NeoForge 网络线程安全与 drag session NPE 修复在实机中的表现。
2. 无法进行多人服务器测试，无法确认双平台在多人同时操作末影箱 bundling 时的行为一致性。
3. 无法确认最低 Forge / NeoForge loader 版本。
4. 无法确认 Modrinth 发布元数据。

## 数据安全

- 本阶段修复均不改动宿主锁定、容器副本、写回目标或网络同步的既有安全规则。
- NeoForge 网络线程安全修复消除了客户端状态并发修改风险。
- NeoForge drag session NPE 修复消除了服务端崩溃风险。
- NeoForge 创造模式 carried 校验对齐确保双平台不会因校验逻辑差异产生复制或丢失。

## 验证

- `git diff --check`
- `gradlew.bat :common:test`
- `gradlew.bat :forge-1.20.1:compileJava`
- `gradlew.bat :neoforge-1.21.1:compileJava`
