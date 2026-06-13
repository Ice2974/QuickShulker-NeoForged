# stage-1.0.0-p4-ender-chest-bundling-rules

本阶段只实现末影箱 bundling 的服务端可复用规则层、玩家作用域库存访问边界和基础测试。

本阶段不接入客户端右键、mouse dragged、拖拽输入事件，也不恢复 `rightClickClose`、Bundle 支持或 reopen inventory。

## 本阶段完成内容

- `common` 新增玩家作用域末影箱 bundling 规则：
  - `insert`：把鼠标物品收入“玩家自己的末影箱库存”
  - `pickup insert`：把目标槽物品收入“玩家自己的末影箱库存”
  - `extract`：从“玩家自己的末影箱库存”按首个非空槽顺序提取一组物品
- `common` 新增 `PlayerEnderChestContentAccess` 和 `PlayerEnderChestBundlingService`
  - 平台层后续只允许把“真实玩家末影箱库存”映射给规则层
  - 不允许从末影箱 `ItemStack` 的 `NBT` / `DataComponent` 读取或写回内容
- Forge / NeoForge 新增真实玩家末影箱库存访问实现
  - 直接读写 `player.getEnderChestInventory()` 对应的 live inventory
  - 不经过宿主末影箱物品栈
- 抽出 `ContainerBundlingRules`
  - 复用现有安全的堆叠合并、空槽查找、首槽 / 末槽提取顺序
  - 让潜影盒与末影箱规则共享同一套基础容器算法

## 行为边界

- 末影箱 bundling 的唯一内容来源是“当前玩家自己的末影箱库存”。
- 末影箱优先级高于潜影盒。
- 鼠标拿起末影箱右键潜影盒时，规则语义是“把潜影盒装入末影箱”。
- 鼠标拿起潜影盒右键末影箱时，规则语义也是“把潜影盒装入末影箱”。
- 如果末影箱已满，操作必须直接拒绝，不能回退成“把末影箱装入潜影盒”。
- 后续接入 mouse dragged 时，鼠标拿起末影箱拖过潜影盒也必须沿用同一优先级，按“潜影盒装入末影箱”处理。

## 服务端安全边界

- 本阶段虽然未接入客户端输入，但后续输入层只能上传“打开 / bundling 意图”和槽位引用，不能把客户端看到的宿主 `ItemStack` 当作可信真值。
- 服务端必须重新定位真实宿主，并基于服务端当前玩家对象重新读取该玩家自己的末影箱库存。
- 末影箱库存按玩家隔离；不得把 A 玩家的末影箱内容写到 B 玩家，也不得写回宿主 `ItemStack`。

## 验证覆盖

- 普通物品放入末影箱
- 潜影盒放入末影箱
- 拿潜影盒右键末影箱时解析为末影箱 `insert`
- 拿末影箱右键潜影盒时解析为末影箱 `pickup insert`
- 末影箱已满时拒绝插入，且不回退到潜影盒 bundling
- 末影箱为空时 `extract` 安全失败
- 从末影箱提取第一组物品到空槽语义
- 插入 / 提取前后物品总量守恒

## 本阶段未做

- 客户端右键接入
- mouse dragged 末影箱 bundling 接入
- 末影箱与潜影盒混合输入的实际 packet / handler 接线
- Minecraft 游戏内单人 / 多人验收
