package com.natesky9.catalystgraves.client.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

import java.util.List;

//Had to do the long java class name
public class CatalogueClientTooltipComponent implements ClientTooltipComponent
{

    //private static final ResourceLocation BINDING_BG = ResourceLocation.fromNamespaceAndPath(CatalystGraves.MODID, "textures/gui/binding.png");

    //Animation, we do old school
    private static final ResourceLocation[] SCULK_SOUL_FRAMES = new ResourceLocation[11];
    static
    {
        for(int i = 0; i < 11; i++)
        {
            SCULK_SOUL_FRAMES[i] = ResourceLocation.withDefaultNamespace("textures/particle/sculk_soul_" + i + ".png");
        }
    }

    private static final int ICON_SIZE = 16;
    private static final int MAX_TEXT_WIDTH = 200;
    private static final int PADDING = 10;

    private final List<FormattedCharSequence> lines;
    private final int textWidth;
    private final int textHeight;

    @SuppressWarnings("null")
    public CatalogueClientTooltipComponent(CatalogueTooltipData data)
    {
        Font font = Minecraft.getInstance().font;
        this.lines = font.split(data.text(), MAX_TEXT_WIDTH);

        int maxWidth = 0;
        for(FormattedCharSequence line : lines)
        {
            int w = font.width(line);
            if(w > maxWidth) maxWidth = w;
        }
        this.textWidth = maxWidth;
        this.textHeight = lines.size() * font.lineHeight;
    }

    @Override
    public int getHeight()
    {
        return textHeight + (PADDING * 2);
    }

    @Override
    @SuppressWarnings("null") 
    public int getWidth(Font font)
    {
        return textWidth + (PADDING * 2);
    }

    @SuppressWarnings("null")
    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics)
    {
        int w = getWidth(font);
        int h = getHeight();


        // guiGraphics.blit(BINDING_BG, x, y, 0, 0, w, h, 256, 256);

        Minecraft mc = Minecraft.getInstance();
        long time = mc.level != null ? mc.level.getGameTime() : 0;

        int particlesPerSide = 4; // Cuántas almas quieres ver a la vez por lado
        int travelDistance = h + ICON_SIZE; // Distancia desde que nacen (abajo) hasta que mueren (arriba)
        int speed = 2; // Velocidad de subida (píxeles por tick)

        for(int i = 0; i < particlesPerSide; i++)
        {
            // Desfasamos cada partícula en el tiempo para que no salgan todas a la vez
            long offsetTime = time + (i * 25L);

            // Calculamos la posición Y. Usamos el módulo (%) para que vuelvan abajo al llegar arriba.
            int yOffset = (int)((offsetTime * speed) % travelDistance);
            int drawY = y + h - yOffset; // Empiezan en la base (y + h) y restamos el avance

            // Calculamos en qué frame de animación está ESTA partícula concreta
            int currentFrame = (int)((offsetTime / 2) % SCULK_SOUL_FRAMES.length);
            ResourceLocation frameTex = SCULK_SOUL_FRAMES[currentFrame];

            // Posiciones X (ligeramente fuera del marco para que lo envuelvan)
            int leftX = x - 8;
            int rightX = x + w - 8;

            // Dibujamos la partícula izquierda
            guiGraphics.blit(frameTex, leftX, drawY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            // Dibujamos la partícula derecha
            guiGraphics.blit(frameTex, rightX, drawY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    @SuppressWarnings("null")
    @Override
    public void renderText(Font font, int x, int y, Matrix4f pose, MultiBufferSource.BufferSource bufferSource)
    {
        int textX = x + PADDING;
        int textY = y + PADDING;

        for(int i = 0; i < lines.size(); i++)
        {
            // Un color clarito azul/sculk para el texto
            font.drawInBatch(lines.get(i), (float)textX, (float)(textY + i * font.lineHeight), 0xFFB3E5FC, true, pose, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
        }
    }
}