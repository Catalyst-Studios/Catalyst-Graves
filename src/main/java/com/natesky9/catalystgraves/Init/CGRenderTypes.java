package com.natesky9.catalystgraves.Init;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public class CGRenderTypes {
    public static final RenderType SPECTRAL =  RenderType.create("test",
    DefaultVertexFormat.POSITION_COLOR_NORMAL,
    VertexFormat.Mode.LINES,
            1536,
            RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(4)))
            .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false));
}
