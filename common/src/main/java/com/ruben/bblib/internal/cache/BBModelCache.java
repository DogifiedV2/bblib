package com.ruben.bblib.internal.cache;

import com.ruben.bblib.api.model.data.ModelData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public final class BBModelCache {

    private static Map<ResourceLocation, ModelData> MODELS = Collections.emptyMap();

    private BBModelCache() {
    }

    public static Map<ResourceLocation, ModelData> getBakedModels() {
        return MODELS;
    }

    @Nullable
    public static ModelData getModel(ResourceLocation location) {
        return MODELS.get(location);
    }

    public static boolean hasModel(ResourceLocation location) {
        return MODELS.containsKey(location);
    }

    static void setModels(Map<ResourceLocation, ModelData> models) {
        MODELS = models;
    }
}

