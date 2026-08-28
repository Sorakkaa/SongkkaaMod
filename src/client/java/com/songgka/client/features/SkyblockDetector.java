package com.songgka.client.features;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;

public class SkyblockDetector {

    public static boolean isInF7OrM7 = false;
    private static int tickCounter = 0;

    public static void onClientTick(Minecraft mc) {
        tickCounter++;
        // On vérifie seulement toutes les 20 ticks (1 seconde) pour ne pas lag
        if (tickCounter >= 20) {
            tickCounter = 0;
            updateStatus(mc);
        }
    }

    private static void updateStatus(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            isInF7OrM7 = false;
            return;
        }

        boolean newState = false;
        
        Scoreboard scoreboard = mc.level.getScoreboard();
        if (scoreboard != null) {
            Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (objective != null) {
                String title = objective.getDisplayName().getString();
                String cleanTitle = title.replaceAll("(?i)§[0-9a-fk-or]", "");
                if (cleanTitle.contains("SKYBLOCK") || cleanTitle.contains("SKIBLOCK")) {
                    for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                        String owner = entry.owner();
                        String displayText = entry.display() != null ? entry.display().getString() : "";
                        
                        PlayerTeam team = scoreboard.getPlayersTeam(owner);
                        String prefix = team != null ? team.getPlayerPrefix().getString() : "";
                        String suffix = team != null ? team.getPlayerSuffix().getString() : "";
                        
                        String fullLine = prefix + owner + suffix + displayText;
                        String cleanLine = fullLine.replaceAll("(?i)§[0-9a-fk-or]", "");

                        if (cleanLine.contains("(F7)") || cleanLine.contains("(M7)")) {
                            newState = true;
                            break;
                        }
                    }
                }
            }
        }
        
        if (newState != isInF7OrM7) {
            isInF7OrM7 = newState;
            if (mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        }
    }
}
