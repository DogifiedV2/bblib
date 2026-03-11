package com.ruben.bblib.example.client;

import com.ruben.bblib.api.renderer.BBEntityRenderer;
import com.ruben.bblib.example.RootsVfxEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class RootsVfxEntityRenderer extends BBEntityRenderer<RootsVfxEntity> {

    public RootsVfxEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new RootsVfxEntityModel());
        this.shadowRadius = 0.0f;
    }
}
