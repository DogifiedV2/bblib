package com.ruben.bblib.api.animatable;

public interface BBAnimatable {

    void registerControllers(ControllerRegistrar controllers);

    AnimatableInstanceCache getAnimatableInstanceCache();

    double getTick(Object object);

    default void hideBone(String boneName) {
        getAnimatableInstanceCache().hideBone(boneName);
    }

    default void showBone(String boneName) {
        getAnimatableInstanceCache().showBone(boneName);
    }

    default boolean isBoneHidden(String boneName) {
        return getAnimatableInstanceCache().isBoneHidden(boneName);
    }
}

