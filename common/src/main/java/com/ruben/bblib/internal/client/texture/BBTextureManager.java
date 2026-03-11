package com.ruben.bblib.internal.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.api.model.data.TextureData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class BBTextureManager {

    private static final BBTextureManager INSTANCE = new BBTextureManager();

    private final Map<String, ResourceLocation> textureLocations = new ConcurrentHashMap<>();

    private BBTextureManager() {
    }

    public static BBTextureManager getInstance() {
        return INSTANCE;
    }

    public ResourceLocation getOrCreateTexture(ResourceLocation modelId, int textureIndex, TextureData textureData) {
        String key = buildKey(modelId, textureIndex);
        return textureLocations.computeIfAbsent(key, k -> registerTexture(k, textureData));
    }

    public static ResourceLocation getDefaultTexture(String namespace, String modelPath) {
        ResourceLocation modelId = ResourceLocation.fromNamespaceAndPath(namespace, modelPath);
        return getDefaultTexture(modelId);
    }

    public static ResourceLocation getDefaultTexture(ResourceLocation modelId) {
        return ResourceLocation.fromNamespaceAndPath(BBLibCommon.MOD_ID, "dynamic/" + flattenKey(modelId) + "_0");
    }

    public void clearTextures() {
        for (ResourceLocation location : textureLocations.values()) {
            Minecraft.getInstance().getTextureManager().release(location);
        }
        textureLocations.clear();
    }

    private ResourceLocation registerTexture(String key, TextureData textureData) {
        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(textureData.imageData()));
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(BBLibCommon.MOD_ID, "dynamic/" + key);
            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
            return location;
        } catch (Exception e) {
            BBLibCommon.LOGGER.error("Failed to create texture for {}", key, e);
            return MissingModelTexture.getLocation();
        }
    }

    private static String buildKey(ResourceLocation modelId, int textureIndex) {
        return flattenKey(modelId) + "_" + textureIndex;
    }

    private static String flattenKey(ResourceLocation modelId) {
        return modelId.getNamespace() + "_" + modelId.getPath().replace('/', '_');
    }
}

