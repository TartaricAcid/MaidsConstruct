# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Maid's Construct is a Minecraft 1.20.1 Forge mod that bridges Touhou Little Maid and Tinkers' Construct. It adds a "
Smeltery Work" task enabling maids to autonomously manage TiC smeltery operations: fueling, ore insertion, fluid
pouring, and item collection.

- Mod ID: `maidsconstruct`
- Package: `com.github.tartaricacid.maidsconstruct`
- Java 17, Forge 47.3.0, Parchment mappings

## Build Commands

```bash
./gradlew build              # Build the mod JAR (output in build/libs/)
./gradlew runClient          # Launch Minecraft client with mod loaded
./gradlew runClient2         # Launch second client (username: tartaric_acid) for multiplayer testing
./gradlew runServer          # Launch dedicated server (--nogui)
./gradlew runData            # Run data generators
```

No test suite exists; testing is done in-game.

## Architecture

### Entry Point & Registration

`LittleMaidCompat` (annotated `@LittleMaidExtension`) is the mod's entry point implementing `ILittleMaid`. It registers:

- `TaskSmeltery` - the maid task definition (icon, brain behaviors, description)
- `SmelteryExtraMaidBrain` - custom memory types for the brain
- `SmelteryStateDebug` - debug visualization targets

`MaidsConstruct` is the `@Mod` class that registers `InitMemories.MEMORIES` (DeferredRegister for custom
MemoryModuleTypes).

### State Machine (core logic)

The maid operates as a state machine with states defined in `SmelteryWorkState`:

```
IDLE -> FUELING -> IDLE
IDLE -> INTERACTING -> WAITING_MELT -> IDLE
IDLE -> POURING -> WAITING_CAST -> COLLECTING -> IDLE
```

**`MaidSmelterySearchTask`** (extends `MaidMoveToBlockTask`) is the central brain - it runs BFS to find smelteries,
evaluates the current state, and decides the next action. It handles IDLE, WAITING_MELT, and WAITING_CAST states
directly, dispatching to action tasks for others.

**`MaidSmelteryActionTask`** is the abstract base for all action behaviors. It checks that the maid is in the correct
state and within interaction range, then calls `performAction()` and transitions to the next state. Concrete subclasses:

- `MaidSmelteryFuelTask` - inserts lava buckets into the smeltery fuel slot
- `MaidSmelteryInteractTask` - interacts with the smeltery controller to insert ores (with ore validation and alloy
  avoidance) and extracts non-meltable items from the smeltery inventory
- `MaidSmelteryPourTask` - activates faucets and ensures largest fluid is at bottom
- `MaidSmelteryCollectTask` - picks up finished items from casting tables/basins

### Custom Brain Memories

Registered in `InitMemories`, managed by `SmelteryExtraMaidBrain`:

- `SMELTERY_STATE` (SmelteryWorkState) - current state machine state
- `SMELTERY_POS` (BlockPos) - cached smeltery controller position

### Utility Classes

- **`SmelteryHelper`** - smeltery inspection (empty slots, tank state, fuel checks) and ore validation with alloy
  avoidance logic
- **`CastingHelper`** - finds faucets/casting tables via structure traversal, scores pouring targets (basin > ingot
  cast > nugget cast), checks casting busy/cooling state
- **`VirtualAlloyTank`** - implements `IAlloyTank` to simulate adding a fluid to the smeltery tank for alloy recipe
  matching
- **`SmelteryBubbles`** - chat bubble messages for maid status display

## Key API Patterns

- TiC `HeatingStructureBlockEntity` fields use Lombok `@Getter` (direct public accessors like `getTank()`,
  `getMeltingInventory()`, `getFuelModule()`)
- `CastingBlockEntity.tank` is private - access via `ForgeCapabilities.FLUID_HANDLER` capability
- `FaucetBlockEntity.activate()` / `isPouring()` are public
- Faucet `FACING` is `FACING_HOPPER` (horizontal + DOWN); input direction = opposite of FACING, output = DOWN
- `MeltingRecipeLookup.findFluid(item)` for fast melting recipe lookups without a full recipe query
- Alloy detection uses `TinkerRecipeTypes.ALLOYING` recipe type with `VirtualAlloyTank`
- Ore items are filtered by `forge:ores` and `forge:raw_materials` tags

## Source Code Reference

- `.ref` folder contains source code from TiC and Touhou Little Maid for reference and should not be modified.

## Language

Code comments and commit messages are in Chinese. The mod has bilingual (zh_cn/en_us) lang files for in-game text.
