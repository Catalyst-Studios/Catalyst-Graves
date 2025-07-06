package com.example.catalystgraves.Init;

import com.example.DataGen.ModEnchantmentTagsProvider;
import com.example.DataGen.ModItemTagsProvider;
import com.example.catalystgraves.CatalystGraves;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> SOULBOUND = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "soulbound"));
    //
    public static void bootstrap(BootstrapContext<Enchantment> context)
    {
        //the soulbound enchantment
        HolderGetter<Item> holderItem = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> holderEnchantment = context.lookup(Registries.ENCHANTMENT);
        context.register(SOULBOUND,
                Enchantment.enchantment(Enchantment.definition(holderItem.getOrThrow(ModItemTagsProvider.SOULBOUND_APPLICABLE),
                        1,1,Enchantment.constantCost(60),
                        Enchantment.constantCost(60),
                        60, EquipmentSlotGroup.ANY))
                        .exclusiveWith(holderEnchantment.getOrThrow(ModEnchantmentTagsProvider.VANISHING_EXCLUSIVE))
                        .build(SOULBOUND.location()));
    }
}
