package com.ruben.bblib.api.animatable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AnimatableInstanceCache {

    private final BBAnimatable animatable;
    private final ConcurrentHashMap<Long, AnimatableManager<?>> managers = new ConcurrentHashMap<>();
    private final Set<String> hiddenBones = ConcurrentHashMap.newKeySet();

    public AnimatableInstanceCache(BBAnimatable animatable) {
        this.animatable = animatable;
    }

    @SuppressWarnings("unchecked")
    public <T extends BBAnimatable> AnimatableManager<T> getManagerForId(long uniqueId) {
        return (AnimatableManager<T>) managers.computeIfAbsent(uniqueId, k -> new AnimatableManager<>(animatable));
    }

    public void hideBone(String boneName) {
        hiddenBones.add(boneName);
    }

    public void showBone(String boneName) {
        hiddenBones.remove(boneName);
    }

    public boolean isBoneHidden(String boneName) {
        return hiddenBones.contains(boneName);
    }

    Set<String> getHiddenBones() {
        return hiddenBones;
    }
}

