package com.natesky9.DataGen;

import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.function.Consumer;

public class CGAdvancementProvider implements AdvancementProvider.AdvancementGenerator {
    public static ResourceLocation ADVANCEMENT_ROOT = name("root");
    public static ResourceLocation SIMPLE_GRAVE = name("simple_grave");
    public static ResourceLocation LESSER_VITALITY = name("lesser_vitality");
    public static ResourceLocation GREATER_VITALITY = name("greater_vitality");
    public static ResourceLocation ILLUMINATION = name("illumination");
    public static ResourceLocation DESECRATE = name("desecrate");
    public static ResourceLocation ORGANIZATION = name("organization");
    public static ResourceLocation CORPOREAL_RECALL = name("corporeal_recall");
    public static ResourceLocation SOULBOUND = name("soulbound_enchanting");
    public static ResourceLocation WIP = name("wip");

    static Criterion<ImpossibleTrigger.TriggerInstance> unlock = CriteriaTriggers.IMPOSSIBLE
            .createCriterion(new ImpossibleTrigger.TriggerInstance());
    //
    public static final AdvancementHolder root = Advancement.Builder.advancement()
            .display(Items.SKELETON_SKULL, Component.translatable("advancement.catalystgraves.root"),
                    Component.translatable("advancement.catalystgraves.root.description"),
                    ResourceLocation.withDefaultNamespace("textures/block/soul_soil.png"),
                    AdvancementType.TASK,true,false,false)
            .addCriterion("dark_in_here", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.location()
                    .setLight(LightPredicate.Builder.light()
                    .setComposite(MinMaxBounds.Ints.between(0,9)))))
                    .build(ADVANCEMENT_ROOT);
    public static final AdvancementHolder simple_grave = Advancement.Builder.advancement()
            .display(Items.CHEST, Component.translatable("advancement.catalystgraves.grave"),
                    Component.translatable("advancement.catalystgraves.grave.description"),null,
                    AdvancementType.GOAL,true,false,false)
            .addCriterion("unlock_grave",unlock)
            .parent(root)
            .build(SIMPLE_GRAVE);
    public static final AdvancementHolder lesser_vitality = Advancement.Builder.advancement()
            .display(Items.APPLE, Component.translatable("advancement.catalystgraves.vitality"),
                    Component.translatable("advancement.catalystgraves.vitality.description"),null,
                    AdvancementType.GOAL,true,false,false)
            .addCriterion("unlock_vitality",unlock)
            .parent(simple_grave)
            .build(LESSER_VITALITY);
    public static final AdvancementHolder greater_vitality = Advancement.Builder.advancement()
            .display(Items.GOLDEN_APPLE, Component.translatable("advancement.catalystgraves.greater_vitality"),
                    Component.translatable("advancement.catalystgraves.greater_vitality.description"),null,
                    AdvancementType.CHALLENGE,true,false,false)
            .addCriterion("unlock_greater_vitality",unlock)
            .parent(lesser_vitality)
            .build(GREATER_VITALITY);
    public static final AdvancementHolder wip = Advancement.Builder.advancement()
            .display(Items.NETHER_STAR, Component.translatable("advancement.catalystgraves.cheat_death"),
                    Component.translatable("advancement.catalystgraves.cheat_death.description"),null,
                    AdvancementType.CHALLENGE,true,false,true)
            .addCriterion("secret_feature",unlock)
            .parent(greater_vitality)
            .build(WIP);
    public static final AdvancementHolder illumination = Advancement.Builder.advancement()
            .display(Items.LANTERN, Component.translatable("advancement.catalystgraves.illumination"),
                    Component.translatable("advancement.catalystgraves.illumination.description"),null,
                    AdvancementType.GOAL,true,false,false)
            .addCriterion("unlock_illumination",unlock)
            .parent(simple_grave)
            .build(ILLUMINATION);
    public static final AdvancementHolder desecrate = Advancement.Builder.advancement()
            .display(Items.GOLDEN_SHOVEL, Component.translatable("advancement.catalystgraves.desecrate"),
                    Component.translatable("advancement.catalystgraves.desecrate.description"),null,
                    AdvancementType.GOAL,true,false,false)
            .addCriterion("unlock_desecrate",unlock)
            .parent(simple_grave)
            .build(DESECRATE);
    public static final AdvancementHolder organized_coffin_disposition = Advancement.Builder.advancement()
            .display(Items.ARMOR_STAND, Component.translatable("advancement.catalystgraves.organization"),
                    Component.translatable("advancement.catalystgraves.organization.description"),null,
                    AdvancementType.CHALLENGE,true,false,false)
            .addCriterion("unlock_organization",unlock)
            .parent(desecrate)
            .build(ORGANIZATION);
    public static final AdvancementHolder recall = Advancement.Builder.advancement()
            .display(Items.ENDER_EYE, Component.translatable("advancement.catalystgraves.corporeal_recall"),
                    Component.translatable("advancement.catalystgraves.corporeal_recall.description"),null,
                    AdvancementType.CHALLENGE,true,false,false)
            .addCriterion("unlock_recall",unlock)
            .parent(desecrate)
            .build(CORPOREAL_RECALL);
    public static final AdvancementHolder soulbound = Advancement.Builder.advancement()
            .display(Blocks.ENCHANTING_TABLE,Component.translatable("advancement.catalystgraves.soul_binding"),
                    Component.translatable("advancement.catalystgraves.soul_binding.description"),null,
                    AdvancementType.GOAL,true,false,false)
            .addCriterion("unlock_enchanting", InventoryChangeTrigger.TriggerInstance.hasItems(Items.ENCHANTING_TABLE,Items.ENCHANTED_BOOK))
            .parent(simple_grave)
            .build(SOULBOUND);
    //public static final AdvancementHolder revive = Advancement.Builder.advancement()
    //        .display(Items.NETHER_STAR, Component.literal("R"))

    @Override
    public void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer, ExistingFileHelper existingFileHelper) {
        //add advancements here, after initializing them above
        consumer.accept(root);
        consumer.accept(simple_grave);
        consumer.accept(lesser_vitality);
        consumer.accept(greater_vitality);
        consumer.accept(illumination);
        consumer.accept(organized_coffin_disposition);
        consumer.accept(desecrate);
        consumer.accept(recall);
        consumer.accept(soulbound);
        consumer.accept(wip);
    }
    static ResourceLocation name(String string)
    {
        return ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,string);
    }
}
