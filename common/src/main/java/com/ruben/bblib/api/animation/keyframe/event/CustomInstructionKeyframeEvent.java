package com.ruben.bblib.api.animation.keyframe.event;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animation.keyframe.event.data.CustomInstructionKeyframeData;

public final class CustomInstructionKeyframeEvent<T extends BBAnimatable>
        extends KeyframeEvent<T, CustomInstructionKeyframeData> {

    public CustomInstructionKeyframeEvent(T animatable, double animationTime, AnimationController<T> controller,
                                          CustomInstructionKeyframeData keyframeData) {
        super(animatable, animationTime, controller, keyframeData);
    }
}
