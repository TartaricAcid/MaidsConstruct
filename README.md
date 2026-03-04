# Maid's Construct 女仆匠魂

Maid's Construct 是一个 Minecraft 1.20.1 Forge
模组，为 [车万女仆（Touhou Little Maid）](https://github.com/TartaricAcid/TouhouLittleMaid)
和 [匠魂（Tinkers' Construct）](https://github.com/SlimeKnights/TinkersConstruct) 之间提供联动功能。

## 模组内容

本模组为女仆添加了「冶炼炉工作」任务，使女仆能够自主管理匠魂冶炼炉的完整工作流程：

- **添加燃料** — 自动检测燃料不足并使用背包中的岩浆桶补充
- **投入矿石** — 将背包中的矿石/粗矿投入冶炼炉熔炼
- **浇铸成品** — 熔炼完成后自动操作浇口，将流体浇入浇铸台/浇铸盆
- **收取物品** — 冷却完成后从浇铸台收取成品放入背包
- **合金规避** — 仅在流体罐清空后才投入新一批矿石，防止不同金属意外合金
- **燃料保护** — 燃料完全耗尽时不会继续投入矿石，避免物品卡在冶炼炉中

女仆会根据当前工作状态显示对应的聊天气泡，支持中英文双语。

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
- **橙色标记** — 女仆当前的行动目标位置

## 关于本仓库

本仓库的首次提交内容由 **Claude Opus 4.6** 生成，目的是测试 AI 在 Minecraft 模组开发领域的当前水平。后续将进行人工二次校核与修正。

## 许可证

BSD-3-Clause
