package com.ruben.bblib.api.animatable;

import com.ruben.bblib.api.animatable.data.DataTicket;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AnimationState<T extends BBAnimatable> {

    private final T animatable;
    private final float partialTick;
    private final boolean isMoving;
    private final Map<DataTicket<?>, Object> extraData = new HashMap<>();
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

    public Map<DataTicket<?>, ?> getExtraData() {
        return extraData;
    }

    public <D> D getData(DataTicket<D> dataTicket) {
        return dataTicket.getData(extraData);
    }

    public <D> AnimationState<T> setData(DataTicket<D> dataTicket, D data) {
        extraData.put(dataTicket, data);
        return this;
    }

    public AnimationState<T> putAllData(Map<? extends DataTicket<?>, ?> data) {
        extraData.putAll(data);
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
        return Objects.equals(getController().getCurrentRawAnimation(), animation);
    }

    public void resetCurrentAnimation() {
        getController().forceAnimationReset();
    }

    public void setControllerSpeed(float speed) {
        getController().setAnimationSpeed(speed);
    }
}

