package com.ruben.bblib.api.model;

import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.internal.cache.BBModelCache;
import com.ruben.bblib.internal.client.texture.BBTextureManager;
import com.ruben.bblib.api.model.ModelData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public abstract class BBModel<T extends BBAnimatable> {

    public abstract ResourceLocation getModelResource(T entity);

    public abstract ResourceLocation getTextureResource(T entity);

    @Nullable
    public ModelData getModelData(T entity) {
        return BBModelCache.getModel(getModelResource(entity));
    }

    public ResourceLocation getDefaultTexture(T entity) {
        return BBTextureManager.getDefaultTexture(getModelResource(entity));
    }
}

