package com.natesky9.catalystgraves.datagen;

import com.natesky9.catalystgraves.Init.CGBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("null")
public class CGRecipesProvider extends RecipeProvider
{
    public CGRecipesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider)
    {
        super(output, lookupProvider);
    }

    @Override
    protected void buildRecipes(RecipeOutput output)
    {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CGBlocks.DECORATIVE_GRAVE_0.get())
            .pattern("X")
            .pattern("S")
            .define('S', Items.DIRT)
            .define('X', Items.BOOK)
            .unlockedBy("has_stone", has(Items.STONE))
            .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CGBlocks.DECORATIVE_GRAVE_1.get())
            .pattern("S")
            .pattern("S")
            .define('S', Items.STONE)
            .unlockedBy("has_stone", has(Items.STONE))
            .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CGBlocks.DECORATIVE_GRAVE_2.get())
            .pattern("LSL")
            .pattern("XXX")
            .pattern("SSS")
            .define('S', Items.STONE_BRICKS)
            .define('X', Items.MOSSY_COBBLESTONE)
            .define('L', Items.TORCH)
            .unlockedBy("has_stone_bricks", has(Items.STONE_BRICKS))
            .save(output);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, CGBlocks.DECORATIVE_GRAVE_3.get())
            .pattern(" S ")
            .pattern("SGS")
            .pattern(" S ")
            .define('S', Items.WHITE_CONCRETE)
            .define('G', Items.GOLD_INGOT)
            .unlockedBy("has_gold", has(Items.GOLD_INGOT))
            .save(output);
    }
}