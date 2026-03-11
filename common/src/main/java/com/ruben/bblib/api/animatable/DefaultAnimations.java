package com.ruben.bblib.api.animatable;

public final class DefaultAnimations {

    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");

    private DefaultAnimations() {
    }

    public static <T extends BBAnimatable> AnimationController<T> genericWalkIdleController(T animatable) {
        return genericWalkIdleController(animatable, "main", 0, WALK, IDLE);
    }

    public static <T extends BBAnimatable> AnimationController<T> genericWalkIdleController(
            T animatable, String controllerName, int transitionTicks, RawAnimation walkAnimation, RawAnimation idleAnimation) {
        return new AnimationController<>(animatable, controllerName, transitionTicks,
                state -> state.setAndContinue(state.isMoving() ? walkAnimation : idleAnimation));
    }
}
