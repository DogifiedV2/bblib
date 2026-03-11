package com.ruben.bblib.example.client;

import com.ruben.bblib.api.renderer.BBEntityRenderer;
import com.ruben.bblib.example.PumpkinBossTestEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class PumpkinBossTestEntityRenderer extends BBEntityRenderer<PumpkinBossTestEntity> {

    public PumpkinBossTestEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PumpkinBossTestEntityModel());
        this.shadowRadius = 1.2f;
    }
}

