package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.example.PumpkinSeedProjectileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class PumpkinSeedProjectileModel extends BBModel<PumpkinSeedProjectileEntity> {

    @Override
    public ResourceLocation getModelResource(PumpkinSeedProjectileEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("bblib", "pumpkin_seed");
    }

    @Override
    public ResourceLocation getTextureResource(PumpkinSeedProjectileEntity entity) {
        return getDefaultTexture(entity);
    }
}
