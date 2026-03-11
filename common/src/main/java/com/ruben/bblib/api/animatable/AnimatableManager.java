package com.ruben.bblib.api.animatable;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnimatableManager<T extends BBAnimatable> {

    private final Map<String, AnimationController<T>> animationControllers;
    private double lastUpdateTime;
    private boolean isFirstTick = true;
    private double firstTickTime = -1;

    @SuppressWarnings("unchecked")
    public AnimatableManager(BBAnimatable animatable) {
        ControllerRegistrar registrar = new ControllerRegistrar();
        animatable.registerControllers(registrar);

        this.animationControllers = new LinkedHashMap<>();
        for (AnimationController<? extends BBAnimatable> controller : registrar.getControllers()) {
            animationControllers.put(controller.getName(), (AnimationController<T>) controller);
        }
    }

    public Map<String, AnimationController<T>> getAnimationControllers() {
        return animationControllers;
    }

    public double getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void updatedAt(double updateTime) {
        this.lastUpdateTime = updateTime;
    }

    public double getFirstTickTime() {
        return firstTickTime;
    }

    public void startedAt(double time) {
        this.firstTickTime = time;
    }

    public boolean isFirstTick() {
        return isFirstTick;
    }

    public void finishFirstTick() {
        this.isFirstTick = false;
    }
}
