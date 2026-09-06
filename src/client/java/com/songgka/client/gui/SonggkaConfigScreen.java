package com.songgka.client.gui;

import com.songgka.client.config.ModConfig;
import com.songgka.client.features.GhostBlockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SonggkaConfigScreen extends Screen {

    private final Screen lastScreen;

    // Layout
    private static final int WIN_W = 420;
    private static final int WIN_H = 300;
    private static final int SIDEBAR_W = 120;
    private static final int ROW_H = 22;

    // Colors - Glassmorphism Aesthetic
    private static final int COL_BG         = 0xCC111115;
    private static final int COL_SIDEBAR    = 0x66000000;
    private static final int COL_ACCENT     = 0xFF9D00FF;
    private static final int COL_ACCENT_DIM = 0xAA7D00DF;
    private static final int COL_HOVER      = 0x339D00FF;
    private static final int COL_TEXT       = 0xFFFFFFFF;
    private static final int COL_TEXT_DIM   = 0xFFAAAAAA;
    private static final int COL_ON         = 0xFF9D00FF;
    private static final int COL_OFF        = 0xFF555555;
    
    // State
    private int activeTab = 0; // 0=Commands, 1=Custom Player, 2=Misc Settings
    
    // Sub-states - static so it remembers state and doesn't force-open on reload
    private static boolean customNameOpen = false;
    private static boolean playerSizeOpen = false;
    private static boolean dungeonCustomBlocksOpen = false;
    private boolean hexInputFocused = false;
    private boolean prefixInputFocused = false;
    private boolean suffixInputFocused = false;
    private boolean editingColor2 = false;
    private boolean providerDropdownOpen = false;
    private int draggingSlider = -1; // 0=X, 1=Y, 2=Z, 3=Hue, 4=Sat, 5=Val

    // Color picker state
    private float selectedHue = 0.0f;
    private float selectedSat = 1.0f;
    private float selectedVal = 1.0f;

    public SonggkaConfigScreen(Screen lastScreen) {
        super(Component.literal("Astra Client Settings"));
        this.lastScreen = lastScreen;
        updateHSBFromConfig();
    }

    private void updateHSBFromConfig() {
        String hex = editingColor2 && ModConfig.INSTANCE.enableGradient ? ModConfig.INSTANCE.customHexColor2 : ModConfig.INSTANCE.customHexColor;
        if (hex != null && hex.length() == 7 && hex.startsWith("#")) {
            try {
                int rgb = Integer.parseInt(hex.substring(1), 16);
                float[] hsb = java.awt.Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
                selectedHue = hsb[0];
                selectedSat = hsb[1];
                selectedVal = hsb[2];
            } catch (Exception ignored) {}
        }
    }

    private int parseHex(String hex, int defaultColor) {
        if (hex != null && hex.length() == 7 && hex.startsWith("#")) {
            try {
                return 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            } catch (Exception ignored) {}
        }
        return defaultColor;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        
        g.fill(0, 0, this.width, this.height, 0x77000000);

        int px = (this.width - WIN_W) / 2;
        int py = (this.height - WIN_H) / 2;

        // Soft drop shadow
        for (int i = 0; i < 6; i++) {
            RenderUtils.fillRoundedRect(g, px - i, py - i, WIN_W + i * 2, WIN_H + i * 2, 8 + i, 0x1A000000);
        }

        RenderUtils.fillRoundedRect(g, px, py, WIN_W, WIN_H, 8, COL_BG);
        RenderUtils.drawGradientOutline(g, px, py, WIN_W, WIN_H, 8, 0x889D00FF, 0x22440088);
        RenderUtils.fillRoundedRect(g, px, py, SIDEBAR_W, WIN_H, 8, COL_SIDEBAR);
        
        var font = Minecraft.getInstance().font;
        
        g.text(font, "§lSONGKKAA UI", px + 12, py + 15, COL_TEXT);
        
        String[] tabs = {"Commands", "Custom Player", "Dungeon", "Misc Settings"};
        int tabY = py + 40;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = (activeTab == i);
            boolean hover = (mx >= px && mx < px + SIDEBAR_W && my >= tabY && my < tabY + ROW_H);
            
            if (active) {
                RenderUtils.fillRoundedRect(g, px + 8, tabY, SIDEBAR_W - 16, ROW_H, 4, COL_ACCENT_DIM);
            } else if (hover) {
                RenderUtils.fillRoundedRect(g, px + 8, tabY, SIDEBAR_W - 16, ROW_H, 4, COL_HOVER);
            }
            
            g.text(font, tabs[i], px + 16, tabY + 7, active ? COL_TEXT : COL_TEXT_DIM);
            tabY += ROW_H + 4;
        }

        int cx = px + SIDEBAR_W + 15;
        int cy = py + 15;
        int cw = WIN_W - SIDEBAR_W - 30;

        g.text(font, "§l" + tabs[activeTab].toUpperCase(), cx, cy, COL_ACCENT);
        cy += 20;

        if (activeTab == 0) {
            cy = renderToggle(g, font, cx, cy, cw, "All Chat (/ac)", ModConfig.INSTANCE.enableAc, mx, my);
            cy = renderToggle(g, font, cx, cy, cw, "Guild Chat (/gc)", ModConfig.INSTANCE.enableGc, mx, my);
            cy = renderToggle(g, font, cx, cy, cw, "Party Chat (/pc)", ModConfig.INSTANCE.enablePc, mx, my);
            
            cy += 10;
            g.text(font, "Fun Commands:", cx, cy, COL_TEXT); cy += 12;
            
            String[] funLabels = {"!song", "!meow", "!wanted", "!kiss", "!feed", "!poke", "!pat", "!hug", "!sus", "!rizz", "!jerry", "!iq", "!sleep", "!yuri", "!soraka", "!frieren"};
            boolean[] funStates = {
                ModConfig.INSTANCE.enableSong, ModConfig.INSTANCE.enableMeow,
                ModConfig.INSTANCE.enableWanted, ModConfig.INSTANCE.enableKiss,
                ModConfig.INSTANCE.enableFeed, ModConfig.INSTANCE.enablePoke,
                ModConfig.INSTANCE.enablePat, ModConfig.INSTANCE.enableHug,
                ModConfig.INSTANCE.enableSus, ModConfig.INSTANCE.enableRizz,
                ModConfig.INSTANCE.enableJerry, ModConfig.INSTANCE.enableIq,
                ModConfig.INSTANCE.enableSleep, ModConfig.INSTANCE.enableYuri,
                ModConfig.INSTANCE.enableSoraka, ModConfig.INSTANCE.enableFrieren
            };
            
            for (int i = 0; i < funLabels.length; i++) {
                int col = i % 2;
                int row = i / 2;
                int tx = cx + col * (cw / 2);
                int ty = cy + row * 18;
                
                boolean hover = (mx >= tx && mx < tx + 50 && my >= ty && my < ty + 14);
                int color = funStates[i] ? COL_ON : (hover ? COL_HOVER : COL_OFF);
                
                if (funStates[i]) {
                    RenderUtils.fillRoundedRect(g, tx - 1, ty - 1, 52, 16, 3, 0x449D00FF);
                }
                
                RenderUtils.fillRoundedRect(g, tx, ty, 50, 14, 2, color);
                RenderUtils.drawGradientOutline(g, tx, ty, 50, 14, 2, 0x66FFFFFF, 0x11FFFFFF);
                g.text(font, funLabels[i], tx + 4, ty + 3, COL_TEXT);
            }
            
        } else if (activeTab == 1) {
            int hH = 20;

            // --- Section 1: Custom Name & Color ---
            int h1Y = cy;
            boolean h1Hover = (mx >= cx && mx < cx + cw && my >= h1Y && my < h1Y + hH);
            RenderUtils.fillRoundedRect(g, cx, h1Y, cw, hH, 4, customNameOpen ? 0x449D00FF : (h1Hover ? COL_HOVER : 0x22FFFFFF));
            RenderUtils.drawGradientOutline(g, cx, h1Y, cw, hH, 4, customNameOpen ? 0xAA9D00FF : 0x44FFFFFF, 0x11FFFFFF);
            g.text(font, (customNameOpen ? "▼ " : "▶ ") + "Custom Name", cx + 8, h1Y + 6, COL_TEXT);
            cy += hH + 4;

            if (customNameOpen) {
                cy = renderToggle(g, font, cx + 6, cy, cw - 12, "Enable Name Color", ModConfig.INSTANCE.enableNameColor, mx, my);
                
                g.text(font, "Prefix: " + ModConfig.INSTANCE.customPrefix + (prefixInputFocused && (System.currentTimeMillis()/400%2==0)?"_":""), cx + 6, cy + 4, prefixInputFocused ? COL_TEXT : COL_TEXT_DIM);
                cy += 18;
                
                g.text(font, "Suffix: " + ModConfig.INSTANCE.customSuffix + (suffixInputFocused && (System.currentTimeMillis()/400%2==0)?"_":""), cx + 6, cy + 4, suffixInputFocused ? COL_TEXT : COL_TEXT_DIM);
                cy += 18;

                cy = renderToggle(g, font, cx + 6, cy, cw - 12, "Enable Gradient", ModConfig.INSTANCE.enableGradient, mx, my);
                
                // Redesigned modern color buttons
                if (ModConfig.INSTANCE.enableGradient) {
                    int btnW = (cw - 18) / 2;
                    int b1X = cx + 6;
                    int b2X = b1X + btnW + 6;
                    int bH = 20;

                    boolean b1Active = !editingColor2;
                    boolean b1Hover = (mx >= b1X && mx < b1X + btnW && my >= cy && my < cy + bH);
                    int col1 = parseHex(ModConfig.INSTANCE.customHexColor, 0xFFFFFFFF);
                    RenderUtils.fillRoundedRect(g, b1X, cy, btnW, bH, 4, b1Active ? 0x559D00FF : (b1Hover ? COL_HOVER : 0x22FFFFFF));
                    RenderUtils.drawGradientOutline(g, b1X, cy, btnW, bH, 4, b1Active ? COL_ACCENT : 0x33FFFFFF, 0x11FFFFFF);
                    RenderUtils.fillRoundedRect(g, b1X + 6, cy + 5, 10, 10, 3, col1);
                    g.text(font, "Color 1" + (b1Active ? " §a✓" : ""), b1X + 20, cy + 6, b1Active ? COL_TEXT : COL_TEXT_DIM);

                    boolean b2Active = editingColor2;
                    boolean b2Hover = (mx >= b2X && mx < b2X + btnW && my >= cy && my < cy + bH);
                    int col2 = parseHex(ModConfig.INSTANCE.customHexColor2, 0xFFFFFFFF);
                    RenderUtils.fillRoundedRect(g, b2X, cy, btnW, bH, 4, b2Active ? 0x559D00FF : (b2Hover ? COL_HOVER : 0x22FFFFFF));
                    RenderUtils.drawGradientOutline(g, b2X, cy, btnW, bH, 4, b2Active ? COL_ACCENT : 0x33FFFFFF, 0x11FFFFFF);
                    RenderUtils.fillRoundedRect(g, b2X + 6, cy + 5, 10, 10, 3, col2);
                    g.text(font, "Color 2" + (b2Active ? " §a✓" : ""), b2X + 20, cy + 6, b2Active ? COL_TEXT : COL_TEXT_DIM);

                    cy += bH + 6;
                }
                
                String curHex = editingColor2 && ModConfig.INSTANCE.enableGradient ? ModConfig.INSTANCE.customHexColor2 : ModConfig.INSTANCE.customHexColor;
                g.text(font, "Hex: " + curHex + (hexInputFocused && (System.currentTimeMillis()/400%2==0)?"_":""), cx + 6, cy + 4, hexInputFocused ? COL_TEXT : COL_TEXT_DIM);
                cy += 18;

                int boxW = 50;
                int boxH = 22;
                int previewColor = parseHex(curHex, 0xFFFFFFFF);
                
                RenderUtils.fillRoundedRect(g, cx + 6, cy, boxW, boxH, 4, previewColor);
                RenderUtils.drawGradientOutline(g, cx + 6, cy, boxW, boxH, 4, 0x66FFFFFF, 0x22FFFFFF);
                String label = editingColor2 && ModConfig.INSTANCE.enableGradient ? "Color 2 Preview" : "Color 1 Preview";
                g.text(font, label, cx + 6 + boxW + 10, cy + 7, COL_TEXT);
                cy += boxH + 6;
                
                int hueW = cw - 12;
                int hueH = 8;
                
                g.text(font, "Hue", cx + 6, cy, COL_TEXT_DIM); cy += 10;
                for (int i = 0; i < hueW; i++) {
                    int c = java.awt.Color.HSBtoRGB((float)i / hueW, 1.0f, 1.0f) | 0xFF000000;
                    g.fill(cx + 6 + i, cy, cx + 6 + i + 1, cy + hueH, c);
                }
                int pickerX = cx + 6 + (int)(selectedHue * hueW);
                pickerX = Math.max(cx + 6, Math.min(cx + 6 + hueW - 3, pickerX));
                g.fill(pickerX - 1, cy - 2, pickerX + 2, cy + hueH + 2, 0xFFFFFFFF);
                cy += hueH + 4;
                
                g.text(font, "Saturation", cx + 6, cy, COL_TEXT_DIM); cy += 10;
                for (int i = 0; i < hueW; i++) {
                    int c = java.awt.Color.HSBtoRGB(selectedHue, (float)i / hueW, selectedVal) | 0xFF000000;
                    g.fill(cx + 6 + i, cy, cx + 6 + i + 1, cy + hueH, c);
                }
                pickerX = cx + 6 + (int)(selectedSat * hueW);
                pickerX = Math.max(cx + 6, Math.min(cx + 6 + hueW - 3, pickerX));
                g.fill(pickerX - 1, cy - 2, pickerX + 2, cy + hueH + 2, 0xFFFFFFFF);
                cy += hueH + 4;
                
                g.text(font, "Brightness", cx + 6, cy, COL_TEXT_DIM); cy += 10;
                for (int i = 0; i < hueW; i++) {
                    int c = java.awt.Color.HSBtoRGB(selectedHue, selectedSat, (float)i / hueW) | 0xFF000000;
                    g.fill(cx + 6 + i, cy, cx + 6 + i + 1, cy + hueH, c);
                }
                pickerX = cx + 6 + (int)(selectedVal * hueW);
                pickerX = Math.max(cx + 6, Math.min(cx + 6 + hueW - 3, pickerX));
                g.fill(pickerX - 1, cy - 2, pickerX + 2, cy + hueH + 2, 0xFFFFFFFF);
                cy += hueH + 6;
            }

            // --- Section 2: Player Size ---
            int h2Y = cy;
            boolean h2Hover = (mx >= cx && mx < cx + cw && my >= h2Y && my < h2Y + hH);
            RenderUtils.fillRoundedRect(g, cx, h2Y, cw, hH, 4, playerSizeOpen ? 0x449D00FF : (h2Hover ? COL_HOVER : 0x22FFFFFF));
            RenderUtils.drawGradientOutline(g, cx, h2Y, cw, hH, 4, playerSizeOpen ? 0xAA9D00FF : 0x44FFFFFF, 0x11FFFFFF);
            g.text(font, (playerSizeOpen ? "▼ " : "▶ ") + "Player Size", cx + 8, h2Y + 6, COL_TEXT);
            cy += hH + 4;

            if (playerSizeOpen) {
                cy = renderToggle(g, font, cx + 6, cy, cw - 12, "Enable Size Editing", ModConfig.INSTANCE.playerSizeEnabled, mx, my);
                cy = renderSlider(g, font, cx + 6, cy, cw - 12, "Size X", ModConfig.INSTANCE.playerSizeX, mx, my, 0);
                cy = renderSlider(g, font, cx + 6, cy, cw - 12, "Size Y", ModConfig.INSTANCE.playerSizeY, mx, my, 1);
                cy = renderSlider(g, font, cx + 6, cy, cw - 12, "Size Z", ModConfig.INSTANCE.playerSizeZ, mx, my, 2);
                
                int bx = cx + 6;
                boolean bHover = (mx >= bx && mx < bx + 100 && my >= cy && my < cy + 20);
                RenderUtils.fillRoundedRect(g, bx, cy, 100, 20, 4, bHover ? COL_HOVER : 0x55FFFFFF);
                g.text(font, "Reset Size", bx + 22, cy + 6, COL_TEXT);
            }

        } else if (activeTab == 2) {
            int hH = 20;

            // --- Section: Custom Blocks Dropdown Menu ---
            int h1Y = cy;
            boolean h1Hover = (mx >= cx && mx < cx + cw && my >= h1Y && my < h1Y + hH);
            RenderUtils.fillRoundedRect(g, cx, h1Y, cw, hH, 4, dungeonCustomBlocksOpen ? 0x449D00FF : (h1Hover ? COL_HOVER : 0x22FFFFFF));
            RenderUtils.drawGradientOutline(g, cx, h1Y, cw, hH, 4, dungeonCustomBlocksOpen ? 0xAA9D00FF : 0x44FFFFFF, 0x11FFFFFF);
            g.text(font, (dungeonCustomBlocksOpen ? "▼ " : "▶ ") + "Custom Blocks", cx + 8, h1Y + 6, COL_TEXT);
            cy += hH + 4;

            if (dungeonCustomBlocksOpen) {
                cy = renderToggle(g, font, cx + 6, cy, cw - 12, "Custom Blocks (F7/M7)", GhostBlockManager.isGhostBlocksEnabled, mx, my);
                cy = renderToggle(g, font, cx + 16, cy, cw - 22, "└ Glass Panes", GhostBlockManager.isGlassGhostBlocksEnabled, mx, my);
                cy += 4;
            }

            // --- Other Dungeon Settings ---
            cy = renderToggle(g, font, cx, cy, cw, "Time Lost to Lag", ModConfig.INSTANCE.enableLagTimeLost, mx, my);
            
            if (ModConfig.INSTANCE.enableLagTimeLost) {
                int bx = cx + 6;
                boolean bHover = (mx >= bx && mx < bx + cw - 12 && my >= cy && my < cy + 20);
                RenderUtils.fillRoundedRect(g, bx, cy, cw - 12, 20, 4, bHover ? COL_HOVER : 0x55FFFFFF);
                String btnText = "Edit Lag HUD Position";
                int txtW = font.width(btnText);
                g.text(font, btnText, bx + (cw - 12 - txtW) / 2, cy + 6, COL_TEXT);
                cy += 24;
            }
        } else if (activeTab == 3) {
            cy = renderToggle(g, font, cx, cy, cw, "Auto Update (juste visuel pour l'instant)", ModConfig.INSTANCE.enableAutoUpdate, mx, my);
            cy += 5;
            
            g.text(font, "Music Provider:", cx, cy + 6, COL_TEXT);
            int bx = cx + 90;
            int bw = 80;
            boolean bHover = (mx >= bx && mx < bx + bw && my >= cy && my < cy + 18);
            RenderUtils.fillRoundedRect(g, bx, cy, bw, 18, 4, bHover ? COL_HOVER : 0x55FFFFFF);
            g.text(font, ModConfig.INSTANCE.musicProvider != null ? ModConfig.INSTANCE.musicProvider : "None", bx + 6, cy + 5, COL_TEXT);
            
            if (providerDropdownOpen) {
                String[] plats = {"None", "YTM", "Spotify", "Deezer"};
                for (int i = 0; i < plats.length; i++) {
                    int dy = cy + 18 + i * 18;
                    boolean dHover = (mx >= bx && mx < bx + bw && my >= dy && my < dy + 18);
                    RenderUtils.fillRoundedRect(g, bx, dy, bw, 18, 0, dHover ? COL_HOVER : 0xEE222222);
                    g.text(font, plats[i], bx + 6, dy + 5, COL_TEXT);
                }
            }
        }
    }

    private int renderToggle(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int x, int y, int w, String label, boolean state, int mx, int my) {
        g.text(font, label, x, y + 6, COL_TEXT);
        
        int pillW = 30;
        int pillH = 14;
        int pillX = x + w - pillW - 10;
        int pillY = y + 4;
        
        RenderUtils.fillRoundedRect(g, pillX, pillY, pillW, pillH, 7, state ? COL_ON : COL_OFF);
        RenderUtils.drawGradientOutline(g, pillX, pillY, pillW, pillH, 7, 0x33000000, 0x11000000);
        
        int knobSize = 10;
        int knobX = state ? (pillX + pillW - knobSize - 2) : (pillX + 2);
        RenderUtils.fillRoundedRect(g, knobX, pillY + 3, knobSize, knobSize, 5, 0x33000000);
        RenderUtils.fillRoundedRect(g, knobX, pillY + 2, knobSize, knobSize, 5, 0xFFFFFFFF);
        
        return y + ROW_H;
    }

    private int renderSlider(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int x, int y, int w, String label, float val, int mx, int my, int id) {
        g.text(font, label + String.format(": %.2f", val), x, y + 2, COL_TEXT);
        
        int trackY = y + 14;
        RenderUtils.fillRoundedRect(g, x, trackY, w, 4, 2, 0x55FFFFFF);
        
        float pct = (val + 1.0f) / 4.0f; // domain [-1, 3] -> [0, 1]
        pct = Math.max(0, Math.min(1, pct));
        
        RenderUtils.fillRoundedRect(g, x, trackY, (int)(w * pct), 4, 2, COL_ACCENT);
        
        int knobX = x + (int)(w * pct) - 4;
        RenderUtils.fillRoundedRect(g, knobX, trackY - 3, 8, 10, 4, 0xFFFFFFFF);
        
        return y + 26;
    }

    private void updateColorFromSliders() {
        int rgb = java.awt.Color.HSBtoRGB(selectedHue, selectedSat, selectedVal) & 0xFFFFFF;
        String hex = String.format("#%06X", rgb);
        if (editingColor2 && ModConfig.INSTANCE.enableGradient) {
            ModConfig.INSTANCE.customHexColor2 = hex;
        } else {
            ModConfig.INSTANCE.customHexColor = hex;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        int mx = (int) (Minecraft.getInstance().mouseHandler.xpos() * (double)this.width / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        
        int px = (this.width - WIN_W) / 2;
        int cx = px + SIDEBAR_W + 15;
        int cw = WIN_W - SIDEBAR_W - 30;

        if (draggingSlider == 0 || draggingSlider == 1 || draggingSlider == 2) {
            float newPct = (float)(mx - (cx + 6)) / (cw - 12);
            newPct = Math.max(0, Math.min(1, newPct));
            float newVal = -1.0f + newPct * 4.0f;
            if (draggingSlider == 0) ModConfig.INSTANCE.playerSizeX = newVal;
            if (draggingSlider == 1) ModConfig.INSTANCE.playerSizeY = newVal;
            if (draggingSlider == 2) ModConfig.INSTANCE.playerSizeZ = newVal;
        } else if (draggingSlider == 3) {
            selectedHue = Math.max(0, Math.min(1, (float)(mx - (cx + 6)) / (cw - 12)));
            updateColorFromSliders();
        } else if (draggingSlider == 4) {
            selectedSat = Math.max(0, Math.min(1, (float)(mx - (cx + 6)) / (cw - 12)));
            updateColorFromSliders();
        } else if (draggingSlider == 5) {
            selectedVal = Math.max(0, Math.min(1, (float)(mx - (cx + 6)) / (cw - 12)));
            updateColorFromSliders();
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        int mx = (int) (Minecraft.getInstance().mouseHandler.xpos() * (double)this.width / (double)Minecraft.getInstance().getWindow().getScreenWidth());
        int my = (int) (Minecraft.getInstance().mouseHandler.ypos() * (double)this.height / (double)Minecraft.getInstance().getWindow().getScreenHeight());
        
        int px = (this.width - WIN_W) / 2;
        int py = (this.height - WIN_H) / 2;
        
        int tabY = py + 40;
        String[] tabs = {"Commands", "Custom Player", "Dungeon", "Misc Settings"};
        for (int i = 0; i < tabs.length; i++) {
            if (mx >= px && mx < px + SIDEBAR_W && my >= tabY && my < tabY + ROW_H) {
                activeTab = i;
                hexInputFocused = prefixInputFocused = suffixInputFocused = false;
                providerDropdownOpen = false;
                return true;
            }
            tabY += ROW_H + 4;
        }

        int cx = px + SIDEBAR_W + 15;
        int cy = py + 35;
        int cw = WIN_W - SIDEBAR_W - 30;
        
        if (activeTab == 0) {
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H) { ModConfig.INSTANCE.enableAc = !ModConfig.INSTANCE.enableAc; ModConfig.INSTANCE.save(); return true; } cy += ROW_H;
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H) { ModConfig.INSTANCE.enableGc = !ModConfig.INSTANCE.enableGc; ModConfig.INSTANCE.save(); return true; } cy += ROW_H;
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H) { ModConfig.INSTANCE.enablePc = !ModConfig.INSTANCE.enablePc; ModConfig.INSTANCE.save(); return true; } cy += ROW_H;
            
            cy += 22;
            for (int i = 0; i < 16; i++) {
                int col = i % 2;
                int row = i / 2;
                int tx = cx + col * (cw / 2);
                int ty = cy + row * 18;
                if (mx >= tx && mx < tx + 50 && my >= ty && my < ty + 14) {
                    switch(i) {
                        case 0 -> ModConfig.INSTANCE.enableSong = !ModConfig.INSTANCE.enableSong;
                        case 1 -> ModConfig.INSTANCE.enableMeow = !ModConfig.INSTANCE.enableMeow;
                        case 2 -> ModConfig.INSTANCE.enableWanted = !ModConfig.INSTANCE.enableWanted;
                        case 3 -> ModConfig.INSTANCE.enableKiss = !ModConfig.INSTANCE.enableKiss;
                        case 4 -> ModConfig.INSTANCE.enableFeed = !ModConfig.INSTANCE.enableFeed;
                        case 5 -> ModConfig.INSTANCE.enablePoke = !ModConfig.INSTANCE.enablePoke;
                        case 6 -> ModConfig.INSTANCE.enablePat = !ModConfig.INSTANCE.enablePat;
                        case 7 -> ModConfig.INSTANCE.enableHug = !ModConfig.INSTANCE.enableHug;
                        case 8 -> ModConfig.INSTANCE.enableSus = !ModConfig.INSTANCE.enableSus;
                        case 9 -> ModConfig.INSTANCE.enableRizz = !ModConfig.INSTANCE.enableRizz;
                        case 10 -> ModConfig.INSTANCE.enableJerry = !ModConfig.INSTANCE.enableJerry;
                        case 11 -> ModConfig.INSTANCE.enableIq = !ModConfig.INSTANCE.enableIq;
                        case 12 -> ModConfig.INSTANCE.enableSleep = !ModConfig.INSTANCE.enableSleep;
                        case 13 -> ModConfig.INSTANCE.enableYuri = !ModConfig.INSTANCE.enableYuri;
                        case 14 -> ModConfig.INSTANCE.enableSoraka = !ModConfig.INSTANCE.enableSoraka;
                        case 15 -> ModConfig.INSTANCE.enableFrieren = !ModConfig.INSTANCE.enableFrieren;
                    }
                    ModConfig.INSTANCE.save();
                    return true;
                }
            }
        } else if (activeTab == 1) {
            int hH = 20;

            // Header 1: Custom Name
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + hH) {
                customNameOpen = !customNameOpen;
                if (customNameOpen) playerSizeOpen = false;
                hexInputFocused = prefixInputFocused = suffixInputFocused = false;
                return true;
            }
            cy += hH + 4;

            if (customNameOpen) {
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + ROW_H) { 
                    ModConfig.INSTANCE.enableNameColor = !ModConfig.INSTANCE.enableNameColor; 
                    ModConfig.INSTANCE.save(); 
                    return true; 
                } cy += ROW_H;
                
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + 18) { prefixInputFocused = true; suffixInputFocused = false; hexInputFocused = false; return true; } cy += 18;
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + 18) { suffixInputFocused = true; prefixInputFocused = false; hexInputFocused = false; return true; } cy += 18;
                
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + ROW_H) { 
                    ModConfig.INSTANCE.enableGradient = !ModConfig.INSTANCE.enableGradient; 
                    ModConfig.INSTANCE.save(); 
                    return true; 
                } cy += ROW_H;

                if (ModConfig.INSTANCE.enableGradient) {
                    int btnW = (cw - 18) / 2;
                    int b1X = cx + 6;
                    int b2X = b1X + btnW + 6;
                    int bH = 20;

                    if (mx >= b1X && mx < b1X + btnW && my >= cy && my < cy + bH) {
                        editingColor2 = false;
                        updateHSBFromConfig();
                        return true;
                    }
                    if (mx >= b2X && mx < b2X + btnW && my >= cy && my < cy + bH) {
                        editingColor2 = true;
                        updateHSBFromConfig();
                        return true;
                    }
                    cy += bH + 6;
                }
                
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + 18) { hexInputFocused = true; prefixInputFocused = false; suffixInputFocused = false; return true; } cy += 18;
                
                cy += 22 + 6; // color preview box (boxH = 22)
                
                cy += 10;
                int hueW = cw - 12;
                if (mx >= cx + 6 && mx < cx + 6 + hueW && my >= cy && my < cy + 10) { draggingSlider = 3; updateColorFromSliders(); return true; } cy += 12;
                
                cy += 10;
                if (mx >= cx + 6 && mx < cx + 6 + hueW && my >= cy && my < cy + 10) { draggingSlider = 4; updateColorFromSliders(); return true; } cy += 12;
                
                cy += 10;
                if (mx >= cx + 6 && mx < cx + 6 + hueW && my >= cy && my < cy + 10) { draggingSlider = 5; updateColorFromSliders(); return true; } cy += 14;
            }

            // Header 2: Player Size
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + hH) {
                playerSizeOpen = !playerSizeOpen;
                if (playerSizeOpen) customNameOpen = false;
                hexInputFocused = prefixInputFocused = suffixInputFocused = false;
                return true;
            }
            cy += hH + 4;

            if (playerSizeOpen) {
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + ROW_H) { ModConfig.INSTANCE.playerSizeEnabled = !ModConfig.INSTANCE.playerSizeEnabled; ModConfig.INSTANCE.save(); return true; } cy += ROW_H;
                
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + 26) { draggingSlider = 0; return true; } cy += 26;
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + 26) { draggingSlider = 1; return true; } cy += 26;
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + 26) { draggingSlider = 2; return true; } cy += 26;
                
                if (mx >= cx + 6 && mx < cx + 106 && my >= cy && my < cy + 20) {
                    ModConfig.INSTANCE.playerSizeX = 1.0f;
                    ModConfig.INSTANCE.playerSizeY = 1.0f;
                    ModConfig.INSTANCE.playerSizeZ = 1.0f;
                    ModConfig.INSTANCE.save();
                    return true;
                }
            }
        } else if (activeTab == 2) {
            int hH = 20;

            // Header: Custom Blocks
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + hH) {
                dungeonCustomBlocksOpen = !dungeonCustomBlocksOpen;
                return true;
            }
            cy += hH + 4;

            if (dungeonCustomBlocksOpen) {
                if (mx >= cx + 6 && mx < cx + cw - 6 && my >= cy && my < cy + ROW_H) { 
                    GhostBlockManager.isGhostBlocksEnabled = !GhostBlockManager.isGhostBlocksEnabled; 
                    if (Minecraft.getInstance().levelRenderer != null) Minecraft.getInstance().levelRenderer.allChanged();
                    return true; 
                } cy += ROW_H;
                if (mx >= cx + 16 && mx < cx + cw - 6 && my >= cy && my < cy + ROW_H) { 
                    GhostBlockManager.isGlassGhostBlocksEnabled = !GhostBlockManager.isGlassGhostBlocksEnabled; 
                    if (Minecraft.getInstance().levelRenderer != null) Minecraft.getInstance().levelRenderer.allChanged();
                    return true; 
                } cy += ROW_H;
                cy += 4;
            }

            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H) { 
                ModConfig.INSTANCE.enableLagTimeLost = !ModConfig.INSTANCE.enableLagTimeLost; 
                ModConfig.INSTANCE.save(); 
                return true; 
            } cy += ROW_H;
            
            if (ModConfig.INSTANCE.enableLagTimeLost) {
                int bx = cx + 6;
                if (mx >= bx && mx < bx + cw - 12 && my >= cy && my < cy + 20) {
                    Minecraft.getInstance().setScreen(new EditLagHudScreen(this));
                    return true;
                }
                cy += 24;
            }
        } else if (activeTab == 3) {
            if (mx >= cx && mx < cx + cw && my >= cy && my < cy + ROW_H) { ModConfig.INSTANCE.enableAutoUpdate = !ModConfig.INSTANCE.enableAutoUpdate; ModConfig.INSTANCE.save(); return true; } cy += ROW_H;
            cy += 5;
            
            int bx = cx + 90;
            if (providerDropdownOpen) {
                if (mx >= bx && mx < bx + 80 && my >= cy + 18 && my < cy + 18 + 72) {
                    String[] plats = {"None", "YTM", "Spotify", "Deezer"};
                    ModConfig.INSTANCE.musicProvider = plats[(int)((my - (cy + 18)) / 18)];
                    ModConfig.INSTANCE.save();
                    providerDropdownOpen = false;
                    return true;
                } else {
                    providerDropdownOpen = false;
                }
            } else if (mx >= bx && mx < bx + 80 && my >= cy && my < cy + 18) {
                providerDropdownOpen = true;
                return true;
            }
        }
        
        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider != -1) {
            draggingSlider = -1;
            ModConfig.INSTANCE.save();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char chr = (char) event.codepoint();
        if (prefixInputFocused && chr >= 32 && chr <= 126) {
            ModConfig.INSTANCE.customPrefix += chr; ModConfig.INSTANCE.save(); return true;
        } else if (suffixInputFocused && chr >= 32 && chr <= 126) {
            ModConfig.INSTANCE.customSuffix += chr; ModConfig.INSTANCE.save(); return true;
        } else if (hexInputFocused) {
            String cur = editingColor2 && ModConfig.INSTANCE.enableGradient ? ModConfig.INSTANCE.customHexColor2 : ModConfig.INSTANCE.customHexColor;
            if (cur == null || !cur.startsWith("#")) cur = "#";
            char c = Character.toUpperCase(chr);
            if (c == '#' && !cur.startsWith("#")) cur = "#" + cur;
            else if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                if (cur.length() < 7) cur = cur + c;
            }
            if (editingColor2 && ModConfig.INSTANCE.enableGradient) ModConfig.INSTANCE.customHexColor2 = cur;
            else ModConfig.INSTANCE.customHexColor = cur;
            updateHSBFromConfig();
            ModConfig.INSTANCE.save();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
        if (key == 259) { // Backspace
            if (prefixInputFocused && ModConfig.INSTANCE.customPrefix.length() > 0) {
                ModConfig.INSTANCE.customPrefix = ModConfig.INSTANCE.customPrefix.substring(0, ModConfig.INSTANCE.customPrefix.length() - 1);
                ModConfig.INSTANCE.save(); return true;
            } else if (suffixInputFocused && ModConfig.INSTANCE.customSuffix.length() > 0) {
                ModConfig.INSTANCE.customSuffix = ModConfig.INSTANCE.customSuffix.substring(0, ModConfig.INSTANCE.customSuffix.length() - 1);
                ModConfig.INSTANCE.save(); return true;
            } else if (hexInputFocused) {
                String cur = editingColor2 && ModConfig.INSTANCE.enableGradient ? ModConfig.INSTANCE.customHexColor2 : ModConfig.INSTANCE.customHexColor;
                if (cur != null && cur.length() > 1) {
                    cur = cur.substring(0, cur.length() - 1);
                    if (editingColor2 && ModConfig.INSTANCE.enableGradient) ModConfig.INSTANCE.customHexColor2 = cur;
                    else ModConfig.INSTANCE.customHexColor = cur;
                    updateHSBFromConfig();
                    ModConfig.INSTANCE.save();
                }
                return true;
            }
        }
        if (key == 256) { // Escape
            if (prefixInputFocused || suffixInputFocused || hexInputFocused || providerDropdownOpen) {
                prefixInputFocused = suffixInputFocused = hexInputFocused = false;
                providerDropdownOpen = false;
                return true;
            }
            this.minecraft.setScreen(lastScreen);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
