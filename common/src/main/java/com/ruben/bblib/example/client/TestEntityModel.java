package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.api.animatable.AnimationState;
import com.ruben.bblib.api.animatable.data.DataTickets;
import com.ruben.bblib.api.animatable.data.EntityRenderData;
import com.ruben.bblib.api.model.animation.BoneRenderState;
import com.ruben.bblib.api.model.animation.BoneRenderStateMap;
import com.ruben.bblib.api.molang.MolangContext;
import com.ruben.bblib.example.TestEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class TestEntityModel extends BBModel<TestEntity> {

    @Override
    public ResourceLocation getModelResource(TestEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("bblib", "test_entity");
    }

    @Override
    public void applyMolangQueries(TestEntity entity, AnimationState<TestEntity> animationState, MolangContext molangContext) {
        EntityRenderData renderData = animationState.getData(DataTickets.ENTITY_RENDER_DATA);
        if (renderData == null) {
            return;
        }

        molangContext.withQuery("q.head_yaw", Mth.wrapDegrees(renderData.headYaw() - renderData.bodyYaw()));
        molangContext.withQuery("q.head_pitch", renderData.headPitch());
    }

    @Override
    public void setCustomAnimations(TestEntity entity, AnimationState<TestEntity> animationState,
                                    BoneRenderStateMap boneRenderStates) {
        BoneRenderState head = boneRenderStates.getBone("head");
        EntityRenderData renderData = animationState.getData(DataTickets.ENTITY_RENDER_DATA);
        if (head == null || renderData == null) {
            return;
        }

        float yawOffset = Mth.clamp(Mth.wrapDegrees(renderData.headYaw() - renderData.bodyYaw()), -35.0f, 35.0f);
        float pitchOffset = Mth.clamp(renderData.headPitch(), -25.0f, 25.0f);
        head.addRotation(pitchOffset * 0.35f, -yawOffset * 0.5f, 0.0f);
    }
}

