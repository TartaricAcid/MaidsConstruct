# Maid's Construct 女仆匠魂

Maid's Construct 是一个 Minecraft 1.20.1 Forge
模组，为 [车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)
和 [匠魂（Tinkers' Construct）](https://github.com/SlimeKnights/TinkersConstruct) 之间提供联动功能。

## 模组内容

本模组为女仆添加了「冶炼炉工作」任务，使女仆能够自主管理匠魂冶炼炉的完整工作流程：

- **自动寻路** — 通过 BFS 寻路搜索附近的冶炼炉/熔铸炉，自动缓存位置
- **添加燃料** — 检测燃料不足时，使用背包中的岩浆桶等流体容器补充（兼容所有实现了 Forge 流体能力的模组容器）
- **投入矿石** — 将背包中的矿石（`forge:ores`）和粗矿（`forge:raw_materials`）投入冶炼炉熔炼，自动跳过温度不足的物品
- **浇铸成品** — 熔炼完成后自动操作浇注口，将流体浇入浇铸台/浇铸盆；支持锭铸模、粒铸模和浇铸盆，按优先级选择最佳目标
- **收取物品** — 冷却完成后从浇铸台/浇铸盆收取成品放入背包
- **合金规避** — 投入矿石前查询合金配方，确认新流体不会与冶炼炉中已有流体形成合金后才投入（仅冶炼炉，熔铸炉不做此限制）
- **流体排序** — 浇铸前自动将流体量最多的流体移至底部，确保优先导出最多的流体
- **燃料保护** — 燃料完全耗尽时不会继续投入矿石，避免物品卡在冶炼炉中
- **状态气泡** — 女仆会根据当前工作状态显示对应的聊天气泡（支持中英文双语），包括无燃料、无矿石、无铸模等提示

## 依赖

| 模组                 | 版本          |
|--------------------|-------------|
| Minecraft          | 1.20.1      |
| Forge              | 47+         |
| Touhou Little Maid | 1.5.0+      |
| Tinkers' Construct | 3.11+       |
| Mantle             | 1.11+（匠魂前置） |

## 调试

本模组支持车万女仆的调试系统。开启 debug 模式后，用调试棒点击执行冶炼工作的女仆，可在世界中看到：

- **绿色标记** — 冶炼炉位置及当前状态（IDLE / FUELING / INSERTING / WAITING_MELT / POURING / WAITING_CAST / COLLECTING）

## 关于本仓库

本仓库的首次提交内容由 **Claude Opus 4.6** 生成，而后由人工全面二次校核修正。

## 许可证

BSD-3-Clause
