package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.example.PumpkinBossTestEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class PumpkinBossTestEntityModel extends BBModel<PumpkinBossTestEntity> {

    @Override
    public ResourceLocation getModelResource(PumpkinBossTestEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("bblib", "pumpkin_boss");
    }
}

