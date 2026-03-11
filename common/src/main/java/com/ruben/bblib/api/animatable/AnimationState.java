package com.ruben.bblib.api.animatable;

public class AnimationState<T extends BBAnimatable> {

    private final T animatable;
    private final float partialTick;
    private final boolean isMoving;
    private AnimationController<T> controller;
    public double animationTick;

    public AnimationState(T animatable, float partialTick, boolean isMoving) {
        this.animatable = animatable;
        this.partialTick = partialTick;
        this.isMoving = isMoving;
    }

    public double getAnimationTick() {
        return animationTick;
    }

    public T getAnimatable() {
        return animatable;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public AnimationController<T> getController() {
        return controller;
    }

    public AnimationState<T> withController(AnimationController<T> controller) {
        this.controller = controller;
        return this;
    }

    public void setAnimation(RawAnimation animation) {
        getController().setAnimation(animation);
    }

    public PlayState setAndContinue(RawAnimation animation) {
        getController().setAnimation(animation);
        return PlayState.CONTINUE;
    }

    public boolean isCurrentAnimation(RawAnimation animation) {
        return java.util.Objects.equals(getController().getCurrentRawAnimation(), animation);
    }

    public void resetCurrentAnimation() {
        getController().forceAnimationReset();
    }
}

