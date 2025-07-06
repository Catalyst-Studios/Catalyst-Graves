package com.natesky9.catalystgraves;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class AdvancementLogic {
    public static void grant(ServerPlayer player, ResourceLocation resourceLocation)
    {
        //all this just to grant an advancement
        PlayerAdvancements advancements = player.getAdvancements();
        ServerAdvancementManager manager = player.server.getAdvancements();
        AdvancementHolder holder = manager.get(resourceLocation);
        Advancement advancement = manager.tree().get(holder).advancement();
        for (List<String> requirements:advancement.requirements().requirements())
        {
            for (String string:requirements)
            {
                advancements.award(holder,string);
            }
        }
    }

    public static boolean hasAdvancement(ServerPlayer player, ResourceLocation value)
    {
        ServerAdvancementManager manager = player.server.getAdvancements();

        AdvancementHolder holder = manager.get(value);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
