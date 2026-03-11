package com.ruben.bblib.api.model;

import com.ruben.bblib.api.BBLibApi;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.TextureData;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public abstract class BBModel<T extends BBAnimatable> {

    public abstract ResourceLocation getModelResource(T entity);

    @Nullable
    public ResourceLocation getTextureResource(T entity) {
        return null;
    }

    @Nullable
    public ResourceLocation getTextureResource(T entity, int textureIndex, TextureData textureData) {
        return textureIndex == 0 ? getTextureResource(entity) : null;
    }

    @Nullable
    public ModelData getModelData(T entity) {
        return BBLibApi.getModel(getModelResource(entity));
    }

    public ResourceLocation getDefaultTexture(T entity) {
        return BBLibApi.getDefaultTexture(getModelResource(entity));
    }
}

