package com.example.catalystgraves.Item;

import com.example.catalystgraves.Menus.BindingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class BindingChapter implements MenuProvider {
    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.enchanting");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new BindingMenu(i, inventory);
    }
}
