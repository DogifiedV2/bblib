package com.ruben.bblib.api.animatable;

import com.ruben.bblib.api.animation.BBAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RawAnimation {

    private final List<Stage> animationStages;

    private RawAnimation() {
        this.animationStages = new ArrayList<>();
    }

    public static RawAnimation begin() {
        return new RawAnimation();
    }

    public RawAnimation thenPlay(String animationName) {
        return then(animationName, BBAnimation.LoopMode.ONCE);
    }

    public RawAnimation thenLoop(String animationName) {
        return then(animationName, BBAnimation.LoopMode.LOOP);
    }

    public RawAnimation thenPlayAndHold(String animationName) {
        return then(animationName, BBAnimation.LoopMode.HOLD);
    }

    public RawAnimation then(String animationName, BBAnimation.LoopMode loopMode) {
        animationStages.add(new Stage(animationName, loopMode));
        return this;
    }

    public List<Stage> getAnimationStages() {
        return animationStages;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return hashCode() == obj.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(animationStages);
    }

    public record Stage(String animationName, BBAnimation.LoopMode loopMode) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            return hashCode() == obj.hashCode();
        }

        @Override
        public int hashCode() {
            return Objects.hash(animationName, loopMode);
        }
    }
}

