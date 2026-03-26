package com.natesky9.catalystgraves.Block;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.natesky9.catalystgraves.AdvancementLogic;
import com.natesky9.catalystgraves.Init.CGConfig;
import com.natesky9.catalystgraves.datagen.CGAdvancementProvider;

import net.minecraft.server.level.ServerPlayer;

/**
 * This is a simple class, just containing player UUID
 * This is a special grave on request, which some people 
 * can request
 */
public class RoyalGrave
{
    private static final Set<UUID> users = Set.of(
        convert("380df991-f603-344c-a090-369bad2a924a"), //Dev
        convert("92fa13e0-1d68-47ca-9847-fc644d330613"), //AlmanaX21
        convert("e02bb88f-9542-4982-b2c6-d6f55ea1b697") //Titop54
    );

    private static final Random rand = new Random();

    public static UUID convert(String s)
    {
        return UUID.fromString(s);
    }

    public static boolean contains(ServerPlayer p)
    {
        if(CGConfig.DISABLE_ROYAL_GRAVE.get()) return false;
        boolean result = users.contains(p.getUUID());
        if(result && !AdvancementLogic.hasAdvancement(p, CGAdvancementProvider.ROYAL_TOILET)) AdvancementLogic.grant(p, CGAdvancementProvider.ROYAL_TOILET);
        if(result) return true;
        boolean result2 = rand.nextInt(1,128) == 1;
        if(result2 && !AdvancementLogic.hasAdvancement(p, CGAdvancementProvider.ROYAL_TOILET)) AdvancementLogic.grant(p, CGAdvancementProvider.ROYAL_TOILET);
        return result2;
    }
}
