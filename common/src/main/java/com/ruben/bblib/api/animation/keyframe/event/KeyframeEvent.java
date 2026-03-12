package com.ruben.bblib.api.animation.keyframe.event;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.AnimationState;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animatable.data.DataTickets;
import com.ruben.bblib.api.animatable.data.DataTicket;
import com.ruben.bblib.api.animation.keyframe.event.data.KeyframeData;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.transform.ResolvedNodeTransform;
import com.ruben.bblib.api.molang.MolangContext;
import com.ruben.bblib.internal.model.transform.BBModelTransformResolver;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public abstract class KeyframeEvent<T extends BBAnimatable, D extends KeyframeData> {

    private final T animatable;
    private final double animationTime;
    private final AnimationController<T> controller;
    private final D keyframeData;
    private final AnimationState<T> animationState;

    protected KeyframeEvent(T animatable, double animationTime, AnimationController<T> controller,
                            D keyframeData, AnimationState<T> animationState) {
        this.animatable = animatable;
        this.animationTime = animationTime;
        this.controller = controller;
        this.keyframeData = keyframeData;
        this.animationState = animationState;
    }

    public T getAnimatable() {
        return animatable;
    }

    public double getAnimationTime() {
        return animationTime;
    }

    public AnimationController<T> getController() {
        return controller;
    }

    public D getKeyframeData() {
        return keyframeData;
    }

    public AnimationState<T> getAnimationState() {
        return animationState;
    }

    public <V> V getData(DataTicket<V> dataTicket) {
        return animationState.getData(dataTicket);
    }

    @Nullable
    public ResolvedNodeTransform getLocatorTransform(String locatorName) {
        if (!(animatable instanceof Entity entity)) {
            return null;
        }

        ModelData modelData = getData(DataTickets.MODEL_DATA);
        MolangContext molangContext = getData(DataTickets.MOLANG_CONTEXT);
        Double currentTick = getData(DataTickets.TICK);
        Float partialTick = getData(DataTickets.PARTIAL_TICK);

        if (modelData == null || molangContext == null || currentTick == null || partialTick == null) {
            return null;
        }

        return BBModelTransformResolver.resolveLocatorTransform(
                entity, modelData, locatorName, controller, molangContext, currentTick, partialTick
        );
    }

    @Nullable
    public ResolvedNodeTransform getBoneTransform(String boneName) {
        if (!(animatable instanceof Entity entity)) {
            return null;
        }

        ModelData modelData = getData(DataTickets.MODEL_DATA);
        MolangContext molangContext = getData(DataTickets.MOLANG_CONTEXT);
        Double currentTick = getData(DataTickets.TICK);
        Float partialTick = getData(DataTickets.PARTIAL_TICK);

        if (modelData == null || molangContext == null || currentTick == null || partialTick == null) {
            return null;
        }

        return BBModelTransformResolver.resolveBoneTransform(
                entity, modelData, boneName, controller, molangContext, currentTick, partialTick
        );
    }
}
