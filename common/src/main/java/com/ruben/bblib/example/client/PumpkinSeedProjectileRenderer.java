package com.ruben.bblib.example.client;

import com.ruben.bblib.api.renderer.BBEntityRenderer;
import com.ruben.bblib.example.PumpkinSeedProjectileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class PumpkinSeedProjectileRenderer extends BBEntityRenderer<PumpkinSeedProjectileEntity> {

    public PumpkinSeedProjectileRenderer(EntityRendererProvider.Context context) {
        super(context, new PumpkinSeedProjectileModel());
        this.shadowRadius = 0.0f;
    }
}
