package com.songgka.client.features;

import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerScoreEntry;

public class TestScoreboard {
    public void test(Scoreboard s, Objective o) {
        for (PlayerScoreEntry entry : s.listPlayerScores(o)) {
            System.out.println(entry.owner());
            System.out.println(entry.display());
        }
    }
}
