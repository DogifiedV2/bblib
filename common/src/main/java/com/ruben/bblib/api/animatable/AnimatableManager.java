package com.ruben.bblib.api.animatable;

import com.ruben.bblib.api.animatable.data.DataTicket;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AnimatableManager<T extends BBAnimatable> {

    private final Map<String, AnimationController<T>> animationControllers;
    private final Map<DataTicket<?>, Object> extraData = new HashMap<>();
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

    @Nullable
    public AnimationController<T> getAnimationController(String name) {
        return animationControllers.get(name);
    }

    public <D> void setData(DataTicket<D> dataTicket, D data) {
        extraData.put(dataTicket, data);
    }

    public <D> D getData(DataTicket<D> dataTicket) {
        return dataTicket.getData(extraData);
    }

    public void applyDataToState(AnimationState<T> state) {
        state.putAllData(extraData);
    }

    public boolean triggerAnimation(String animationName) {
        for (AnimationController<T> controller : animationControllers.values()) {
            if (controller.triggerAnimation(animationName)) {
                return true;
            }
        }
        return false;
    }

    public boolean triggerAnimation(String controllerName, String animationName) {
        AnimationController<T> controller = animationControllers.get(controllerName);
        return controller != null && controller.triggerAnimation(animationName);
    }

    public boolean stopTriggeredAnimation() {
        for (AnimationController<T> controller : animationControllers.values()) {
            if (controller.stopTriggeredAnimation()) {
                return true;
            }
        }
        return false;
    }

    public boolean stopTriggeredAnimation(String animationName) {
        for (AnimationController<T> controller : animationControllers.values()) {
            if (controller.stopTriggeredAnimation(animationName)) {
                return true;
            }
        }
        return false;
    }

    public boolean stopTriggeredAnimation(String controllerName, @Nullable String animationName) {
        AnimationController<T> controller = animationControllers.get(controllerName);
        if (controller == null) {
            return false;
        }
        return animationName != null ? controller.stopTriggeredAnimation(animationName) : controller.stopTriggeredAnimation();
    }

    public boolean resetTriggeredAnimation() {
        for (AnimationController<T> controller : animationControllers.values()) {
            if (controller.resetTriggeredAnimation()) {
                return true;
            }
        }
        return false;
    }

    public boolean resetTriggeredAnimation(String animationName) {
        for (AnimationController<T> controller : animationControllers.values()) {
            if (controller.resetTriggeredAnimation(animationName)) {
                return true;
            }
        }
        return false;
    }

    public boolean resetTriggeredAnimation(String controllerName, @Nullable String animationName) {
        AnimationController<T> controller = animationControllers.get(controllerName);
        if (controller == null) {
            return false;
        }
        return animationName != null ? controller.resetTriggeredAnimation(animationName) : controller.resetTriggeredAnimation();
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
