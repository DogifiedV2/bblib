package com.ruben.bblib.api.animation;

import com.ruben.bblib.api.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.ParticleKeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.SoundKeyframeData;

import java.util.ArrayList;
import java.util.Collections;
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
    private final Keyframes keyframes;

    public BBAnimation(String uuid, String name, LoopMode loopMode, float length) {
        this.uuid = uuid;
        this.name = name;
        this.loopMode = loopMode;
        this.length = length;
        this.boneAnimatorsByName = new HashMap<>();
        this.boneAnimatorsByUuid = new HashMap<>();
        this.orderedBoneAnimators = new ArrayList<>();
        this.keyframes = new Keyframes(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
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

    public Keyframes getKeyframes() {
        return keyframes;
    }

    public void addSoundKeyframe(SoundKeyframeData keyframe) {
        keyframes.sounds().add(keyframe);
    }

    public void addParticleKeyframe(ParticleKeyframeData keyframe) {
        keyframes.particles().add(keyframe);
    }

    public void addCustomInstructionKeyframe(CustomInstructionKeyframeData keyframe) {
        keyframes.customInstructions().add(keyframe);
    }

    public void sortKeyframes() {
        keyframes.sounds().sort((left, right) -> Double.compare(left.startTime(), right.startTime()));
        keyframes.particles().sort((left, right) -> Double.compare(left.startTime(), right.startTime()));
        keyframes.customInstructions().sort((left, right) -> Double.compare(left.startTime(), right.startTime()));
    }

    public record Keyframes(List<SoundKeyframeData> sounds,
                            List<ParticleKeyframeData> particles,
                            List<CustomInstructionKeyframeData> customInstructions) {
        public List<SoundKeyframeData> soundsView() {
            return Collections.unmodifiableList(sounds);
        }

        public List<ParticleKeyframeData> particlesView() {
            return Collections.unmodifiableList(particles);
        }

        public List<CustomInstructionKeyframeData> customInstructionsView() {
            return Collections.unmodifiableList(customInstructions);
        }
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

