# Maid's Construct 女仆匠魂

![img](./icon.png)

**[English](#english) | [中文](#中文)**

---

## 中文

Maid's Construct 是一个 Minecraft 1.20.1 Forge
模组，为 [车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)
和 [匠魂（Tinkers' Construct）](https://github.com/SlimeKnights/TinkersConstruct) 之间提供联动功能。

### 模组内容

本模组为女仆添加了「冶炼炉」任务，使女仆能够自主管理匠魂冶炼炉/熔铸炉的完整工作流程：

- **自动寻路** — 通过 BFS 搜索附近的冶炼炉/熔铸炉，自动缓存位置
- **添加燃料** — 检测燃料不足时，使用背包中的流体容器补充燃料（兼容所有实现了 Forge 流体 Cap 的模组容器）
- **投入矿石** — 将背包中符合白名单的物品投入冶炼炉熔炼，自动跳过温度不足的物品
- **浇铸成品** — 熔炼完成后自动操作浇注口，将流体浇入浇铸台/浇铸盆；支持锭铸模、宝石铸模、粒铸模和浇铸盆，按优先级选择最佳目标；浇铸前会检查是否存在对应的浇铸配方
- **收取物品** — 冷却完成后从浇铸台/浇铸盆收取成品放入背包
- **合金规避** — 投入矿石前查询合金配方，确认新流体不会与冶炼炉中已有流体形成新的合金后才投入（仅冶炼炉，熔铸炉不做此限制）
- **流体排序** — 浇铸前自动将流体量最多的流体移至底部，确保优先导出最多的流体
- **燃料保护** — 燃料完全耗尽时不会继续投入矿石，避免物品卡在冶炼炉中
- **不可熔化物品提取** — 自动从冶炼炉中提取无法熔化的物品（无配方或温度不够）
- **自动合成** — 自动将 9 个粒合成为锭、9 个锭/宝石合成为块（可关闭）
- **背包整理** — 自动按 `块 > 锭 > 宝石 > 粒 > 其他` 排序整理背包（可关闭）
- **状态气泡** — 女仆会根据当前工作状态显示对应的聊天气泡，包括无燃料、无矿石、无铸模、背包已满等提示

### 自定义配置

#### 模组配置（全局，影响所有女仆）

|   配置项   |        说明        |  默认值   |
|:-------:|:----------------:|:------:|
| 女仆冶炼炉免疫 |  女仆不会被冶炼炉熔炼为流体   |   开启   |
| 允许的燃料流体 | 女仆可使用的燃料流体 ID 列表 | 熔岩、烈焰血 |

#### 女仆任务配置（每只女仆独立设置）

在女仆 GUI 中可为每只女仆单独配置：

|  配置项   |        说明         | 默认值 |
|:------:|:-----------------:|:---:|
| 自动合成锭  |   自动将 9 个粒合成为锭    | 开启  |
| 自动合成块  |  自动将 9 个锭或宝石合成为块  | 开启  |
| 自动整理背包 |   合成后按类型排序整理背包    | 开启  |
| 忽略标签限制 | 忽略白名单标签，投入所有可熔炼物品 | 关闭  |

#### 物品白名单标签

通过 `#maidsconstruct:smeltery_allowlist` 物品标签控制女仆可以投入冶炼炉的物品。默认包含：

- `forge:ores`（原矿石）
- `forge:raw_materials`（粗矿）
- `forge:dusts`（粉末）
- `forge:raw_nuggets`（粗粒，可选）
- 匠魂泥砖（grout、nether grout）

可通过数据包自定义此标签以添加或移除允许的物品。

### 依赖

|         模组         |   版本   |
|:------------------:|:------:|
|     Minecraft      | 1.20.1 |
|       Forge        | 47.2+  |
| Touhou Little Maid | 1.5.0+ |
| Tinkers' Construct | 3.11+  |

### 调试

本模组支持车万女仆的调试系统。开启 debug 模式后，用调试棒点击执行冶炼工作的女仆，可在世界中看到：

- **绿色标记** — 冶炼炉位置及当前状态（IDLE / FUELING / INSERTING / WAITING_MELT / POURING / WAITING_CAST / COLLECTING）

### 关于本仓库

本仓库的首次提交内容由 **Claude Opus 4.6** 生成，而后由人工全面二次校核修正。

### 许可证

BSD-3-Clause

---

## English

Maid's Construct is a Minecraft 1.20.1 Forge mod that bridges
[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) and
[Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct).

### Features

This mod adds a "Smeltery" task for maids, enabling them to autonomously manage the full TiC smeltery/foundry workflow:

- **Auto Pathfinding** — BFS search for nearby smelteries/foundries with position caching
- **Fuel Management** — Detects low fuel and refills using fluid containers from inventory (compatible with all mod
  implementing Forge fluid capability)
- **Ore Insertion** — Inserts allowlisted items into the smeltery for melting, automatically skipping items that require
  higher temperature
- **Fluid Pouring** — After melting, automatically activates faucets to pour fluid into casting tables/basins; supports
  ingot casts, gem casts, nugget casts, and basins with priority-based target selection; checks for valid casting
  recipes before pouring
- **Item Collection** — Collects finished items from casting tables/basins after cooling
- **Alloy Avoidance** — Checks alloy recipes before inserting ores, only inserts when the new fluid won't form a new
  alloy with existing fluids (smeltery only, not foundry)
- **Fluid Sorting** — Automatically moves the largest fluid volume to the bottom before pouring, ensuring priority
  export
- **Fuel Protection** — Won't insert ores when fuel is completely depleted, preventing items from getting stuck
- **Non-Meltable Extraction** — Automatically extracts items that can't be melted (no recipe or insufficient
  temperature) from the smeltery
- **Auto Compacting** — Auto-craft 9 nuggets into ingots, 9 ingots/gems into blocks (can be disabled)
- **Inventory Sorting** — Auto-sort inventory by `Blocks > Ingots > Gems > Nuggets > Other` (can be disabled)
- **Status Bubbles** — Maids display chat bubbles based on current work state, including no fuel, no ores, no casts,
  inventory full, etc.

### Configuration

#### Mod Config (Global, affects all maids)

| Option                 | Description                                 | Default             |
|------------------------|---------------------------------------------|---------------------|
| Maid Smeltery Immunity | Maids are immune to smeltery melting damage | Enabled             |
| Allowed Fuels          | List of fluid IDs maids can use as fuel     | Lava, Blazing Blood |

#### Per-Maid Task Config (Individual per maid)

Configurable per maid in the maid GUI:

| Option               | Description                                           | Default  |
|----------------------|-------------------------------------------------------|----------|
| Auto-craft Ingots    | Auto-compact 9 nuggets into ingots                    | Enabled  |
| Auto-craft Blocks    | Auto-compact 9 ingots or gems into blocks             | Enabled  |
| Auto-sort Inventory  | Sort inventory by type after crafting                 | Enabled  |
| Ignore Allowlist Tag | Ignore the allowlist tag and insert any meltable item | Disabled |

#### Item Allowlist Tag

The `#maidsconstruct:smeltery_allowlist` item tag controls which items maids can insert into the smeltery. By default it
includes:

- `forge:ores` (ores)
- `forge:raw_materials` (raw materials)
- `forge:dusts` (dusts)
- `forge:raw_nuggets` (raw nuggets, optional)
- Tinkers' grout and nether grout

You can customize this tag via datapacks to add or remove allowed items.

### Dependencies

| Mod                | Version |
|--------------------|---------|
| Minecraft          | 1.20.1  |
| Forge              | 47.2+   |
| Touhou Little Maid | 1.5.0+  |
| Tinkers' Construct | 3.11+   |

### Debugging

This mod supports Touhou Little Maid's debug system. With debug mode enabled, use the debug stick on a maid performing
smeltery work to see:

- **Green marker** — Smeltery position and current state (IDLE / FUELING / INSERTING / WAITING_MELT / POURING /
  WAITING_CAST / COLLECTING)

### About This Repository

The initial commit of this repository was generated by **Claude Opus 4.6**, then fully reviewed and corrected by humans.

### License

BSD-3-Clause
