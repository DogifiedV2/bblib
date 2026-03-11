package com.ruben.bblib.api.animatable;

import net.minecraft.world.entity.Entity;

public interface BBEntityAnimatable extends BBAnimatable {

    @Override
    default double getTick(Object object) {
        return ((Entity) this).tickCount;
    }

    @SuppressWarnings("unchecked")
    default AnimatableManager<? extends BBEntityAnimatable> getAnimatableManager() {
        Entity entity = (Entity) this;
        return (AnimatableManager<? extends BBEntityAnimatable>) (AnimatableManager<?>)
                getAnimatableInstanceCache().getManagerForId(entity.getId());
    }

    default boolean triggerAnimation(String animationName) {
        return getAnimatableManager().triggerAnimation(animationName);
    }

    default boolean triggerAnimation(String controllerName, String animationName) {
        return getAnimatableManager().triggerAnimation(controllerName, animationName);
    }

    default boolean stopTriggeredAnimation() {
        return getAnimatableManager().stopTriggeredAnimation();
    }

    default boolean stopTriggeredAnimation(String controllerName, String animationName) {
        return getAnimatableManager().stopTriggeredAnimation(controllerName, animationName);
    }

    default boolean resetTriggeredAnimation(String controllerName, String animationName) {
        return getAnimatableManager().resetTriggeredAnimation(controllerName, animationName);
    }
}
