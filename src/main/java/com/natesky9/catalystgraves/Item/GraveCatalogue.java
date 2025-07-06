package com.natesky9.catalystgraves.Item;

import com.natesky9.catalystgraves.Block.SimpleGraveEntity;
import com.natesky9.catalystgraves.GraveLogic;
import com.natesky9.catalystgraves.Menus.CatalogueMenu;
import com.natesky9.catalystgraves.Init.CGBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GraveCatalogue extends Item implements MenuProvider {
    BindingChapter bindingMenu = new BindingChapter();
    public GraveCatalogue(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        System.out.println("use");

        if (player.isUsingItem())
            return InteractionResultHolder.consume(player.getItemInHand(usedHand));

        if (player instanceof ServerPlayer server)
        {
            //if (!(server.containerMenu instanceof BindingMenu))
                server.openMenu(this);
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        System.out.println("useon");
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.PASS;
        if (level.getBlockState(pos).is(Blocks.ENCHANTING_TABLE))
            return bookSucceedTable(player,player.serverLevel(),pos);
        if (level.getBlockState(pos).is(Blocks.ENDER_CHEST))
        {
            Optional<GlobalPos> death = player.getLastDeathLocation();
            if (death.isEmpty())
                return graveFailNoPreviousDeath(player);
            BlockPos deathPos = death.get().pos();
            ServerLevel target = level.getServer().getLevel(death.get().dimension());
            if (target == null)
                return graveFailLevelNull(player);
            if (target.getBlockEntity(deathPos) instanceof SimpleGraveEntity grave)
                graveSucceedRecover(player,target,deathPos,grave);
        }
        if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL))
        {
            Optional<GlobalPos> death = player.getLastDeathLocation();
            if (death.isEmpty())
                return graveFailNoPreviousDeath(player);
            BlockPos deathPos = death.get().pos();
            ServerLevel target = level.getServer().getLevel(death.get().dimension());
            if (target == null)
                return graveFailLevelNull(player);
            if (target.getBlockState(deathPos).is(CGBlocks.SIMPLE_GRAVE))
            {
                player.startUsingItem(context.getHand());
                return graveSucceedTeleport(player,target,deathPos);
            }
            else
            {
                player.sendSystemMessage(Component.translatable("string.grave.fail"));
                return InteractionResult.CONSUME;
            }
        }
        return super.useOn(context);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 20;
    }

    InteractionResult graveFailNoPreviousDeath(Player player)
    {
        //case where the death location doesn't exist
        player.sendSystemMessage(Component.translatable("string.grave.fail.nondeath"));
        if (player.level() instanceof ServerLevel level)
            level.playSound(null,player.blockPosition(),SoundEvents.VILLAGER_NO,SoundSource.PLAYERS);
        return InteractionResult.CONSUME;
    }
    InteractionResult graveFailLevelNull(Player player)
    {
        //case where the level doesn't exist anymore?
        player.sendSystemMessage(Component.translatable("string.grave.fail.levelnull"));
        if (player.level() instanceof ServerLevel level)
            level.playSound(null,player.blockPosition(),SoundEvents.VILLAGER_HURT,SoundSource.PLAYERS);
        return InteractionResult.CONSUME;
    }
    InteractionResult graveSucceedTeleport(Player player,ServerLevel level,BlockPos pos)
    {
        DimensionTransition transition = new DimensionTransition(level, pos.relative(
                player.getNearestViewDirection()).getCenter(),Vec3.ZERO,
                0f,0f, DimensionTransition.DO_NOTHING);
        player.changeDimension(transition);
        if (level instanceof ServerLevel server)
        {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL,pos.getX(),pos.getY(),pos.getZ(),100,1,1,1,1);
            server.playSound(null,pos,SoundEvents.END_PORTAL_SPAWN,SoundSource.BLOCKS,.5f,.5f);
        }
        return InteractionResult.CONSUME;
    }
    InteractionResult graveSucceedRecover(Player player, ServerLevel level, BlockPos deathPos, SimpleGraveEntity grave)
    {
        BlockPos pos = player.blockPosition();
        GraveLogic.RestoreContents(level,player,grave.getItems());
        level.setBlockAndUpdate(deathPos, Blocks.COBBLESTONE_WALL.defaultBlockState());
        if (level instanceof ServerLevel server)
        {
            server.sendParticles(ParticleTypes.REVERSE_PORTAL,pos.getX(),pos.getY(),pos.getZ(),100,1,1,1,1);
            server.playSound(null,pos,SoundEvents.END_PORTAL_FRAME_FILL,SoundSource.BLOCKS);
            server.playSound(null,deathPos,SoundEvents.END_PORTAL_SPAWN,SoundSource.BLOCKS,.5f,.5f);
        }
        return InteractionResult.CONSUME;
    }
    InteractionResult bookSucceedTable(Player player, ServerLevel level, BlockPos pos)
    {
        player.openMenu(bindingMenu);
        level.playSound(null,pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,1,.5f);
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.catalogue");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new CatalogueMenu(i, inventory);
    }
}
