package com.ruben.bblib.internal.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.ruben.bblib.internal.BBLibCommon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public final class MissingModelTexture {

    private static final ResourceLocation LOCATION = ResourceLocation.fromNamespaceAndPath(BBLibCommon.MOD_ID, "missing_model");
    private static final int SIZE = 16;
    private static final int CELL_SIZE = 4;
    private static final int PINK = 0xFFFF00FF;
    private static final int BLACK = 0xFF000000;

    private static boolean registered = false;

    private MissingModelTexture() {
    }

    public static ResourceLocation getLocation() {
        if (!registered) {
            register();
        }
        return LOCATION;
    }

    private static synchronized void register() {
        if (registered) {
            return;
        }

        NativeImage image = new NativeImage(SIZE, SIZE, false);

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                boolean pinkCell = ((x / CELL_SIZE) + (y / CELL_SIZE)) % 2 == 0;
                image.setPixelRGBA(x, y, pinkCell ? PINK : BLACK);
            }
        }

        DynamicTexture texture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(LOCATION, texture);
        registered = true;
    }
}

