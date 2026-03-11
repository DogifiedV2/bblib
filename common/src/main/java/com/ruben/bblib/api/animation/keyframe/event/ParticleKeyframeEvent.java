package com.ruben.bblib.api.animation.keyframe.event;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animation.keyframe.event.data.ParticleKeyframeData;

public final class ParticleKeyframeEvent<T extends BBAnimatable> extends KeyframeEvent<T, ParticleKeyframeData> {

    public ParticleKeyframeEvent(T animatable, double animationTime, AnimationController<T> controller,
                                 ParticleKeyframeData keyframeData) {
        super(animatable, animationTime, controller, keyframeData);
    }
}
