package com.ruben.bblib.api.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BBAnimation {

    private final String uuid;
    private final String name;
    private final LoopMode loopMode;
    private final float length;
    private final Map<String, BBBoneAnimator> boneAnimatorsByName;
    private final Map<String, BBBoneAnimator> boneAnimatorsByUuid;
    private final List<BBBoneAnimator> orderedBoneAnimators;

    public BBAnimation(String uuid, String name, LoopMode loopMode, float length) {
        this.uuid = uuid;
        this.name = name;
        this.loopMode = loopMode;
        this.length = length;
        this.boneAnimatorsByName = new HashMap<>();
        this.boneAnimatorsByUuid = new HashMap<>();
        this.orderedBoneAnimators = new ArrayList<>();
    }

    public void addBoneAnimator(BBBoneAnimator animator) {
        orderedBoneAnimators.add(animator);

        if (animator.getBoneName() != null && !animator.getBoneName().isEmpty()) {
            boneAnimatorsByName.put(animator.getBoneName(), animator);
        }
        if (animator.getBoneUuid() != null && !animator.getBoneUuid().isEmpty()) {
            boneAnimatorsByUuid.put(animator.getBoneUuid(), animator);
        }
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public LoopMode getLoopMode() {
        return loopMode;
    }

    public float getLength() {
        return length;
    }

    public Map<String, BBBoneAnimator> getBoneAnimators() {
        return boneAnimatorsByName;
    }

    public BBBoneAnimator getBoneAnimator(String boneName) {
        return boneAnimatorsByName.get(boneName);
    }

    public BBBoneAnimator getBoneAnimator(String boneName, String boneUuid) {
        if (boneUuid != null && !boneUuid.isEmpty()) {
            BBBoneAnimator byUuid = boneAnimatorsByUuid.get(boneUuid);
            if (byUuid != null) {
                return byUuid;
            }
        }
        return getBoneAnimator(boneName);
    }

    public enum LoopMode {
        ONCE,
        LOOP,
        HOLD;

        public static LoopMode fromString(String str) {
            return switch (str.toLowerCase()) {
                case "loop", "true" -> LOOP;
                case "hold" -> HOLD;
                default -> ONCE;
            };
        }
    }
}

