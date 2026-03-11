package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.example.RootsVfxEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class RootsVfxEntityModel extends BBModel<RootsVfxEntity> {

    @Override
    public ResourceLocation getModelResource(RootsVfxEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("bblib", "roots_vfx");
    }

    @Override
    public ResourceLocation getTextureResource(RootsVfxEntity entity) {
        return getDefaultTexture(entity);
    }
}
