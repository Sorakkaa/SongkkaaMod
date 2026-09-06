package com.songgka.client.gui;

import com.songgka.client.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class EditLagHudScreen extends Screen {

    private final Screen parent;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private final String mockText = "1000ms";

    public EditLagHudScreen(Screen parent) {
        super(Component.literal("Edit Lag HUD"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        
        g.fill(0, 0, this.width, this.height, 0x44000000);

        // Transparent gray alignment grid / guide lines
        int colLine = 0x22FFFFFF; // subtle transparent gray/white
        int colCenterLine = 0x44FFFFFF; // slightly more visible for center lines

        // Screen center guidelines
        int midX = this.width / 2;
        int midY = this.height / 2;
        g.fill(midX, 0, midX + 1, this.height, colCenterLine);
        g.fill(0, midY, this.width, midY + 1, colCenterLine);

        // Subtle quarter lines
        g.fill(midX / 2, 0, midX / 2 + 1, this.height, colLine);
        g.fill(midX + midX / 2, 0, midX + midX / 2 + 1, this.height, colLine);
        g.fill(0, midY / 2, this.width, midY / 2 + 1, colLine);
        g.fill(0, midY + midY / 2, this.width, midY + midY / 2 + 1, colLine);
        
        var font = Minecraft.getInstance().font;
        String hint = "Drag to move. Scroll wheel to resize (scale). ESC to save.";
        int hintW = font.width(hint);
        g.text(font, hint, (this.width - hintW) / 2, 20, 0xFFFFFF);

        int x = ModConfig.INSTANCE.lagHudX;
        int y = ModConfig.INSTANCE.lagHudY;
        float scale = ModConfig.INSTANCE.lagHudScale;
        if (scale <= 0.1f) scale = 1.0f;
        
        int textW = (int) (font.width(mockText) * scale);
        int textH = (int) (font.lineHeight * scale);
        
        boolean hovering = mx >= x - 2 && mx <= x + textW + 2 && my >= y - 2 && my <= y + textH + 2;

        // Dynamic alignment guide lines following HUD when hovering or dragging
        if (hovering || dragging) {
            int colHudGuide = 0x33AAAAAA;
            g.fill(x, 0, x + 1, this.height, colHudGuide);
            g.fill(x + textW, 0, x + textW + 1, this.height, colHudGuide);
            g.fill(0, y, this.width, y + 1, colHudGuide);
            g.fill(0, y + textH, this.width, y + textH + 1, colHudGuide);

            g.fill(x - 2, y - 2, x + textW + 2, y + textH + 2, 0x55FFFFFF);
        }

        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, mockText, 0, 0, 0xFFFF5555);
        g.pose().popMatrix();

        if (hovering || dragging) {
            String scaleHint = String.format(java.util.Locale.US, "Scale: %.1fx", scale);
            g.text(font, scaleHint, x, y + textH + 4, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0) {
            float scale = ModConfig.INSTANCE.lagHudScale;
            if (scale <= 0.1f) scale = 1.0f;
            if (scrollY > 0) {
                scale += 0.1f;
            } else {
                scale -= 0.1f;
            }
            if (scale < 0.5f) scale = 0.5f;
            if (scale > 4.0f) scale = 4.0f;
            ModConfig.INSTANCE.lagHudScale = Math.round(scale * 10.0f) / 10.0f;
            ModConfig.INSTANCE.save();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        {
            double mx = Minecraft.getInstance().mouseHandler.xpos() * (double)this.width / (double)Minecraft.getInstance().getWindow().getScreenWidth();
            double my = Minecraft.getInstance().mouseHandler.ypos() * (double)this.height / (double)Minecraft.getInstance().getWindow().getScreenHeight();
            
            var font = Minecraft.getInstance().font;
            int x = ModConfig.INSTANCE.lagHudX;
            int y = ModConfig.INSTANCE.lagHudY;
            float scale = ModConfig.INSTANCE.lagHudScale;
            if (scale <= 0.1f) scale = 1.0f;

            int textW = (int) (font.width(mockText) * scale);
            int textH = (int) (font.lineHeight * scale);
            
            if (mx >= x - 2 && mx <= x + textW + 2 && my >= y - 2 && my <= y + textH + 2) {
                dragging = true;
                dragOffsetX = (int) (mx - x);
                dragOffsetY = (int) (my - y);
                return true;
            }
        }
        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            double mx = Minecraft.getInstance().mouseHandler.xpos() * (double)this.width / (double)Minecraft.getInstance().getWindow().getScreenWidth();
            double my = Minecraft.getInstance().mouseHandler.ypos() * (double)this.height / (double)Minecraft.getInstance().getWindow().getScreenHeight();
            ModConfig.INSTANCE.lagHudX = (int) (mx - dragOffsetX);
            ModConfig.INSTANCE.lagHudY = (int) (my - dragOffsetY);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) {
            dragging = false;
            ModConfig.INSTANCE.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ModConfig.INSTANCE.save();
        Minecraft.getInstance().setScreen(parent);
    }
}
