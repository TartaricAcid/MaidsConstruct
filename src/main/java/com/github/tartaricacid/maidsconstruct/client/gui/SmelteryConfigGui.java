package com.github.tartaricacid.maidsconstruct.client.gui;

import com.github.tartaricacid.maidsconstruct.client.container.SmelteryConfigContainer;
import com.github.tartaricacid.maidsconstruct.init.InitNetwork;
import com.github.tartaricacid.maidsconstruct.init.InitTaskData;
import com.github.tartaricacid.maidsconstruct.network.SetSmelteryConfigMessage;
import com.github.tartaricacid.maidsconstruct.task.data.SmelteryConfigData;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.task.MaidTaskConfigGui;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;
import java.util.Objects;

public class SmelteryConfigGui extends MaidTaskConfigGui<SmelteryConfigContainer> {
    private SmelteryCheckbox craftIngotsButton;
    private SmelteryCheckbox craftBlocksButton;
    private SmelteryCheckbox sortInventoryButton;

    private boolean craftIngots;
    private boolean craftBlocks;
    private boolean sortInventory;

    public SmelteryConfigGui(SmelteryConfigContainer screenContainer, Inventory inv, Component titleIn) {
        super(screenContainer, inv, titleIn);
        SmelteryConfigData config = Objects.requireNonNullElse(this.getMaid().getData(InitTaskData.SMELTERY_CONFIG), SmelteryConfigData.DEFAULT);
        this.craftIngots = config.craftIngots();
        this.craftBlocks = config.craftBlocks();
        this.sortInventory = config.sortInventory();
    }

    @Override
    protected void initAdditionWidgets() {
        int startLeft = leftPos + 95;
        int startTop = topPos + 57;

        MutableComponent ingotsText = Component.translatable("gui.maidsconstruct.config.craft_ingots").withStyle(ChatFormatting.DARK_GRAY);
        craftIngotsButton = new SmelteryCheckbox(startLeft, startTop, ingotsText, craftIngots, b -> craftIngots = b);
        this.addRenderableWidget(craftIngotsButton);

        MutableComponent blocksText = Component.translatable("gui.maidsconstruct.config.craft_blocks").withStyle(ChatFormatting.DARK_GRAY);
        craftBlocksButton = new SmelteryCheckbox(startLeft, startTop + 22, blocksText, craftBlocks, b -> craftBlocks = b);
        this.addRenderableWidget(craftBlocksButton);

        MutableComponent sortText = Component.translatable("gui.maidsconstruct.config.sort_inventory").withStyle(ChatFormatting.DARK_GRAY);
        sortInventoryButton = new SmelteryCheckbox(startLeft, startTop + 44, sortText, sortInventory, b -> sortInventory = b);
        this.addRenderableWidget(sortInventoryButton);
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected void renderAddition(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        MutableComponent title = Component.translatable("gui.maidsconstruct.config.title");
        int x = leftPos + 169 - font.width(title) / 2;
        graphics.drawString(font, title, x, topPos + 40, ChatFormatting.DARK_GRAY.getColor(), false);
    }

    @Override
    protected void renderAdditionTransTooltip(GuiGraphics graphics, int x, int y) {
        if (craftIngotsButton.isHovered()) {
            String key = craftIngots
                    ? "gui.maidsconstruct.config.craft_ingots.tooltip.true"
                    : "gui.maidsconstruct.config.craft_ingots.tooltip.false";
            graphics.renderComponentTooltip(font, Collections.singletonList(
                    Component.translatable(key)
            ), x, y);
        }
        if (craftBlocksButton.isHovered()) {
            String key = craftBlocks
                    ? "gui.maidsconstruct.config.craft_blocks.tooltip.true"
                    : "gui.maidsconstruct.config.craft_blocks.tooltip.false";
            graphics.renderComponentTooltip(font, Collections.singletonList(
                    Component.translatable(key)
            ), x, y);
        }
        if (sortInventoryButton.isHovered()) {
            String key = sortInventory
                    ? "gui.maidsconstruct.config.sort_inventory.tooltip.true"
                    : "gui.maidsconstruct.config.sort_inventory.tooltip.false";
            graphics.renderComponentTooltip(font, Collections.singletonList(
                    Component.translatable(key)
            ), x, y);
        }
    }

    @Override
    public void onClose() {
        if (this.getMaid() != null) {
            InitNetwork.CHANNEL.sendToServer(new SetSmelteryConfigMessage(this.getMaid().getId(), craftIngots, craftBlocks, sortInventory));
        }
        super.onClose();
    }
}
