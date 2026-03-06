package com.github.tartaricacid.maidsconstruct.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SmelteryCheckbox extends Checkbox {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");

    private final BooleanConsumer onPress;

    public SmelteryCheckbox(int pX, int pY, Component message, boolean selected, BooleanConsumer onPress) {
        super(pX, pY, 100, 20, message, selected);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        super.onPress();
        this.onPress.accept(this.selected());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        float texU = this.isFocused() ? 20.0F : 0.0F;
        float texV = this.selected() ? 20.0F : 0.0F;
        int alphaChannel = Mth.ceil(this.alpha * 255.0F) << 24;
        int textColor = 0xE0E0E0 | alphaChannel;
        int textX = this.getX() + 24;
        int textY = this.getY() + (this.height - 8) / 2;

        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        graphics.blit(TEXTURE, this.getX(), this.getY(), texU, texV, 20, this.height, 64, 64);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        graphics.drawString(font, this.getMessage(), textX, textY, textColor, false);
    }
}
