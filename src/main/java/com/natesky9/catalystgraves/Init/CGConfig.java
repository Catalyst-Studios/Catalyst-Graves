package com.natesky9.catalystgraves.Init;

import java.util.List;

import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

@SuppressWarnings("null")
@Mod(value = CatalystGraves.MODID, dist = Dist.CLIENT)
public class CGConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> TRANSMUTE_ITEM =
        BUILDER.comment("The item to hold while sleeping to get the mod book")
            .define("sleepItem", "minecraft:writable_book");
    public static final ModConfigSpec.ConfigValue<Integer> DESECRATE_COST =
        BUILDER.comment("Levels to unlock rapid looting")
            .defineInRange("desecrateLevelCost", 3, 0, 100);
    public static final ModConfigSpec.ConfigValue<Integer> ORGANIZATION_COST =
        BUILDER.comment("Levels to unlock organized looting")
            .defineInRange("organizationLevelCost", 5, 0, 100);
    public static final ModConfigSpec.ConfigValue<Integer> LESSER_VITALITY_COST =
        BUILDER.comment("Levels to unlock lesser vitality")
            .defineInRange("lesserVitalityLevelCost", 10, 0, 100);
    public static final ModConfigSpec.ConfigValue<Integer> GREATER_VITALITY_COST =
        BUILDER.comment("Levels to unlock greater vitality")
            .defineInRange("vitalityLevelCost", 15, 0, 100);
    public static final ModConfigSpec.ConfigValue<Integer> VITALITY_DURATION =
        BUILDER.comment("Duration in ticks of the Vitality hearts")
            .defineInRange("vitalityDuration", 1200, 20, 72000);
    public static final ModConfigSpec.ConfigValue<Integer> VITALITY_AMPLIFIER =
        BUILDER.comment("Potency of the Vitality hearts, with each level adding 2 hearts")
            .defineInRange("vitalityAmplifier", 2, 0, 64);
    public static final ModConfigSpec.ConfigValue<Integer> ILLUMINATION_COST =
        BUILDER.comment("Levels to unlock grave marking")
            .defineInRange("illuminationLevelCost", 4, 0, 100);
    public static final ModConfigSpec.ConfigValue<Integer> CORPOREAL_RECALL =
        BUILDER.comment("Levels to unlock corporeal recall")
            .defineInRange("corporealRecallLevelCost", 18, 0, 100);
    public static final ModConfigSpec.ConfigValue<Integer> SOULBOUND_COST =
        BUILDER.comment("Level per Soulbound enchant")
            .defineInRange("soulboundCost", 6, 0, 100);
    public static final ModConfigSpec.ConfigValue<Boolean> DOES_SOULBOUND_VANISH =
        BUILDER.comment("Should the enchantment be removed when it works")
            .define("isSoulBoundDisposable", false);
    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> NATIVE_SOULBOUNDS =
        BUILDER.comment("List of items to inherently give soulbound properties to")
            .defineListAllowEmpty("nativeSoulbounds", List.of("catalystgraves:catalogue"), () -> "", CGConfig::validateItemName);
    public static final ModConfigSpec.ConfigValue<Boolean> SPAWN_WITH_BOOK =
        BUILDER.comment("Spawn with book")
            .define("spawnWithBook", false);
    public static final ModConfigSpec.ConfigValue<Boolean> FREEBIE =
        BUILDER.comment("Skip the early game, giving the player: SIMPLE_GRAVE, DESECRATE, and ORGANIZATION out of the gate")
            .define("giveFreebies", true);
    public static final ModConfigSpec.ConfigValue<Boolean> PRIVATE_GRAVES =
        BUILDER.comment("Should graves be unlootable by others")
            .define("isGravePrivateProperty", true);
    public static final ModConfigSpec.ConfigValue<Integer> DISPLAYED_ITEMS =
        BUILDER.comment("Number of items to display on the grave")
            .define("itemDisplayCount", 3);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public CGConfig(ModContainer container)
    {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
