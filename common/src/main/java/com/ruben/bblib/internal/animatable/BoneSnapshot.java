package com.ruben.bblib.internal.animatable;

import com.ruben.bblib.api.model.Vec3f;

public record BoneSnapshot(Vec3f rotation, Vec3f position, Vec3f scale) {

    public static BoneSnapshot identity() {
        return new BoneSnapshot(Vec3f.ZERO, Vec3f.ZERO, Vec3f.ONE);
    }

    public BoneSnapshot lerp(BoneSnapshot target, float progress) {
        return new BoneSnapshot(
                lerpVec(rotation, target.rotation, progress),
                lerpVec(position, target.position, progress),
                lerpVec(scale, target.scale, progress)
        );
    }

    private static Vec3f lerpVec(Vec3f from, Vec3f to, float progress) {
        return new Vec3f(
                from.x() + (to.x() - from.x()) * progress,
                from.y() + (to.y() - from.y()) * progress,
                from.z() + (to.z() - from.z()) * progress
        );
    }
}

