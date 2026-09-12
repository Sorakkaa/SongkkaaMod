package com.songgka.client.features;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class TestGui {
    public static void test(Minecraft client) {
        client.gui.setTimes(10, 60, 10);
        client.gui.setTitle(Component.literal("Test"));
    }
}
