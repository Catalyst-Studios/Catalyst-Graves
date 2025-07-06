package com.natesky9.catalystgraves.Init;

import com.natesky9.DataGen.CGEnchantmentTagsProvider;
import com.natesky9.DataGen.CGItemTagsProvider;
import com.natesky9.catalystgraves.CatalystGraves;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class CGEnchantments {
    public static final ResourceKey<Enchantment> SOULBOUND = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "soulbound"));
    //
    public static void bootstrap(BootstrapContext<Enchantment> context)
    {
        //the soulbound enchantment
        HolderGetter<Item> holderItem = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> holderEnchantment = context.lookup(Registries.ENCHANTMENT);
        context.register(SOULBOUND,
                Enchantment.enchantment(Enchantment.definition(holderItem.getOrThrow(CGItemTagsProvider.SOULBOUND_APPLICABLE),
                        1,1,Enchantment.constantCost(60),
                        Enchantment.constantCost(60),
                        60, EquipmentSlotGroup.ANY))
                        .exclusiveWith(holderEnchantment.getOrThrow(CGEnchantmentTagsProvider.VANISHING_EXCLUSIVE))
                        .build(SOULBOUND.location()));
    }
}
