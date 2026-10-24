package com.natesky9.catalystgraves.client.menus;

import com.natesky9.catalystgraves.CatalystGraves;
import com.natesky9.catalystgraves.init.CGConfig;
import com.natesky9.catalystgraves.init.CGEnchantments;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@SuppressWarnings("null")
public record BindingPacket(int slot) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<BindingPacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "binding_selection"));

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, BindingPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.INT, BindingPacket::slot,
                              BindingPacket::new);

    public static class ClientPayloadHandler
    {
        public static void handleData(final BindingPacket packet, final IPayloadContext context)
        {
        }
    }

    public static class ServerPayloadHandler
    {
        public static void handleData(final BindingPacket packet, final IPayloadContext context)
        {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel serverLevel = player.serverLevel();
            int slot = packet.slot;
            ItemStack stack = player.getInventory().getItem(slot);

            int cost = CGConfig.SOULBOUND_COST.get();
            if (player.experienceLevel < cost)
            {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS);
                return;
            }

            if (stack.isEmpty() || com.natesky9.catalystgraves.server.GraveLogic.isBlacklistedFromSoulbound(stack))
            {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS);
                return;
            }

            Registry<Enchantment> enchantmentRegistry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            Holder<Enchantment> enchantmentHolder = enchantmentRegistry.getHolderOrThrow(CGEnchantments.SOULBOUND);
            if (stack.getEnchantmentLevel(enchantmentHolder) > 0)
            {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS);
                return;
            }

            player.giveExperienceLevels(-cost);
            stack.enchant(enchantmentHolder, 1);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS);
            serverLevel.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY(), player.getZ(), 40, 1, 1, 1, .2f);
        }
    }
}