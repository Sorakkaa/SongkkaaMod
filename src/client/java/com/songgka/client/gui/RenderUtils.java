package com.songgka.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class RenderUtils {

    public static void fillRoundedRect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        for (int i = 0; i < h; i++) {
            int dx = 0;
            if (i < r) {
                int dy = r - i;
                dx = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            } else if (i >= h - r) {
                int dy = i - (h - r) + 1;
                dx = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            }
            g.fill(x + dx, y + i, x + w - dx, y + i + 1, color);
        }
    }

    public static void drawGradientOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int colorTop, int colorBottom) {
        if (r <= 0) r = 1;
        for (int i = 0; i < h; i++) {
            int dx = 0;
            if (i < r) {
                int dy = r - i;
                dx = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            } else if (i >= h - r) {
                int dy = i - (h - r) + 1;
                dx = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            }
            int color = mixColors(colorTop, colorBottom, (float) i / h);
            
            if (i == 0 || i == h - 1) {
                g.fill(x + dx, y + i, x + w - dx, y + i + 1, color);
            } else {
                g.fill(x + dx, y + i, x + dx + 1, y + i + 1, color);
                g.fill(x + w - dx - 1, y + i, x + w - dx, y + i + 1, color);
            }
        }
    }

    public static int mixColors(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 * (1 - ratio) + a2 * ratio);
        int r = (int)(r1 * (1 - ratio) + r2 * ratio);
        int g = (int)(g1 * (1 - ratio) + g2 * ratio);
        int b = (int)(b1 * (1 - ratio) + b2 * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
