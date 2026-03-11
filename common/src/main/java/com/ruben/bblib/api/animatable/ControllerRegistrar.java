package com.ruben.bblib.api.animatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControllerRegistrar {

    private final List<AnimationController<? extends BBAnimatable>> controllers = new ArrayList<>();

    public ControllerRegistrar add(AnimationController<?>... controllers) {
        this.controllers.addAll(Arrays.asList(controllers));
        return this;
    }

    public ControllerRegistrar add(AnimationController<?> controller) {
        controllers.add(controller);
        return this;
    }

    public List<AnimationController<? extends BBAnimatable>> getControllers() {
        return controllers;
    }
}

