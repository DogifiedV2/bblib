package com.ruben.bblib.api;

import com.ruben.bblib.api.animatable.AnimatableInstanceCache;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.TextureData;
import com.ruben.bblib.api.util.HitboxCalculator;
import com.ruben.bblib.internal.cache.BBModelCache;
import com.ruben.bblib.internal.client.texture.BBTextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BBLibApi {

    private BBLibApi() {
    }

    public static AnimatableInstanceCache createCache(BBAnimatable animatable) {
        return new AnimatableInstanceCache(animatable);
    }

    public static HitboxCalculator.HitboxResult calculateHitbox(ModelData modelData) {
        return HitboxCalculator.calculateFromModel(modelData);
    }

    public static HitboxCalculator.HitboxResult calculateHitbox(ResourceLocation modelId) {
        return HitboxCalculator.calculateFromModel(getModel(modelId));
    }

    @Nullable
    public static ModelData getModel(ResourceLocation modelId) {
        return BBModelCache.getModel(modelId);
    }

    public static boolean hasModel(ResourceLocation modelId) {
        return BBModelCache.hasModel(modelId);
    }

    public static List<String> getAnimationNames(ResourceLocation modelId) {
        ModelData modelData = getModel(modelId);
        return modelData != null ? modelData.getAnimationNames() : List.of();
    }

    public static int getLoadedModelCount() {
        return BBModelCache.getBakedModels().size();
    }

    public static ResourceLocation getDefaultTexture(ResourceLocation modelId) {
        return BBTextureManager.getDefaultTexture(modelId);
    }

    public static ResourceLocation createEmbeddedTexture(ResourceLocation modelId, int textureIndex, TextureData textureData) {
        return BBTextureManager.getInstance().getOrCreateTexture(modelId, textureIndex, textureData);
    }
}
