package com.ruben.bblib.api.animation;

import com.ruben.bblib.api.model.data.Vec3f;
import com.ruben.bblib.api.molang.MolangContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BBBoneAnimator {

    private final String boneUuid;
    private final String boneName;
    private final List<BBKeyframe> rotationKeyframes;
    private final List<BBKeyframe> positionKeyframes;
    private final List<BBKeyframe> scaleKeyframes;

    public BBBoneAnimator(String boneUuid, String boneName) {
        this.boneUuid = boneUuid;
        this.boneName = boneName;
        this.rotationKeyframes = new ArrayList<>();
        this.positionKeyframes = new ArrayList<>();
        this.scaleKeyframes = new ArrayList<>();
    }

    public void addKeyframe(BBKeyframe keyframe) {
        List<BBKeyframe> targetList = switch (keyframe.channel()) {
            case ROTATION -> rotationKeyframes;
            case POSITION -> positionKeyframes;
            case SCALE -> scaleKeyframes;
        };
        targetList.add(keyframe);
    }

    public void sortKeyframes() {
        Collections.sort(rotationKeyframes);
        Collections.sort(positionKeyframes);
        Collections.sort(scaleKeyframes);
    }

    public String getBoneUuid() {
        return boneUuid;
    }

    public String getBoneName() {
        return boneName;
    }

    public List<BBKeyframe> getRotationKeyframes() {
        return rotationKeyframes;
    }

    public List<BBKeyframe> getPositionKeyframes() {
        return positionKeyframes;
    }

    public List<BBKeyframe> getScaleKeyframes() {
        return scaleKeyframes;
    }

    public Vec3f getRotationAt(float time) {
        return getRotationAt(time, new MolangContext().withAnimTime(time));
    }

    public Vec3f getRotationAt(float time, MolangContext context) {
        return interpolate(rotationKeyframes, time, context);
    }

    public Vec3f getPositionAt(float time) {
        return getPositionAt(time, new MolangContext().withAnimTime(time));
    }

    public Vec3f getPositionAt(float time, MolangContext context) {
        return interpolate(positionKeyframes, time, context);
    }

    public Vec3f getScaleAt(float time) {
        return getScaleAt(time, new MolangContext().withAnimTime(time));
    }

    public Vec3f getScaleAt(float time, MolangContext context) {
        Vec3f scale = interpolate(scaleKeyframes, time, context);
        if (scale == null) {
            return new Vec3f(1, 1, 1);
        }
        return scale;
    }

    private Vec3f interpolate(List<BBKeyframe> keyframes, float time, MolangContext context) {
        if (keyframes.isEmpty()) {
            return null;
        }

        if (keyframes.size() == 1) {
            return keyframes.getFirst().evaluate(context);
        }

        if (time <= keyframes.getFirst().time()) {
            return keyframes.getFirst().evaluate(context);
        }

        if (time >= keyframes.getLast().time()) {
            return keyframes.getLast().evaluate(context);
        }

        BBKeyframe before = null;
        BBKeyframe after = null;

        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (keyframes.get(i).time() <= time && keyframes.get(i + 1).time() >= time) {
                before = keyframes.get(i);
                after = keyframes.get(i + 1);
                break;
            }
        }

        if (before == null || after == null) {
            return keyframes.getFirst().evaluate(context);
        }

        float t = (time - before.time()) / (after.time() - before.time());

        return switch (before.interpolation()) {
            case STEP -> before.evaluate(context);
            case CATMULLROM -> interpolateCatmullRom(keyframes, before, after, t, context);
            default -> lerp(before.evaluate(context), after.evaluate(context), t);
        };
    }

    private Vec3f lerp(Vec3f a, Vec3f b, float t) {
        return new Vec3f(
                a.x() + (b.x() - a.x()) * t,
                a.y() + (b.y() - a.y()) * t,
                a.z() + (b.z() - a.z()) * t
        );
    }

    private Vec3f interpolateCatmullRom(List<BBKeyframe> keyframes, BBKeyframe before, BBKeyframe after, float t, MolangContext context) {
        int beforeIndex = keyframes.indexOf(before);
        int afterIndex = keyframes.indexOf(after);

        Vec3f p0 = beforeIndex > 0 ? keyframes.get(beforeIndex - 1).evaluate(context) : before.evaluate(context);
        Vec3f p1 = before.evaluate(context);
        Vec3f p2 = after.evaluate(context);
        Vec3f p3 = afterIndex < keyframes.size() - 1 ? keyframes.get(afterIndex + 1).evaluate(context) : after.evaluate(context);

        return catmullRom(p0, p1, p2, p3, t);
    }

    private Vec3f catmullRom(Vec3f p0, Vec3f p1, Vec3f p2, Vec3f p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;

        float x = 0.5f * ((2 * p1.x()) + (-p0.x() + p2.x()) * t + (2 * p0.x() - 5 * p1.x() + 4 * p2.x() - p3.x()) * t2 + (-p0.x() + 3 * p1.x() - 3 * p2.x() + p3.x()) * t3);
        float y = 0.5f * ((2 * p1.y()) + (-p0.y() + p2.y()) * t + (2 * p0.y() - 5 * p1.y() + 4 * p2.y() - p3.y()) * t2 + (-p0.y() + 3 * p1.y() - 3 * p2.y() + p3.y()) * t3);
        float z = 0.5f * ((2 * p1.z()) + (-p0.z() + p2.z()) * t + (2 * p0.z() - 5 * p1.z() + 4 * p2.z() - p3.z()) * t2 + (-p0.z() + 3 * p1.z() - 3 * p2.z() + p3.z()) * t3);

        return new Vec3f(x, y, z);
    }
}

