package com.natesky9.catalystgraves.client;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("null")
public class ColorUtils
{

    // Patrón que detecta #RRGGBB o #r
    private static final Pattern HEX_PATTERN = Pattern.compile("#([0-9a-fA-F]{6}|r)");

    /**
     * Obtiene la traducción de una clave y procesa las marcas de color HEX (#RRGGBB) y reset (#r).
     */
    public static Component parseTranslatable(String key)
    {
        String rawText = Language.getInstance().getOrDefault(key);
        return parseHexText(rawText);
    }

    public static Component parseHexText(String text)
    {
        MutableComponent result = Component.empty();
        Matcher matcher = HEX_PATTERN.matcher(text);

        int lastIdx = 0;
        TextColor currentColor = null;

        while(matcher.find())
        {
            // Añadir el segmento de texto anterior al match actual
            if(matcher.start() > lastIdx)
            {
                String segment = text.substring(lastIdx, matcher.start());
                MutableComponent part = Component.literal(segment);
                if(currentColor != null)
                {
                    part.setStyle(part.getStyle().withColor(currentColor));
                }
                result.append(part);
            }

            String code = matcher.group(1);
            if("r".equalsIgnoreCase(code))
            {
                currentColor = null; // Resetear color
            }
            else
            {
                int rgb = Integer.parseInt(code, 16);
                currentColor = TextColor.fromRgb(rgb);
            }

            lastIdx = matcher.end();
        }

        // Añadir el texto restante si lo hay
        if(lastIdx < text.length())
        {
            String segment = text.substring(lastIdx);
            MutableComponent part = Component.literal(segment);
            if(currentColor != null)
            {
                part.setStyle(part.getStyle().withColor(currentColor));
            }
            result.append(part);
        }

        return result;
    }
}