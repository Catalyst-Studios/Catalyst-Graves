package com.natesky9.catalystgraves.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record CatalogueTooltipData(Component text) implements TooltipComponent {
}