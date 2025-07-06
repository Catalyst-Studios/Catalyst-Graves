package com.natesky9.catalystgraves.Menus;

import com.natesky9.catalystgraves.CatalystGraves;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record CataloguePacket(String name, int cost) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CataloguePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID,"catalogue_selection"));
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static final StreamCodec<ByteBuf,CataloguePacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8,CataloguePacket::name,
                    ByteBufCodecs.INT,CataloguePacket::cost,
                    CataloguePacket::new);
    //
    public static class ClientPayloadHandler {
        public static void handleData(final CataloguePacket packet, final IPayloadContext context)
        {
            System.out.println("client payload");
            //we shouldn't need this, since the catalogue only sends client -> server
        }
    }
    //
    public static class ServerPayloadHandler {
        public static void handleData(final CataloguePacket packet, final IPayloadContext context)
        {
            System.out.println(packet.name);
            ResourceLocation name = ResourceLocation.parse(packet.name);
            ServerPlayer player = (ServerPlayer) context.player();
            PlayerAdvancements advancements = player.getAdvancements();

            ServerAdvancementManager manager = player.getServer().getAdvancements();
            AdvancementHolder holder = manager.get(name);
            if (holder == null)
            {
                System.out.println("Something wrong with the resource location: " + name);
                return;
            }
            Advancement advancement = manager.tree().get(holder).advancement();
            //
            int cost = packet.cost;
            int playerLevel = player.experienceLevel;
            if (cost > playerLevel)
            {
                System.out.println("Cost is higher than player level! "
                + "How did you manage that?");
                return;
            }
            player.giveExperienceLevels(-cost);
            //
            for (List<String> requirements:advancement.requirements().requirements())
            {
                for (String string:requirements)
                {
                    advancements.award(holder,string);
                }
            }
        }
    }
}
