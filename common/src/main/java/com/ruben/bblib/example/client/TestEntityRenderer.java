package com.ruben.bblib.example.client;

import com.ruben.bblib.api.renderer.BBEntityRenderer;
import com.ruben.bblib.example.TestEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class TestEntityRenderer extends BBEntityRenderer<TestEntity> {

    public TestEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new TestEntityModel());
        addRenderLayer(new TestEntityHeadLayer(this));
    }
}

