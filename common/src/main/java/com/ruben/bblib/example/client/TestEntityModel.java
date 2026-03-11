package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.example.TestEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class TestEntityModel extends BBModel<TestEntity> {

    @Override
    public ResourceLocation getModelResource(TestEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("bblib", "test_entity");
    }
}

