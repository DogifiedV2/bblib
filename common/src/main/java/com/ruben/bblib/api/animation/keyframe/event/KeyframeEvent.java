package com.ruben.bblib.api.animation.keyframe.event;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animation.keyframe.event.data.KeyframeData;

public abstract class KeyframeEvent<T extends BBAnimatable, D extends KeyframeData> {

    private final T animatable;
    private final double animationTime;
    private final AnimationController<T> controller;
    private final D keyframeData;

    protected KeyframeEvent(T animatable, double animationTime, AnimationController<T> controller, D keyframeData) {
        this.animatable = animatable;
        this.animationTime = animationTime;
        this.controller = controller;
        this.keyframeData = keyframeData;
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
}
