package com.ruben.bblib.api.animation.keyframe.event;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animation.keyframe.event.data.SoundKeyframeData;

public final class SoundKeyframeEvent<T extends BBAnimatable> extends KeyframeEvent<T, SoundKeyframeData> {

    public SoundKeyframeEvent(T animatable, double animationTime, AnimationController<T> controller,
                              SoundKeyframeData keyframeData) {
        super(animatable, animationTime, controller, keyframeData);
    }
}
