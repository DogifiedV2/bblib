package com.ruben.bblib.api.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ruben.bblib.api.animatable.AnimatableManager;
import com.ruben.bblib.api.animatable.AnimationState;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.api.model.animation.BoneRenderState;
import com.ruben.bblib.api.model.animation.BoneRenderStateMap;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.molang.MolangContext;
import com.ruben.bblib.api.renderer.BBEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class BBRenderContext<T extends Entity & BBAnimatable> {

    private final BBEntityRenderer<T> renderer;
    private final BBModel<T> model;
    private final T entity;
    private final ModelData modelData;
    private final PoseStack poseStack;
    private final MultiBufferSource bufferSource;
    private final int packedLight;
    private final int packedOverlay;
    private final AnimatableManager<T> manager;
    private final AnimationState<T> animationState;
    private final BoneRenderStateMap boneRenderStates;
    private final MolangContext molangContext;
    private final float partialTick;
    private final float animationTime;
    private final double currentTick;

    public BBRenderContext(BBEntityRenderer<T> renderer, BBModel<T> model, T entity, ModelData modelData,
                           PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                           AnimatableManager<T> manager, AnimationState<T> animationState,
                           BoneRenderStateMap boneRenderStates, MolangContext molangContext,
                           float partialTick, float animationTime, double currentTick) {
        this.renderer = renderer;
        this.model = model;
        this.entity = entity;
        this.modelData = modelData;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
        this.manager = manager;
        this.animationState = animationState;
        this.boneRenderStates = boneRenderStates;
        this.molangContext = molangContext;
        this.partialTick = partialTick;
        this.animationTime = animationTime;
        this.currentTick = currentTick;
    }

    public BBEntityRenderer<T> getRenderer() {
        return renderer;
    }

    public BBModel<T> getModel() {
        return model;
    }

    public T getEntity() {
        return entity;
    }

    public ModelData getModelData() {
        return modelData;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getBufferSource() {
        return bufferSource;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public int getPackedOverlay() {
        return packedOverlay;
    }

    public AnimatableManager<T> getManager() {
        return manager;
    }

    public AnimationState<T> getAnimationState() {
        return animationState;
    }

    public BoneRenderStateMap getBoneRenderStates() {
        return boneRenderStates;
    }

    @Nullable
    public BoneRenderState getBoneRenderState(String boneName) {
        return boneRenderStates.getBone(boneName);
    }

    public MolangContext getMolangContext() {
        return molangContext;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public float getAnimationTime() {
        return animationTime;
    }

    public double getCurrentTick() {
        return currentTick;
    }
}
