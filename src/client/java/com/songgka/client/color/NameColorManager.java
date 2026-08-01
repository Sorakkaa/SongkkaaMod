package com.songgka.client.color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.songgka.client.config.ModConfig;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NameColorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("Songkkaa");
    private static long lastDebugLog = 0;

    public static class ColorOption {
        public final String code;
        public final String name;
        public final int hexColor;

        public ColorOption(String code, String name, int hexColor) {
            this.code = code;
            this.name = name;
            this.hexColor = hexColor;
        }
    }

    public static final ColorOption[] COLORS = new ColorOption[]{
        new ColorOption("NONE",   "\u00A7o None",        0xFFAAAAAA),
        new ColorOption("CHROMA", "\uD83C\uDF08 Rainbow",    0xFF55FFFF),
        new ColorOption("HEX",    "\uD83C\uDFA8 Custom Hex", 0xFF55FFAA)
    };

    private static final String[] CHROMA_CODES = new String[]{"\u00A7c", "\u00A76", "\u00A7e", "\u00A7a", "\u00A7b", "\u00A79", "\u00A7d"};
    private static final Map<String, String> PLAYER_COLORS = new ConcurrentHashMap<>();
    public static final Map<String, float[]> PLAYER_SIZES = new ConcurrentHashMap<>();
    private static final String SYNC_URL = "https://kvdb.io/V1XN9Z3K8M4P7Q2L1S/songgka_colors";
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static boolean initialized = false;

    public static ColorOption getCurrentOption() {
        int idx = ModConfig.INSTANCE.nameColorIndex;
        if (idx < 0 || idx >= COLORS.length) {
            idx = 0;
        }
        return COLORS[idx];
    }

    public static String normalizeHex(String code) {
        if (code == null) return null;
        String s = code.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 6 && s.matches("[0-9A-Fa-f]{6}")) {
            return "#" + s.toUpperCase();
        }
        return null;
    }

    public static String getCurrentColorCode() {
        ColorOption opt = getCurrentOption();
        if (opt == null) return "";
        if ("NONE".equals(opt.code)) {
            return "";
        } else if ("CHROMA".equals(opt.code)) {
            return "CHROMA";
        } else {
            String norm = normalizeHex(ModConfig.INSTANCE.customHexColor);
            return norm != null ? norm : "#FFC6F9";
        }
    }

    public static void cycleColor() {
        ModConfig.INSTANCE.nameColorIndex = (ModConfig.INSTANCE.nameColorIndex + 1) % COLORS.length;
        ModConfig.INSTANCE.save();
        syncLocalPlayerColor();
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        syncLocalPlayerColor();

        SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                fetchRemoteColors();
            } catch (Exception ignored) {
            }
        }, 0, 3, TimeUnit.SECONDS);
    }



    private static String lastSyncedColor = null;

    public static void syncLocalPlayerColor() {
        CompletableFuture.runAsync(() -> {
            try {
                var mc = Minecraft.getInstance();
                if (mc.player == null) return;

                String uuid = mc.player.getUUID().toString();
                String name = mc.player.getScoreboardName();
                String colorCode = ModConfig.INSTANCE.enableNameColor ? getCurrentColorCode() : "";

                PLAYER_COLORS.put(uuid, colorCode);
                PLAYER_COLORS.put(name.toLowerCase(), colorCode);
                
                String syncKey = colorCode + "_" + ModConfig.INSTANCE.playerSizeX + "_" + ModConfig.INSTANCE.playerSizeY + "_" + ModConfig.INSTANCE.playerSizeZ;
                if (syncKey.equals(lastSyncedColor)) return;
                lastSyncedColor = syncKey;

                JsonObject fullData = new JsonObject();
                try {
                    URL getUrl = URI.create(ModConfig.INSTANCE.syncUrl).toURL();
                    HttpURLConnection getConn = (HttpURLConnection) getUrl.openConnection(java.net.Proxy.NO_PROXY);
                    getConn.setRequestMethod("GET");
                    getConn.setConnectTimeout(3000);
                    getConn.setReadTimeout(3000);
                    if (getConn.getResponseCode() == 200) {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(getConn.getInputStream(), StandardCharsets.UTF_8))) {
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) sb.append(line);
                            if (sb.length() > 0 && sb.charAt(0) == '{') {
                                fullData = JsonParser.parseString(sb.toString()).getAsJsonObject();
                            }
                        }
                    }
                } catch (Exception ignored) {}

                JsonObject userObj = new JsonObject();
                userObj.addProperty("username", name);
                userObj.addProperty("color", colorCode);
                userObj.addProperty("sizeX", ModConfig.INSTANCE.playerSizeX);
                userObj.addProperty("sizeY", ModConfig.INSTANCE.playerSizeY);
                userObj.addProperty("sizeZ", ModConfig.INSTANCE.playerSizeZ);
                fullData.add(uuid, userObj);
                fullData.add(name.toLowerCase(), userObj);

                URL putUrl = URI.create(ModConfig.INSTANCE.syncUrl).toURL();
                HttpURLConnection putConn = (HttpURLConnection) putUrl.openConnection(java.net.Proxy.NO_PROXY);
                putConn.setRequestMethod("PUT");
                putConn.setDoOutput(true);
                putConn.setConnectTimeout(3000);
                putConn.setReadTimeout(3000);
                putConn.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = putConn.getOutputStream()) {
                    os.write(fullData.toString().getBytes(StandardCharsets.UTF_8));
                }
                putConn.getResponseCode();
            } catch (Exception ignored) {
            }
        });
    }

    private static void fetchRemoteColors() {
        try {
            URL url = URI.create(ModConfig.INSTANCE.syncUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    if (sb.length() > 0 && sb.charAt(0) == '{') {
                        JsonObject obj = JsonParser.parseString(sb.toString()).getAsJsonObject();
                        for (String key : obj.keySet()) {
                            try {
                                JsonElement el = obj.get(key);
                                if (el != null && el.isJsonObject()) {
                                    JsonObject userObj = el.getAsJsonObject();
                                    
                                    var mc = Minecraft.getInstance();
                                    boolean isLocalPlayer = (mc.player != null && (key.equalsIgnoreCase(mc.player.getUUID().toString()) || key.equalsIgnoreCase(mc.player.getScoreboardName())));
                                    
                                    if (!isLocalPlayer) {
                                        if (userObj.has("color")) {
                                            String color = userObj.get("color").getAsString();
                                            PLAYER_COLORS.put(key.toLowerCase(), color);
                                            if (userObj.has("username")) {
                                                String uname = userObj.get("username").getAsString().toLowerCase();
                                                if (!uname.isEmpty()) {
                                                    PLAYER_COLORS.put(uname, color);
                                                }
                                            }
                                        }
                                        if (userObj.has("sizeX") && userObj.has("sizeY") && userObj.has("sizeZ")) {
                                            float sx = userObj.get("sizeX").getAsFloat();
                                            float sy = userObj.get("sizeY").getAsFloat();
                                            float sz = userObj.get("sizeZ").getAsFloat();
                                            PLAYER_SIZES.put(key.toLowerCase(), new float[]{sx, sy, sz});
                                            if (userObj.has("username")) {
                                                String uname = userObj.get("username").getAsString().toLowerCase();
                                                if (!uname.isEmpty()) {
                                                    PLAYER_SIZES.put(uname, new float[]{sx, sy, sz});
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static String getColorForPlayer(UUID uuid, String name) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.getUUID().equals(uuid) || mc.player.getScoreboardName().equalsIgnoreCase(name))) {
            return ModConfig.INSTANCE.enableNameColor ? getCurrentColorCode() : "";
        }

        String c = null;
        if (uuid != null && PLAYER_COLORS.containsKey(uuid.toString())) {
            c = PLAYER_COLORS.get(uuid.toString());
        } else if (name != null && PLAYER_COLORS.containsKey(name.toLowerCase())) {
            c = PLAYER_COLORS.get(name.toLowerCase());
        }

        if (c != null && !c.isEmpty()) {
            if ("CHROMA".equals(c)) {
                return getCurrentColorCode();
            }
            return c;
        }

        return null;
    }

    public static net.minecraft.network.chat.Component colorizeText(net.minecraft.network.chat.Component component) {
        if (component == null) return null;
        try {
            // Debug log every 5 seconds
            long now = System.currentTimeMillis();
            String plainText = component.getString();
            if (now - lastDebugLog > 5000 && plainText != null && plainText.toLowerCase().contains("sorakkaa")) {
                lastDebugLog = now;
                LOGGER.info("[Songkkaa DEBUG] colorizeText input: '{}'", plainText);
                LOGGER.info("[Songkkaa DEBUG] component tree: {}", dumpComponent(component, 0));
            }
            net.minecraft.network.chat.Component result = modifyComponent(component);
            if (now - lastDebugLog < 100 && plainText != null && plainText.toLowerCase().contains("sorakkaa")) {
                LOGGER.info("[Songkkaa DEBUG] colorizeText output: '{}'", result.getString());
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("[Songkkaa] Error in colorizeText", e);
            return component;
        }
    }

    private static String dumpComponent(net.minecraft.network.chat.Component comp, int depth) {
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(depth);
        var contents = comp.getContents();
        sb.append(indent).append("Type=").append(contents.getClass().getSimpleName());
        if (contents instanceof net.minecraft.network.chat.contents.PlainTextContents p) {
            sb.append(" text='").append(p.text()).append("'");
        } else if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents t) {
            sb.append(" key='").append(t.getKey()).append("' args=").append(t.getArgs().length);
            for (int i = 0; i < t.getArgs().length; i++) {
                Object arg = t.getArgs()[i];
                if (arg instanceof net.minecraft.network.chat.Component ac) {
                    sb.append("\n").append(dumpComponent(ac, depth+1));
                } else {
                    sb.append("\n").append(indent).append("  arg[").append(i).append("]=").append(arg);
                }
            }
        }
        sb.append(" style=").append(comp.getStyle());
        for (net.minecraft.network.chat.Component sib : comp.getSiblings()) {
            sb.append("\n").append(dumpComponent(sib, depth+1));
        }
        return sb.toString();
    }

    public static String stripMiniMessageTags(String text) {
        if (text == null || text.isEmpty()) return text;
        String s = text.replaceAll("<[^>]*#[0-9A-Fa-f\u00A7rRk-oK-O]{6,12}[^>]*>", "");
        s = s.replaceAll("<#/?[0-9A-Fa-f]{6}>", "");
        s = s.replaceAll("<(?:color|c|font|gradient|rainbow|hover|click):[^>]+>", "");
        s = s.replaceAll("</(?:color|c|font|gradient|rainbow|hover|click|bold|italic|underlined|strikethrough|obfuscated|reset)>", "");
        s = s.replaceAll("<(?:bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf|reset|r)>", "");
        s = s.replaceAll("</[^>]*>", "");
        return s;
    }

    private static boolean isUsernameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    private static net.minecraft.network.chat.Component modifyComponent(net.minecraft.network.chat.Component component) {
        if (component == null) return null;

        var mc = Minecraft.getInstance();
        java.util.List<String> targetNames = new java.util.ArrayList<>();
        Map<String, String> nameToColor = new java.util.HashMap<>();

        // Build from remote colors first
        for (Map.Entry<String, String> entry : PLAYER_COLORS.entrySet()) {
            String key = entry.getKey();
            String color = entry.getValue();
            if (color == null) color = "";
            if (!key.contains("-") && key.length() <= 16) {
                if (!targetNames.contains(key)) targetNames.add(key);
                nameToColor.put(key.toLowerCase(), color);
            }
        }

        // Local player always overrides remote ("" = None = strip tags, no color)
        if (mc.player != null) {
            String localName = mc.player.getScoreboardName();
            if (localName != null && !localName.isEmpty()) {
                String localColor = ModConfig.INSTANCE.enableNameColor ? getCurrentColorCode() : "";
                if (!targetNames.contains(localName)) targetNames.add(localName);
                nameToColor.put(localName.toLowerCase(), localColor);
            }
        }

        // Sort targetNames descending by length so longer names match before substrings (e.g. SorakkaaLover before Sorakkaa)
        targetNames.sort((a, b) -> Integer.compare(b.length(), a.length()));

        return recursivelyModify(component, targetNames, nameToColor);
    }

    private static net.minecraft.network.chat.Component recursivelyModify(
            net.minecraft.network.chat.Component component,
            java.util.List<String> targetNames,
            Map<String, String> nameToColor) {
        if (component == null) return null;

        boolean selfChanged = false;
        net.minecraft.network.chat.Component newSelf = null;

        var contents = component.getContents();
        if (contents instanceof net.minecraft.network.chat.contents.TranslatableContents trans) {
            Object[] args = trans.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean argsChanged = false;
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof net.minecraft.network.chat.Component argComp) {
                    net.minecraft.network.chat.Component modifiedArg = recursivelyModify(argComp, targetNames, nameToColor);
                    newArgs[i] = modifiedArg;
                    if (modifiedArg != argComp) argsChanged = true;
                } else if (arg instanceof String argStr) {
                    net.minecraft.network.chat.Component argComp = net.minecraft.network.chat.Component.literal(argStr);
                    net.minecraft.network.chat.Component modifiedArg = recursivelyModify(argComp, targetNames, nameToColor);
                    if (modifiedArg != argComp) {
                        newArgs[i] = modifiedArg;
                        argsChanged = true;
                    } else {
                        newArgs[i] = argStr;
                    }
                } else {
                    newArgs[i] = arg;
                }
            }
            if (argsChanged) {
                selfChanged = true;
                newSelf = net.minecraft.network.chat.Component.translatable(trans.getKey(), newArgs).withStyle(component.getStyle());
            }
        } else if (contents instanceof net.minecraft.network.chat.contents.PlainTextContents plain) {
            String rawText = plain.text();
            if (rawText != null && !rawText.isEmpty()) {
                String text = stripMiniMessageTags(rawText);
                if (!text.equals(rawText)) {
                    selfChanged = true;
                    newSelf = net.minecraft.network.chat.Component.literal(text).withStyle(component.getStyle());
                }

                String textLower = text.toLowerCase();
                for (String name : targetNames) {
                    if (name == null || name.isEmpty()) continue;
                    String nameLower = name.toLowerCase();

                    // Find index of name with word boundary check
                    int idx = -1;
                    int searchPos = 0;
                    while ((searchPos = textLower.indexOf(nameLower, searchPos)) != -1) {
                        String subAfter = textLower.substring(searchPos + nameLower.length());
                        if (subAfter.startsWith(" head]") || subAfter.startsWith(" head")) {
                            searchPos += nameLower.length();
                            continue;
                        }

                        boolean beforeOk = (searchPos == 0) || !isUsernameChar(text.charAt(searchPos - 1));
                        boolean afterOk = (searchPos + nameLower.length() == text.length()) || !isUsernameChar(text.charAt(searchPos + nameLower.length()));

                        if (beforeOk && afterOk) {
                            idx = searchPos;
                            break;
                        }
                        searchPos += nameLower.length();
                    }

                    if (idx != -1) {
                        String colorCode = nameToColor.get(nameLower);
                        var mc = Minecraft.getInstance();
                        boolean isSorakkaa = "sorakkaa".equalsIgnoreCase(nameLower);
                        boolean isMairuy = "mairuy".equalsIgnoreCase(nameLower);
                        if (colorCode != null && !colorCode.isEmpty()) {
                            selfChanged = true;
                            String originalCaseName = text.substring(idx, idx + name.length());
                            String before = text.substring(0, idx);
                            // Strip only trailing \u00A7X color/format codes right before the name
                            String cleanBefore = before.replaceAll("(\u00A7[0-9a-fA-Fk-rK-R])+$", "");
                            String after = text.substring(idx + name.length());

                            String normHex = normalizeHex(colorCode);
                            String finalBefore = cleanBefore;
                            if (!isSorakkaa) finalBefore = finalBefore.replaceAll("\u00A7[lL]", "");
                            if (!isMairuy) finalBefore = finalBefore.replaceAll("\u00A7[oO]", "");
                            String finalAfter = after;
                            if (!isSorakkaa) finalAfter = finalAfter.replaceAll("\u00A7[lL]", "");
                            if (!isMairuy) finalAfter = finalAfter.replaceAll("\u00A7[oO]", "");

                            if ("CHROMA".equalsIgnoreCase(colorCode)) {
                                net.minecraft.network.chat.MutableComponent builder = net.minecraft.network.chat.Component.literal("");
                                if (!finalBefore.isEmpty()) builder.append(net.minecraft.network.chat.Component.literal(finalBefore));
                                long step = System.currentTimeMillis() / 150;
                                for (int ci = 0; ci < originalCaseName.length(); ci++) {
                                    int colIdx = Math.abs((int) ((step + ci) % CHROMA_CODES.length));
                                    net.minecraft.network.chat.TextColor cc = net.minecraft.network.chat.TextColor.fromLegacyFormat(
                                        net.minecraft.ChatFormatting.getByCode(CHROMA_CODES[colIdx].charAt(1)));
                                    net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY;
                                    if (cc != null) st = st.withColor(cc);
                                    if (isSorakkaa) st = st.withBold(true);
                                    else st = st.withBold(false);
                                    if (isMairuy) st = st.withItalic(true);
                                    else st = st.withItalic(false);
                                    String charStr = String.valueOf(originalCaseName.charAt(ci));
                                    builder.append(net.minecraft.network.chat.Component.literal(charStr).withStyle(st));
                                }
                                if (!finalAfter.isEmpty()) builder.append(net.minecraft.network.chat.Component.literal(finalAfter));
                                newSelf = builder;
                            } else if (normHex != null) {
                                try {
                                    int hexInt = Integer.parseInt(normHex.substring(1), 16);
                                    net.minecraft.network.chat.TextColor tc = net.minecraft.network.chat.TextColor.fromRgb(hexInt);
                                    net.minecraft.network.chat.MutableComponent builder = net.minecraft.network.chat.Component.literal("");
                                    if (!finalBefore.isEmpty()) builder.append(net.minecraft.network.chat.Component.literal(finalBefore));
                                    net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY.withColor(tc);
                                    if (isSorakkaa) st = st.withBold(true);
                                    else st = st.withBold(false);
                                    if (isMairuy) st = st.withItalic(true);
                                    else st = st.withItalic(false);
                                    String nameStr = originalCaseName.replaceAll("\u00A7[lLoO]", "");
                                    builder.append(net.minecraft.network.chat.Component.literal(nameStr).withStyle(st));
                                    if (!finalAfter.isEmpty()) builder.append(net.minecraft.network.chat.Component.literal(finalAfter));
                                    newSelf = builder;
                                } catch (Exception ignored) {}
                            } else {
                                net.minecraft.ChatFormatting fmt = net.minecraft.ChatFormatting.getByCode(colorCode.length() > 1 ? colorCode.charAt(1) : 'r');
                                if (fmt != null) {
                                    net.minecraft.network.chat.TextColor tc = net.minecraft.network.chat.TextColor.fromLegacyFormat(fmt);
                                    net.minecraft.network.chat.MutableComponent builder = net.minecraft.network.chat.Component.literal("");
                                    if (!finalBefore.isEmpty()) builder.append(net.minecraft.network.chat.Component.literal(finalBefore));
                                    net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY.withColor(tc);
                                    if (isSorakkaa) st = st.withBold(true);
                                    else st = st.withBold(false);
                                    if (isMairuy) st = st.withItalic(true);
                                    else st = st.withItalic(false);
                                    String nameStr = originalCaseName.replaceAll("\u00A7[lLoO]", "");
                                    builder.append(net.minecraft.network.chat.Component.literal(nameStr).withStyle(st));
                                    if (!finalAfter.isEmpty()) builder.append(net.minecraft.network.chat.Component.literal(finalAfter));
                                    newSelf = builder;
                                }
                            }
                            break;
                        } else if (!text.equals(rawText)) {
                            selfChanged = true;
                            newSelf = net.minecraft.network.chat.Component.literal(text).withStyle(component.getStyle());
                            break;
                        }
                    }
                }
            }
        }
        java.util.List<net.minecraft.network.chat.Component> originalSiblings = component.getSiblings();
        java.util.List<net.minecraft.network.chat.Component> newSiblings = new java.util.ArrayList<>(originalSiblings.size());
        boolean siblingsChanged = false;

        for (net.minecraft.network.chat.Component sibling : originalSiblings) {
            net.minecraft.network.chat.Component modifiedSibling = recursivelyModify(sibling, targetNames, nameToColor);
            newSiblings.add(modifiedSibling);
            if (modifiedSibling != sibling) {
                siblingsChanged = true;
            }
        }

        if (selfChanged) {
            net.minecraft.network.chat.MutableComponent result = (net.minecraft.network.chat.MutableComponent) newSelf;
            for (net.minecraft.network.chat.Component s : newSiblings) {
                result.append(s);
            }
            return result;
        } else if (siblingsChanged) {
            net.minecraft.network.chat.MutableComponent result = component.copy();
            result.getSiblings().clear();
            for (net.minecraft.network.chat.Component s : newSiblings) {
                result.append(s);
            }
            return result;
        }

        return component;
    }

    // Legacy \u00A7-code chroma for plain chat text
    private static String getChromaName(String name) {
        StringBuilder sb = new StringBuilder();
        long step = (System.currentTimeMillis() / 150);
        for (int i = 0; i < name.length(); i++) {
            int colIdx = Math.abs((int) ((step + i) % CHROMA_CODES.length));
            sb.append(CHROMA_CODES[colIdx]).append(name.charAt(i));
        }
        sb.append("\u00A7r");
        return sb.toString();
    }
}
