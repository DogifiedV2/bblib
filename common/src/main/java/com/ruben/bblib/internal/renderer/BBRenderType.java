package com.ruben.bblib.internal.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public final class BBRenderType {

    private BBRenderType() {
    }

    public static RenderType entityCutoutNoCull(ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }

    public static RenderType entityTranslucentEmissive(ResourceLocation texture) {
        return RenderType.eyes(texture);
    }
}

