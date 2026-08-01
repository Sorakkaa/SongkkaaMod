package com.songgka.client.gui;

import com.songgka.client.config.ModConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class SonggkaConfigScreen extends Screen {

    private final Screen lastScreen;

    // Layout
    private static final int START_X  = 6;
    private static final int START_Y  = 6;
    private static final int PANEL_W  = 148;
    private static final int GAP      = 5;
    private static final int HEADER_H = 16;
    private static final int CAT_H    = 14;   // niveau 1 : Party Commands
    private static final int ROW_H    = 13;   // ligne toggle (All/Guild/Party Chat)
    private static final int SUBCAT_H = 13;   // niveau 2 : Commands
    private static final int SUBCMD_H = 13;   // ligne commande (song)
    private static final int INDENT_1 = 8;
    private static final int INDENT_2 = 16;

    // Couleurs
    private static final int COL_BG      = 0xEE101018;
    private static final int COL_HDR_BG  = 0xFF1B0B38;
    private static final int COL_HDR_FG  = 0xFFCC77FF;
    private static final int COL_ACCENT  = 0xFFBB55FF;
    private static final int COL_CAT_BG  = 0xFF180F26;
    private static final int COL_CAT_FG  = 0xFFBB88DD;
    private static final int COL_SUB_BG  = 0xFF130B20;
    private static final int COL_TEXT    = 0xFFCCCCCC;
    private static final int COL_CMD     = 0xFF9977BB;  // texte commande (song)
    private static final int COL_ON      = 0xFFCC66FF;
    private static final int COL_OFF     = 0xFF282838;
    private static final int COL_HOVER   = 0x28FFFFFF;
    private static final int COL_SEP     = 0x18FFFFFF;

    // États pliage
    private boolean partyCmdExpanded  = false;
    private boolean cmdExpanded       = false;
    private boolean colorPickerExpanded = false;
    private boolean songConfigExpanded  = false;
    private boolean platformDropdownOpen = false;
    private boolean presetDropdownOpen = false;
    private boolean hexInputFocused = false;
    private float selectedHue = 0.0f;
    
    private boolean sizeConfigExpanded = false;
    private int draggingSlider = -1; // 0=X, 1=Y, 2=Z

    private int commandeX, miscX;

    public SonggkaConfigScreen(Screen lastScreen) {
        super(Component.literal("Songkkaa ClickGUI"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        commandeX = START_X;
        miscX     = START_X + PANEL_W + GAP;
    }

    // ── RENDU ──────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        g.fill(0, 0, this.width, this.height, 0x50000000);
        renderCommandePanel(g, commandeX, START_Y, mx, my);
        renderMiscPanel    (g, miscX,     START_Y, mx, my);
    }

    // ── Panneau COMMANDE ───────────────────────────────────────────────────
    private void renderCommandePanel(GuiGraphicsExtractor g, int px, int py, int mx, int my) {
        var font = Minecraft.getInstance().font;

        // Hauteur dynamique
        int rows   = partyCmdExpanded ? 3 : 0;              // All/Guild/Party Chat
        int subCat = partyCmdExpanded ? 1 : 0;              // "Commands" header
        int cmds   = (partyCmdExpanded && cmdExpanded) ? 1 : 0; // song
        int totalH = HEADER_H + CAT_H + rows * ROW_H + subCat * SUBCAT_H + cmds * SUBCMD_H + 2;

        g.fill(px, py, px + PANEL_W, py + totalH, COL_BG);

        // Header "Commands"
        g.fill(px, py, px + PANEL_W, py + HEADER_H, COL_HDR_BG);
        g.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, COL_ACCENT);
        g.text(font, "§l  🎮  Commands", px + 6, py + 4, COL_HDR_FG);

        // ── Niveau 1 : "Party Commands" ──
        int catY = py + HEADER_H;
        if (mx >= px && mx < px + PANEL_W && my >= catY && my < catY + CAT_H)
            g.fill(px, catY, px + PANEL_W, catY + CAT_H, COL_HOVER);
        g.fill(px, catY, px + PANEL_W, catY + CAT_H, COL_CAT_BG);
        g.fill(px, catY, px + 2, catY + CAT_H, partyCmdExpanded ? COL_ACCENT : 0x50BB55FF);
        g.text(font, (partyCmdExpanded ? "§7▼ " : "§7▶ ") + "§bParty Commands",
                px + INDENT_1, catY + 3, COL_CAT_FG);
        g.fill(px, catY + CAT_H - 1, px + PANEL_W, catY + CAT_H, COL_SEP);

        if (!partyCmdExpanded) {
            g.fill(px, py + totalH - 1, px + PANEL_W, py + totalH, 0x40BB55FF);
            return;
        }

        // ── 3 lignes toggle (All / Guild / Party Chat) ──
        int toggleBase = catY + CAT_H;
        renderToggleRow(g, font, px, toggleBase, 0, "All Chat",   ModConfig.INSTANCE.enableAc, mx, my);
        renderToggleRow(g, font, px, toggleBase, 1, "Guild Chat", ModConfig.INSTANCE.enableGc, mx, my);
        renderToggleRow(g, font, px, toggleBase, 2, "Party Chat", ModConfig.INSTANCE.enablePc, mx, my);

        // ── Niveau 2 : "Commands" ──
        int subY = toggleBase + 3 * ROW_H;
        if (mx >= px && mx < px + PANEL_W && my >= subY && my < subY + SUBCAT_H)
            g.fill(px, subY, px + PANEL_W, subY + SUBCAT_H, COL_HOVER);
        g.fill(px, subY, px + PANEL_W, subY + SUBCAT_H, COL_SUB_BG);
        g.fill(px + INDENT_1, subY, px + INDENT_1 + 2, subY + SUBCAT_H,
                cmdExpanded ? 0xFF9955CC : 0x30BB55FF);
        g.text(font, (cmdExpanded ? "§7▼ " : "§7▶ ") + "§7Commands",
                px + INDENT_1 + 5, subY + 2, COL_CAT_FG);
        g.fill(px, subY + SUBCAT_H - 1, px + PANEL_W, subY + SUBCAT_H, COL_SEP);

        // ── Commandes (si "Commands" déplié) ──
        if (cmdExpanded) {
            int cmdY = subY + SUBCAT_H;
            if (mx >= px && mx < px + PANEL_W && my >= cmdY && my < cmdY + SUBCMD_H)
                g.fill(px, cmdY, px + PANEL_W, cmdY + SUBCMD_H, COL_HOVER);
            g.fill(px, cmdY + SUBCMD_H - 1, px + PANEL_W, cmdY + SUBCMD_H, COL_SEP);
            g.fill(px + INDENT_2, cmdY + SUBCMD_H / 2,
                    px + INDENT_2 + 4, cmdY + SUBCMD_H / 2 + 1, 0x60BB55FF);
            g.text(font, "song", px + INDENT_2 + 7, cmdY + 2, COL_CMD);
            // Toggle pill
            boolean songOn = ModConfig.INSTANCE.enableSong;
            int pillW = 20, pillH = 7;
            int pillX = px + PANEL_W - pillW - 5;
            int pillY = cmdY + (SUBCMD_H - pillH) / 2;
            g.fill(pillX, pillY, pillX + pillW, pillY + pillH, songOn ? COL_ON : COL_OFF);
            g.fill(pillX, pillY, pillX + pillW, pillY + 1, 0x40FFFFFF);
            int knobX = songOn ? pillX + pillW - pillH : pillX;
            g.fill(knobX, pillY, knobX + pillH, pillY + pillH, 0xFFFFFFFF);
        }

        g.fill(px, py + totalH - 1, px + PANEL_W, py + totalH, 0x40BB55FF);
    }

    private void renderToggleRow(GuiGraphicsExtractor g,
                                  net.minecraft.client.gui.Font font,
                                  int px, int baseY, int i,
                                  String label, boolean enabled,
                                  int mx, int my) {
        int ry  = baseY + i * ROW_H;
        int ry2 = ry + ROW_H;

        if (mx >= px && mx < px + PANEL_W && my >= ry && my < ry2)
            g.fill(px, ry, px + PANEL_W, ry2, COL_HOVER);
        g.fill(px, ry2 - 1, px + PANEL_W, ry2, COL_SEP);
        g.fill(px + INDENT_1, ry + ROW_H / 2,
                px + INDENT_1 + 4, ry + ROW_H / 2 + 1, 0x60BB55FF);
        g.text(font, label, px + INDENT_1 + 7, ry + 2, COL_TEXT);

        int pillW = 20, pillH = 7;
        int pillX = px + PANEL_W - pillW - 5;
        int pillY = ry + (ROW_H - pillH) / 2;
        g.fill(pillX, pillY, pillX + pillW, pillY + pillH, enabled ? COL_ON : COL_OFF);
        g.fill(pillX, pillY, pillX + pillW, pillY + 1, 0x40FFFFFF);
        int knobX = enabled ? pillX + pillW - pillH : pillX;
        g.fill(knobX, pillY, knobX + pillH, pillY + pillH, 0xFFFFFFFF);
    }

    // ── Panneau MISC ───────────────────────────────────────────────────────
    private void renderMiscPanel(GuiGraphicsExtractor g, int px, int py, int mx, int my) {
        var font = Minecraft.getInstance().font;
        int dropH = (songConfigExpanded && platformDropdownOpen) ? (ROW_H * 4) : 0;
        int presetDropH = (colorPickerExpanded && presetDropdownOpen) ? (ROW_H * com.songgka.client.color.NameColorManager.COLORS.length) : 0;
        int totalH = HEADER_H + CAT_H + (colorPickerExpanded ? (ROW_H * 2 + presetDropH + 96) : 0) + 
                     CAT_H + (songConfigExpanded ? (ROW_H * 3 + dropH) : 0) +
                     CAT_H + (sizeConfigExpanded ? (ROW_H * 3) : 0) + 4;

        g.fill(px, py, px + PANEL_W, py + totalH, COL_BG);

        // Header MISC
        g.fill(px, py, px + PANEL_W, py + HEADER_H, COL_HDR_BG);
        g.fill(px, py + HEADER_H - 1, px + PANEL_W, py + HEADER_H, COL_ACCENT);
        g.text(font, "§l  ⚙  MISC", px + 6, py + 4, COL_HDR_FG);

        int basey = py + HEADER_H;

        // ── Catégorie 1 : Name Color ──
        int cat1Y1 = basey;
        int cat1Y2 = basey + CAT_H;
        if (mx >= px && mx < px + PANEL_W && my >= cat1Y1 && my < cat1Y2)
            g.fill(px, cat1Y1, px + PANEL_W, cat1Y2, COL_HOVER);
        g.fill(px, cat1Y1, px + PANEL_W, cat1Y2, COL_CAT_BG);
        g.fill(px, cat1Y1, px + 2, cat1Y2, colorPickerExpanded ? COL_ACCENT : 0x50BB55FF);
        g.text(font, (colorPickerExpanded ? "§7▼ " : "§7▶ ") + "§bName Color",
                px + INDENT_1, cat1Y1 + 3, COL_CAT_FG);
        g.fill(px, cat1Y2 - 1, px + PANEL_W, cat1Y2, COL_SEP);

        // Body Name Color (si déplié)
        if (colorPickerExpanded) {
            int r0y1 = cat1Y2;
            int r0y2 = r0y1 + ROW_H;
            if (mx >= px && mx < px + PANEL_W && my >= r0y1 && my < r0y2)
                g.fill(px, r0y1, px + PANEL_W, r0y2, COL_HOVER);
            g.fill(px, r0y2 - 1, px + PANEL_W, r0y2, COL_SEP);
            g.fill(px + INDENT_1, r0y1 + ROW_H / 2, px + INDENT_1 + 4, r0y1 + ROW_H / 2 + 1, 0x60BB55FF);
            g.text(font, "Enable", px + INDENT_1 + 7, r0y1 + 2, COL_TEXT);

            // Toggle Pill for Activer
            boolean enabled = ModConfig.INSTANCE.enableNameColor;
            int pillW = 16, pillH = 7;
            int pillX = px + PANEL_W - pillW - 5;
            int pillY = r0y1 + (ROW_H - pillH) / 2;
            g.fill(pillX, pillY, pillX + pillW, pillY + pillH, enabled ? COL_ON : COL_OFF);
            int knobX = enabled ? pillX + pillW - pillH : pillX;
            g.fill(knobX, pillY, knobX + pillH, pillY + pillH, 0xFFFFFFFF);

            // Row for Preset Color dropdown
            int r01y1 = r0y2;
            int r01y2 = r01y1 + ROW_H;
            if (mx >= px && mx < px + PANEL_W && my >= r01y1 && my < r01y2)
                g.fill(px, r01y1, px + PANEL_W, r01y2, COL_HOVER);
            g.fill(px, r01y2 - 1, px + PANEL_W, r01y2, COL_SEP);
            g.fill(px + INDENT_1, r01y1 + ROW_H / 2, px + INDENT_1 + 4, r01y1 + ROW_H / 2 + 1, 0x60BB55FF);

            var opt = com.songgka.client.color.NameColorManager.getCurrentOption();
            String presetLabel = (presetDropdownOpen ? "§7▼ " : "§7▶ ") + "Preset: " + (opt != null ? opt.name : "?");
            g.text(font, presetLabel, px + INDENT_1 + 7, r01y1 + 2, COL_TEXT);

            // Dropdown items
            int renderPresetDropH = 0;
            if (presetDropdownOpen) {
                var colors = com.songgka.client.color.NameColorManager.COLORS;
                for (int i = 0; i < colors.length; i++) {
                    int dy1 = r01y2 + i * ROW_H;
                    int dy2 = dy1 + ROW_H;
                    boolean isSelected = i == ModConfig.INSTANCE.nameColorIndex;
                    g.fill(px, dy1, px + PANEL_W, dy2, isSelected ? 0x40BB55FF : 0xFF0D0820);
                    if (mx >= px && mx < px + PANEL_W && my >= dy1 && my < dy2)
                        g.fill(px, dy1, px + PANEL_W, dy2, COL_HOVER);
                    g.fill(px, dy2 - 1, px + PANEL_W, dy2, COL_SEP);
                    g.text(font, (isSelected ? "§d✔ " : "  ") + colors[i].name, px + INDENT_1 + 7, dy1 + 2, COL_TEXT);
                }
                renderPresetDropH = colors.length * ROW_H;
            }

            int dropY = r01y2 + renderPresetDropH + 4;
            int boxX = px + INDENT_1;
            int boxY = dropY;
            int boxW = PANEL_W - INDENT_1 * 2;
            int boxH = 55;

            // 1. 2D Saturation / Value Box
            String curHex = ModConfig.INSTANCE.customHexColor;
            int currentRgb = 0xFF55AA;
            float currentSat = 1.0f, currentVal = 1.0f;
            if (curHex != null && curHex.startsWith("#") && curHex.length() == 7) {
                try {
                    currentRgb = Integer.parseInt(curHex.substring(1), 16);
                    float[] hsb = java.awt.Color.RGBtoHSB((currentRgb >> 16) & 0xFF, (currentRgb >> 8) & 0xFF, currentRgb & 0xFF, null);
                    selectedHue = hsb[0];
                    currentSat = hsb[1];
                    currentVal = hsb[2];
                } catch (Exception ignored) {}
            }

            for (int x = 0; x < boxW; x += 3) {
                float sat = (float) x / (float) boxW;
                for (int y = 0; y < boxH; y += 3) {
                    float val = 1.0f - ((float) y / (float) boxH);
                    int rgb = java.awt.Color.HSBtoRGB(selectedHue, sat, val);
                    g.fill(boxX + x, boxY + y, Math.min(boxX + boxW, boxX + x + 3), Math.min(boxY + boxH, boxY + y + 3), rgb | 0xFF000000);
                }
            }

            // Outer border
            g.fill(boxX - 1, boxY - 1, boxX + boxW + 1, boxY, 0xFF353545);
            g.fill(boxX - 1, boxY + boxH, boxX + boxW + 1, boxY + boxH + 1, 0xFF353545);
            g.fill(boxX - 1, boxY, boxX, boxY + boxH, 0xFF353545);
            g.fill(boxX + boxW, boxY, boxX + boxW + 1, boxY + boxH, 0xFF353545);

            // Circular Pin Handle
            int handleX = boxX + Math.round(currentSat * boxW);
            int handleY = boxY + Math.round((1.0f - currentVal) * boxH);
            g.fill(handleX - 3, handleY - 3, handleX + 4, handleY + 4, 0xFFFFFFFF);
            g.fill(handleX - 2, handleY - 2, handleX + 3, handleY + 3, currentRgb | 0xFF000000);

            // 2. 1D Hue Spectrum Slider Bar
            int hueX = boxX;
            int hueY = boxY + boxH + 5;
            int hueW = boxW;
            int hueH = 9;

            for (int i = 0; i < hueW; i++) {
                float h = (float) i / (float) hueW;
                int rgb = java.awt.Color.HSBtoRGB(h, 1.0f, 1.0f);
                g.fill(hueX + i, hueY, hueX + i + 1, hueY + hueH, rgb | 0xFF000000);
            }

            int huePinX = hueX + Math.round(selectedHue * hueW);
            g.fill(huePinX - 3, hueY - 1, huePinX + 4, hueY + hueH + 1, 0xFFFFFFFF);
            int pureHueRgb = java.awt.Color.HSBtoRGB(selectedHue, 1.0f, 1.0f);
            g.fill(huePinX - 2, hueY, huePinX + 3, hueY + hueH, pureHueRgb | 0xFF000000);

            // 3. Hex Code Input Box
            int hexX = boxX + 15;
            int hexY = hueY + hueH + 5;
            int hexW = boxW - 30;
            int hexH = 13;

            g.fill(hexX, hexY, hexX + hexW, hexY + hexH, 0xFF12121A);
            int borderCol = hexInputFocused ? 0xFFBB55FF : 0xFF353545;
            g.fill(hexX, hexY, hexX + hexW, hexY + 1, borderCol);
            g.fill(hexX, hexY + hexH - 1, hexX + hexW, hexY + hexH, borderCol);
            g.fill(hexX, hexY, hexX + 1, hexY + hexH, borderCol);
            g.fill(hexX + hexW - 1, hexY, hexX + hexW, hexY + hexH, borderCol);

            String displayVal = (curHex != null && curHex.startsWith("#") ? curHex.substring(1) : "FF55AA");
            boolean cursorVisible = hexInputFocused && (System.currentTimeMillis() / 400 % 2 == 0);
            g.text(font, displayVal + (cursorVisible ? "§f|" : ""), hexX + 8, hexY + 2, COL_TEXT);
        }

        // ── Catégorie 2 : Player Size ──
        int cat2Y1 = cat1Y2 + (colorPickerExpanded ? (ROW_H * 2 + presetDropH + 96) : 0);
        int cat2Y2 = cat2Y1 + CAT_H;

        if (mx >= px && mx < px + PANEL_W && my >= cat2Y1 && my < cat2Y2)
            g.fill(px, cat2Y1, px + PANEL_W, cat2Y2, COL_HOVER);
        g.fill(px, cat2Y1, px + PANEL_W, cat2Y2, COL_CAT_BG);
        g.fill(px, cat2Y1, px + 2, cat2Y2, sizeConfigExpanded ? COL_ACCENT : 0x50BB55FF);
        g.text(font, (sizeConfigExpanded ? "§7▼ " : "§7▶ ") + "§bPlayer Size",
                px + INDENT_1, cat2Y1 + 3, COL_CAT_FG);
        g.fill(px, cat2Y2 - 1, px + PANEL_W, cat2Y2, COL_SEP);

        if (sizeConfigExpanded) {
            int ry = cat2Y2;
            if (mx >= px && mx < px + PANEL_W && my >= ry && my < ry + ROW_H) g.fill(px, ry, px + PANEL_W, ry + ROW_H, COL_HOVER);
            g.fill(px, ry + ROW_H - 1, px + PANEL_W, ry + ROW_H, COL_SEP);
            g.fill(px + INDENT_1, ry + ROW_H / 2, px + INDENT_1 + 4, ry + ROW_H / 2 + 1, 0x60BB55FF);
            g.text(font, "Enable", px + INDENT_1 + 7, ry + 2, COL_TEXT);

            // Toggle Pill for Player Size
            boolean sizeEnabled = ModConfig.INSTANCE.playerSizeEnabled;
            int pillW = 16, pillH = 7;
            int pillX = px + PANEL_W - pillW - 5;
            int pillY = ry + (ROW_H - pillH) / 2;
            g.fill(pillX, pillY, pillX + pillW, pillY + pillH, sizeEnabled ? COL_ON : COL_OFF);
            int knobX = sizeEnabled ? pillX + pillW - pillH : pillX;
            g.fill(knobX, pillY, knobX + pillH, pillY + pillH, 0xFFFFFFFF);

            renderSizeSlider(g, font, px, cat2Y2 + ROW_H,   "Size X", ModConfig.INSTANCE.playerSizeX, mx, my, 0);
            renderSizeSlider(g, font, px, cat2Y2 + 2*ROW_H, "Size Y", ModConfig.INSTANCE.playerSizeY, mx, my, 1);
            renderSizeSlider(g, font, px, cat2Y2 + 3*ROW_H, "Size Z", ModConfig.INSTANCE.playerSizeZ, mx, my, 2);
        }

        // ── Catégorie 3 : Song Config ──
        int cat3Y1 = cat2Y2 + (sizeConfigExpanded ? (ROW_H * 4) : 0);
        int cat3Y2 = cat3Y1 + CAT_H;

        if (mx >= px && mx < px + PANEL_W && my >= cat3Y1 && my < cat3Y2)
            g.fill(px, cat3Y1, px + PANEL_W, cat3Y2, COL_HOVER);
        g.fill(px, cat3Y1, px + PANEL_W, cat3Y2, COL_CAT_BG);
        g.fill(px, cat3Y1, px + 2, cat3Y2, songConfigExpanded ? COL_ACCENT : 0x50BB55FF);
        g.text(font, (songConfigExpanded ? "§7▼ " : "§7▶ ") + "§bSong Config",
                px + INDENT_1, cat3Y1 + 3, COL_CAT_FG);
        g.fill(px, cat3Y2 - 1, px + PANEL_W, cat3Y2, COL_SEP);

        // Body Song Config (si déplié)
        if (songConfigExpanded) {
            int r1y1 = cat3Y2;
            int r1y2 = r1y1 + ROW_H;
            if (mx >= px && mx < px + PANEL_W && my >= r1y1 && my < r1y2)
                g.fill(px, r1y1, px + PANEL_W, r1y2, COL_HOVER);
            g.fill(px, r1y2 - 1, px + PANEL_W, r1y2, COL_SEP);
            g.fill(px + INDENT_1, r1y1 + ROW_H / 2, px + INDENT_1 + 4, r1y1 + ROW_H / 2 + 1, 0x60BB55FF);

            String providerStr = ModConfig.INSTANCE.musicProvider != null ? ModConfig.INSTANCE.musicProvider : "None";
            String arrow = platformDropdownOpen ? "§7▼" : "§7▶";
            g.text(font, "Platform: §e" + providerStr + " " + arrow, px + INDENT_1 + 7, r1y1 + 2, COL_TEXT);

            int currentY = r1y2;

            if (platformDropdownOpen) {
                String[] options = new String[]{"None", "YouTube Music", "Spotify", "Deezer"};
                for (int i = 0; i < options.length; i++) {
                    int optY1 = currentY;
                    int optY2 = optY1 + ROW_H;
                    if (mx >= px && mx < px + PANEL_W && my >= optY1 && my < optY2)
                        g.fill(px, optY1, px + PANEL_W, optY2, COL_HOVER);
                    g.fill(px, optY2 - 1, px + PANEL_W, optY2, COL_SEP);

                    boolean isSelected = options[i].equalsIgnoreCase(providerStr);
                    String prefix = isSelected ? "  §a✔ " : "    ";
                    g.text(font, prefix + "§f" + options[i], px + INDENT_2, optY1 + 2, isSelected ? 0xFF55FF55 : COL_TEXT);
                    currentY = optY2;
                }
            }

            int r2y1 = currentY;
            int r2y2 = r2y1 + ROW_H;
            g.fill(px, r2y2 - 1, px + PANEL_W, r2y2, COL_SEP);
            g.fill(px + INDENT_1, r2y1 + ROW_H / 2, px + INDENT_1 + 4, r2y1 + ROW_H / 2 + 1, 0x60BB55FF);

            boolean isNone = "None".equalsIgnoreCase(providerStr);
            boolean isNativeWin = "Deezer".equalsIgnoreCase(providerStr) || "Spotify".equalsIgnoreCase(providerStr);
            boolean isConnected = isNativeWin || (ModConfig.INSTANCE.authToken != null && !ModConfig.INSTANCE.authToken.isEmpty());

            String statusText = isNone ? "§7None Selected" : (isNativeWin ? "§aWindows Native" : (isConnected ? "§aConnected" : "§cDisconnected"));
            g.text(font, "Status: " + statusText, px + INDENT_1 + 7, r2y1 + 2, COL_TEXT);

            int r3y1 = r2y2;
            int r3y2 = r3y1 + ROW_H;
            if (mx >= px && mx < px + PANEL_W && my >= r3y1 && my < r3y2)
                g.fill(px, r3y1, px + PANEL_W, r3y2, COL_HOVER);
            g.fill(px, r3y2 - 1, px + PANEL_W, r3y2, COL_SEP);
            g.fill(px + INDENT_1, r3y1 + ROW_H / 2, px + INDENT_1 + 4, r3y1 + ROW_H / 2 + 1, 0x60BB55FF);

            String actionText = isNone ? "§7Select a platform above" : (isNativeWin ? "§7Auto-detection enabled" : "§e🔗 Connect " + providerStr);
            g.text(font, actionText, px + INDENT_1 + 7, r3y1 + 2, COL_TEXT);
        }
        
        dropH = (songConfigExpanded && platformDropdownOpen) ? (ROW_H * 4) : 0;
        totalH = cat3Y2 - py;
        if (songConfigExpanded) {
            totalH += ROW_H * 3 + dropH;
        }

        g.fill(px, py + totalH - 1, px + PANEL_W, py + totalH, 0x40BB55FF);
    }
    
    private void renderSizeSlider(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int px, int ry, String label, float value, int mx, int my, int index) {
        int ry2 = ry + ROW_H;
        if (mx >= px && mx < px + PANEL_W && my >= ry && my < ry2)
            g.fill(px, ry, px + PANEL_W, ry2, COL_HOVER);
        g.fill(px, ry2 - 1, px + PANEL_W, ry2, COL_SEP);
        
        g.text(font, label, px + INDENT_1, ry + 2, COL_TEXT);
        
        String valStr = String.format(java.util.Locale.US, "%.2f", value);
        int valW = font.width(valStr);
        g.text(font, valStr, px + PANEL_W - valW - INDENT_1, ry + 2, COL_TEXT);
        
        int sliderX = px + PANEL_W / 3;
        int sliderW = PANEL_W / 2;
        int sliderY = ry + ROW_H / 2;
        
        g.fill(sliderX, sliderY, sliderX + sliderW, sliderY + 1, 0xFF555555);
        
        float pct = (value - (-1.0f)) / (3.0f - (-1.0f));
        int knobX = sliderX + (int)(pct * sliderW);
        
        g.fill(sliderX, sliderY, knobX, sliderY + 1, COL_ACCENT);
        g.fill(knobX - 2, sliderY - 2, knobX + 2, sliderY + 3, 0xFFFFFFFF);
    }

    // ── CLICS ──────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        int mx = (int) event.x();
        int my = (int) event.y();
        hexInputFocused = false;

        if (mx >= commandeX && mx < commandeX + PANEL_W) {

            // Niveau 1 : Party Commands
            int catY = START_Y + HEADER_H;
            if (my >= catY && my < catY + CAT_H) {
                partyCmdExpanded = !partyCmdExpanded;
                if (!partyCmdExpanded) cmdExpanded = false;
                return true;
            }

            if (partyCmdExpanded) {
                int toggleBase = catY + CAT_H;

                // Toggles All/Guild/Party Chat
                if (my >= toggleBase && my < toggleBase + ROW_H) {
                    ModConfig.INSTANCE.enableAc = !ModConfig.INSTANCE.enableAc;
                    ModConfig.INSTANCE.save(); return true;
                }
                if (my >= toggleBase + ROW_H && my < toggleBase + ROW_H * 2) {
                    ModConfig.INSTANCE.enableGc = !ModConfig.INSTANCE.enableGc;
                    ModConfig.INSTANCE.save(); return true;
                }
                if (my >= toggleBase + ROW_H * 2 && my < toggleBase + ROW_H * 3) {
                    ModConfig.INSTANCE.enablePc = !ModConfig.INSTANCE.enablePc;
                    ModConfig.INSTANCE.save(); return true;
                }

                // Niveau 2 : Commands
                int subY = toggleBase + 3 * ROW_H;
                if (my >= subY && my < subY + SUBCAT_H) {
                    cmdExpanded = !cmdExpanded;
                    return true;
                }

                if (cmdExpanded) {
                    int cmdY = subY + SUBCMD_H;
                    if (my >= cmdY && my < cmdY + SUBCMD_H) {
                        ModConfig.INSTANCE.enableSong = !ModConfig.INSTANCE.enableSong;
                        ModConfig.INSTANCE.save();
                        return true;
                    }
                }
            }
        } else if (mx >= miscX && mx < miscX + PANEL_W) {
            int basey = START_Y + HEADER_H;
            int cat1Y1 = basey;
            int cat1Y2 = basey + CAT_H;

            // Catégorie 1 : Couleur Pseudo
            if (my >= cat1Y1 && my < cat1Y2) {
                colorPickerExpanded = !colorPickerExpanded;
                return true;
            }

            if (colorPickerExpanded) {
                int r0y1 = cat1Y2;
                int r0y2 = cat1Y2 + ROW_H;

                // Sub-row 1 : Toggle Activer
                if (my >= r0y1 && my < r0y2) {
                    ModConfig.INSTANCE.enableNameColor = !ModConfig.INSTANCE.enableNameColor;
                    ModConfig.INSTANCE.save();
                    com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
                    return true;
                }

                int r01y1 = r0y2;
                int r01y2 = r01y1 + ROW_H;

                // Sub-row 2 : Preset dropdown toggle
                if (my >= r01y1 && my < r01y2) {
                    presetDropdownOpen = !presetDropdownOpen;
                    return true;
                }

                // Dropdown items
                int presetDropH = 0;
                if (presetDropdownOpen) {
                    var colors = com.songgka.client.color.NameColorManager.COLORS;
                    for (int i = 0; i < colors.length; i++) {
                        int dy1 = r01y2 + i * ROW_H;
                        int dy2 = dy1 + ROW_H;
                        if (my >= dy1 && my < dy2) {
                            ModConfig.INSTANCE.nameColorIndex = i;
                            ModConfig.INSTANCE.save();
                            presetDropdownOpen = false;
                            com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
                            return true;
                        }
                    }
                    presetDropH = colors.length * ROW_H;
                }

                int dropY = r01y2 + presetDropH + 4;
                int boxX = miscX + INDENT_1;
                int boxY = dropY;
                int boxW = PANEL_W - INDENT_1 * 2;
                int boxH = 55;

                // 1. Click on 2D SV Box
                if (mx >= boxX && mx < boxX + boxW && my >= boxY && my < boxY + boxH) {
                    updateColorFromSV(mx - boxX, my - boxY, boxW, boxH);
                    return true;
                }

                // 2. Click on 1D Hue Bar
                int hueX = boxX;
                int hueY = boxY + boxH + 5;
                int hueW = boxW;
                int hueH = 9;

                if (mx >= hueX && mx < hueX + hueW && my >= hueY && my < hueY + hueH) {
                    updateColorFromHue(mx - hueX, hueW);
                    return true;
                }

                // 3. Click on Hex Input Box
                int hexX = boxX + 15;
                int hexY = hueY + hueH + 5;
                int hexW = boxW - 30;
                int hexH = 13;

                if (mx >= hexX && mx < hexX + hexW && my >= hexY && my < hexY + hexH) {
                    hexInputFocused = true;
                    return true;
                }
            }

            // Catégorie 2 : Player Size
            int cat2Y1 = cat1Y2 + (colorPickerExpanded ? (ROW_H * 2 + (presetDropdownOpen ? com.songgka.client.color.NameColorManager.COLORS.length * ROW_H : 0) + 96) : 0);
            int cat2Y2 = cat2Y1 + CAT_H;

            if (my >= cat2Y1 && my < cat2Y2) {
                sizeConfigExpanded = !sizeConfigExpanded;
                return true;
            }

            if (sizeConfigExpanded) {
                int ryToggle = cat2Y2;
                if (my >= ryToggle && my < ryToggle + ROW_H) {
                    ModConfig.INSTANCE.playerSizeEnabled = !ModConfig.INSTANCE.playerSizeEnabled;
                    ModConfig.INSTANCE.save();
                    return true;
                }

                int sliderX = miscX + PANEL_W / 3;
                int sliderW = PANEL_W / 2;
                
                for (int i = 0; i < 3; i++) {
                    int ry = cat2Y2 + ROW_H + i * ROW_H;
                    if (my >= ry && my < ry + ROW_H && mx >= sliderX && mx <= sliderX + sliderW) {
                        draggingSlider = i;
                        updateSliderFromMouse(mx, sliderX, sliderW);
                        return true;
                    }
                }
            }

            // Catégorie 3 : Song Config
            int cat3Y1 = cat2Y2 + (sizeConfigExpanded ? (ROW_H * 4) : 0);
            int cat3Y2 = cat3Y1 + CAT_H;

            if (my >= cat3Y1 && my < cat3Y2) {
                songConfigExpanded = !songConfigExpanded;
                return true;
            }

            if (songConfigExpanded) {
                int r1y1 = cat3Y2;
                int r1y2 = r1y1 + ROW_H;

                // Click Platform row: toggle dropdown
                if (my >= r1y1 && my < r1y2) {
                    platformDropdownOpen = !platformDropdownOpen;
                    return true;
                }

                int currentY = r1y2;

                if (platformDropdownOpen) {
                    String[] options = new String[]{"None", "YouTube Music", "Spotify", "Deezer"};
                    for (int i = 0; i < options.length; i++) {
                        int optY1 = currentY;
                        int optY2 = optY1 + ROW_H;
                        if (my >= optY1 && my < optY2) {
                            ModConfig.INSTANCE.musicProvider = options[i];
                            ModConfig.INSTANCE.save();
                            platformDropdownOpen = false;
                            return true;
                        }
                        currentY = optY2;
                    }
                }

                int r3y1 = currentY + ROW_H;
                int r3y2 = r3y1 + ROW_H;

                // Click Connexion
                if (my >= r3y1 && my < r3y2) {
                    String p = ModConfig.INSTANCE.musicProvider;
                    if ("YTM".equalsIgnoreCase(p) || "YouTube Music".equalsIgnoreCase(p)) {
                        com.songgka.client.SonggkaClient.requestPairing();
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(event, focused);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (draggingSlider != -1) {
            int sliderX = miscX + PANEL_W / 3;
            int sliderW = PANEL_W / 2;
            updateSliderFromMouse(mx, sliderX, sliderW);
            return true;
        }

        if (colorPickerExpanded && mx >= miscX && mx < miscX + PANEL_W) {
            int basey = START_Y + HEADER_H;
            int cat1Y2 = basey + CAT_H;
            int r0y2 = cat1Y2 + ROW_H;   // Enable row
            int r01y2 = r0y2 + ROW_H;    // Preset row
            int presetDropH = presetDropdownOpen ? (ROW_H * com.songgka.client.color.NameColorManager.COLORS.length) : 0;
            int dropY = r01y2 + presetDropH + 4;
            int boxX = miscX + INDENT_1;
            int boxY = dropY;
            int boxW = PANEL_W - INDENT_1 * 2;
            int boxH = 55;

            if (my >= boxY && my < boxY + boxH) {
                updateColorFromSV(mx - boxX, my - boxY, boxW, boxH);
                return true;
            }

            int hueX = boxX;
            int hueY = boxY + boxH + 5;
            int hueW = boxW;
            int hueH = 9;

            if (my >= hueY && my < hueY + hueH) {
                updateColorFromHue(mx - hueX, hueW);
                return true;
            }
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }
    
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (draggingSlider != -1) {
            draggingSlider = -1;
            ModConfig.INSTANCE.save();
            return true;
        }
        return super.mouseReleased(event);
    }



    private void updateSliderFromMouse(int mx, int sliderX, int sliderW) {
        float pct = (float)(mx - sliderX) / sliderW;
        pct = Math.max(0.0f, Math.min(1.0f, pct));
        float val = -1.0f + pct * 4.0f; // range [-1, 3]
        if (draggingSlider == 0) ModConfig.INSTANCE.playerSizeX = val;
        else if (draggingSlider == 1) ModConfig.INSTANCE.playerSizeY = val;
        else if (draggingSlider == 2) ModConfig.INSTANCE.playerSizeZ = val;
    }

    private void updateColorFromSV(int relX, int relY, int boxW, int boxH) {
        float sat = Math.max(0.0f, Math.min(1.0f, (float) relX / (float) boxW));
        float val = Math.max(0.0f, Math.min(1.0f, 1.0f - ((float) relY / (float) boxH)));
        int rgb = java.awt.Color.HSBtoRGB(selectedHue, sat, val) & 0xFFFFFF;
        ModConfig.INSTANCE.customHexColor = String.format("#%06X", rgb);
        ModConfig.INSTANCE.nameColorIndex = 2;
        ModConfig.INSTANCE.save();
        com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
    }

    private void updateColorFromHue(int relX, int hueW) {
        selectedHue = Math.max(0.0f, Math.min(1.0f, (float) relX / (float) hueW));
        // Maintain current sat & val
        String curHex = ModConfig.INSTANCE.customHexColor;
        float sat = 1.0f, val = 1.0f;
        if (curHex != null && curHex.startsWith("#") && curHex.length() == 7) {
            try {
                int currentRgb = Integer.parseInt(curHex.substring(1), 16);
                float[] hsb = java.awt.Color.RGBtoHSB((currentRgb >> 16) & 0xFF, (currentRgb >> 8) & 0xFF, currentRgb & 0xFF, null);
                sat = hsb[1];
                val = hsb[2];
            } catch (Exception ignored) {}
        }
        int rgb = java.awt.Color.HSBtoRGB(selectedHue, sat, val) & 0xFFFFFF;
        ModConfig.INSTANCE.customHexColor = String.format("#%06X", rgb);
        ModConfig.INSTANCE.nameColorIndex = 2;
        ModConfig.INSTANCE.save();
        com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (hexInputFocused) {
            String cur = ModConfig.INSTANCE.customHexColor;
            if (cur == null || !cur.startsWith("#")) cur = "#";

            char chr = (char) event.codepoint();
            char c = Character.toUpperCase(chr);
            if (c == '#' && !cur.startsWith("#")) {
                cur = "#" + cur;
            } else if ((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F')) {
                if (cur.length() < 7) {
                    cur = cur + c;
                }
            }

            ModConfig.INSTANCE.customHexColor = cur;
            ModConfig.INSTANCE.nameColorIndex = 2;
            ModConfig.INSTANCE.save();
            com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (hexInputFocused) {
            if (event.key() == 259) { // GLFW_KEY_BACKSPACE
                String cur = ModConfig.INSTANCE.customHexColor;
                if (cur != null && cur.length() > 1) {
                    ModConfig.INSTANCE.customHexColor = cur.substring(0, cur.length() - 1);
                    ModConfig.INSTANCE.nameColorIndex = 2;
                    ModConfig.INSTANCE.save();
                    com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ModConfig.INSTANCE.save();
        com.songgka.client.color.NameColorManager.syncLocalPlayerColor();
        if (this.minecraft != null) this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
