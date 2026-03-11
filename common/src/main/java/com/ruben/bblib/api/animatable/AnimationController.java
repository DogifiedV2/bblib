package com.ruben.bblib.api.animatable;

import com.ruben.bblib.api.animation.BBAnimation;
import com.ruben.bblib.api.animation.BBBoneAnimator;
import com.ruben.bblib.api.animation.keyframe.event.CustomInstructionKeyframeEvent;
import com.ruben.bblib.api.animation.keyframe.event.ParticleKeyframeEvent;
import com.ruben.bblib.api.animation.keyframe.event.SoundKeyframeEvent;
import com.ruben.bblib.api.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.KeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.ParticleKeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.SoundKeyframeData;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.Vec3f;
import com.ruben.bblib.api.molang.MolangContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AnimationController<T extends BBAnimatable> {

    @FunctionalInterface
    public interface AnimationPredicate<T extends BBAnimatable> {
        PlayState handle(AnimationState<T> state);
    }

    private final T animatable;
    private final String name;
    private final int transitionLengthTicks;
    private final AnimationPredicate<T> predicate;
    private final Map<String, RawAnimation> triggerableAnimations = new HashMap<>();
    private final Set<KeyframeData> executedKeyframes = new HashSet<>();

    private RawAnimation currentRawAnimation;
    private RawAnimation triggeredAnimation;
    private String triggeredAnimationName;
    private int currentStageIndex;
    private BBAnimation currentAnimation;
    private boolean animationRunning;
    private boolean animationFinished;
    private boolean forceAnimationReset;

    private double animationStartTick;
    private double transitionStartTick;
    private boolean isTransitioning;
    private final Map<String, BoneSnapshot> boneSnapshots = new HashMap<>();

    private float animationSpeed = 1.0f;
    private float currentAnimationTick;
    private float lastEventAnimationTick = -1.0f;

    private SoundKeyframeHandler<T> soundKeyframeHandler;
    private ParticleKeyframeHandler<T> particleKeyframeHandler;
    private CustomInstructionKeyframeHandler<T> customInstructionKeyframeHandler;

    public AnimationController(T animatable, String name, int transitionLengthTicks, AnimationPredicate<T> predicate) {
        this.animatable = animatable;
        this.name = name;
        this.transitionLengthTicks = transitionLengthTicks;
        this.predicate = predicate;
    }

    public String getName() {
        return name;
    }

    public RawAnimation getCurrentRawAnimation() {
        return currentRawAnimation;
    }

    public BBAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    @Nullable
    public RawAnimation getTriggeredAnimation() {
        return triggeredAnimation;
    }

    @Nullable
    public String getTriggeredAnimationName() {
        return triggeredAnimationName;
    }

    public boolean isPlayingTriggeredAnimation() {
        return triggeredAnimation != null && animationRunning;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public AnimationController<T> setSoundKeyframeHandler(SoundKeyframeHandler<T> soundKeyframeHandler) {
        this.soundKeyframeHandler = soundKeyframeHandler;
        return this;
    }

    public AnimationController<T> setParticleKeyframeHandler(ParticleKeyframeHandler<T> particleKeyframeHandler) {
        this.particleKeyframeHandler = particleKeyframeHandler;
        return this;
    }

    public AnimationController<T> setCustomInstructionKeyframeHandler(
            CustomInstructionKeyframeHandler<T> customInstructionKeyframeHandler) {
        this.customInstructionKeyframeHandler = customInstructionKeyframeHandler;
        return this;
    }

    public void setAnimationSpeed(float speed) {
        this.animationSpeed = speed;
    }

    public void setAnimation(RawAnimation animation) {
        if (animation == null || animation.getAnimationStages().isEmpty()) {
            stop();
            return;
        }

        if (!forceAnimationReset && animation.equals(currentRawAnimation) && !animationFinished) {
            return;
        }

        if (animationRunning && transitionLengthTicks > 0) {
            captureCurrentBoneSnapshots();
        }

        currentRawAnimation = animation;
        currentStageIndex = 0;
        currentAnimation = null;
        animationRunning = true;
        animationFinished = false;
        forceAnimationReset = false;
        resetExecutedKeyframes();
    }

    public void forceAnimationReset() {
        currentRawAnimation = null;
        currentStageIndex = 0;
        currentAnimation = null;
        animationRunning = false;
        animationStartTick = 0;
        isTransitioning = false;
        currentAnimationTick = 0;
        animationFinished = false;
        forceAnimationReset = false;
        boneSnapshots.clear();
        resetExecutedKeyframes();
    }

    public AnimationController<T> triggerable(String animationName, RawAnimation animation) {
        triggerableAnimations.put(animationName, animation);
        return this;
    }

    public boolean triggerAnimation(String animationName) {
        RawAnimation animation = triggerableAnimations.get(animationName);
        if (animation == null) {
            return false;
        }

        triggeredAnimation = animation;
        triggeredAnimationName = animationName;
        forceAnimationReset = true;
        setAnimation(animation);
        return true;
    }

    public boolean stopTriggeredAnimation() {
        if (triggeredAnimation == null) {
            return false;
        }

        clearTriggeredAnimation();
        forceAnimationReset();
        return true;
    }

    public boolean stopTriggeredAnimation(String animationName) {
        if (!Objects.equals(triggeredAnimationName, animationName)) {
            return false;
        }
        return stopTriggeredAnimation();
    }

    public boolean resetTriggeredAnimation() {
        if (triggeredAnimationName == null) {
            return false;
        }
        return triggerAnimation(triggeredAnimationName);
    }

    public boolean resetTriggeredAnimation(String animationName) {
        if (!Objects.equals(triggeredAnimationName, animationName)) {
            return false;
        }
        return triggerAnimation(animationName);
    }

    public void process(AnimationState<T> state, ModelData modelData, double currentTick) {
        state.withController(this);

        PlayState playState;
        if (triggeredAnimation != null) {
            setAnimation(triggeredAnimation);
            playState = PlayState.CONTINUE;
        } else {
            playState = predicate.handle(state);
        }

        if (playState == PlayState.STOP) {
            if (animationRunning) {
                captureCurrentBoneSnapshots();
            }
            stop();
            return;
        }

        if (currentRawAnimation == null || currentRawAnimation.getAnimationStages().isEmpty()) {
            return;
        }

        List<RawAnimation.Stage> stages = currentRawAnimation.getAnimationStages();
        if (currentStageIndex >= stages.size()) {
            currentStageIndex = stages.size() - 1;
        }

        RawAnimation.Stage currentStage = stages.get(currentStageIndex);

        BBAnimation targetAnimation = modelData.getAnimation(currentStage.animationName());
        if (targetAnimation == null) {
            return;
        }

        if (currentAnimation != targetAnimation) {
            if (currentAnimation != null && transitionLengthTicks > 0 && !boneSnapshots.isEmpty()) {
                isTransitioning = true;
                transitionStartTick = currentTick;
            }
            currentAnimation = targetAnimation;
            animationStartTick = currentTick;
            resetExecutedKeyframes();
        }

        float animationLength = currentAnimation.getLength();
        if (animationLength <= 0) {
            return;
        }

        double elapsedTicks = (currentTick - animationStartTick) * animationSpeed;
        double elapsedSeconds = elapsedTicks / 20.0;

        BBAnimation.LoopMode effectiveLoopMode = currentStage.loopMode();

        switch (effectiveLoopMode) {
            case LOOP -> state.animationTick = elapsedSeconds % animationLength;
            case HOLD -> state.animationTick = Math.min(elapsedSeconds, animationLength);
            case ONCE -> {
                if (elapsedSeconds > animationLength) {
                    if (currentStageIndex + 1 < stages.size()) {
                        captureCurrentBoneSnapshots();
                        currentStageIndex++;
                        resetExecutedKeyframes();
                        animationStartTick = currentTick;
                        state.animationTick = 0;
                        currentAnimationTick = 0;
                        return;
                    } else {
                        animationRunning = false;
                        animationFinished = true;
                        state.animationTick = animationLength;
                    }
                } else {
                    state.animationTick = elapsedSeconds;
                }
            }
        }

        currentAnimationTick = (float) state.animationTick;
        dispatchKeyframeEvents(currentAnimationTick);

        if (animationFinished) {
            currentAnimation = null;
        }

        if (triggeredAnimation != null && animationFinished) {
            clearTriggeredAnimation();
            forceAnimationReset();
        }
    }

    public BoneAnimationResult computeBoneAnimation(String boneName, String boneUuid,
                                                     MolangContext molangContext, double currentTick) {
        if (currentAnimation == null) {
            BoneSnapshot snapshot = boneSnapshots.get(boneName);
            if (snapshot != null) {
                return new BoneAnimationResult(snapshot.rotation(), snapshot.position(), snapshot.scale(), false);
            }
            return null;
        }

        BBBoneAnimator boneAnimator = currentAnimation.getBoneAnimator(boneName, boneUuid);
        Vec3f animRotation = null;
        Vec3f animPosition = null;
        Vec3f animScale = null;
        MolangContext controllerContext = molangContext.copy().withAnimTime(currentAnimationTick);

        if (boneAnimator != null) {
            animRotation = boneAnimator.getRotationAt(currentAnimationTick, controllerContext);
            animPosition = boneAnimator.getPositionAt(currentAnimationTick, controllerContext);
            animScale = boneAnimator.getScaleAt(currentAnimationTick, controllerContext);
        }

        if (isTransitioning && transitionLengthTicks > 0) {
            double transitionElapsed = currentTick - transitionStartTick;
            float progress = (float) Math.min(1.0, transitionElapsed / transitionLengthTicks);

            if (progress >= 1.0f) {
                isTransitioning = false;
                boneSnapshots.clear();
            } else {
                BoneSnapshot snapshot = boneSnapshots.getOrDefault(boneName, BoneSnapshot.identity());
                BoneSnapshot targetSnapshot = new BoneSnapshot(
                        animRotation != null ? animRotation : Vec3f.ZERO,
                        animPosition != null ? animPosition : Vec3f.ZERO,
                        animScale != null ? animScale : Vec3f.ONE
                );
                BoneSnapshot blended = snapshot.lerp(targetSnapshot, progress);
                return new BoneAnimationResult(blended.rotation(), blended.position(), blended.scale(), true);
            }
        }

        if (animRotation == null && animPosition == null && animScale == null) {
            return null;
        }

        return new BoneAnimationResult(
                animRotation != null ? animRotation : Vec3f.ZERO,
                animPosition != null ? animPosition : Vec3f.ZERO,
                animScale != null ? animScale : Vec3f.ONE,
                false
        );
    }

    private void captureCurrentBoneSnapshots() {
        // Snapshots are captured by the renderer when it processes bones
        // This is a marker that transition should happen
    }

    private void stop() {
        animationRunning = false;
        currentAnimation = null;
        currentAnimationTick = 0;
        animationFinished = false;
        resetExecutedKeyframes();
    }

    private void clearTriggeredAnimation() {
        triggeredAnimation = null;
        triggeredAnimationName = null;
    }

    private void dispatchKeyframeEvents(float animationTime) {
        if (currentAnimation == null) {
            return;
        }

        if (lastEventAnimationTick >= 0 && animationTime < lastEventAnimationTick) {
            resetExecutedKeyframes();
        }

        BBAnimation.Keyframes keyframes = currentAnimation.getKeyframes();

        for (SoundKeyframeData keyframeData : keyframes.sounds()) {
            if (animationTime >= keyframeData.startTime() && executedKeyframes.add(keyframeData) && soundKeyframeHandler != null) {
                soundKeyframeHandler.handle(new SoundKeyframeEvent<>(animatable, animationTime, this, keyframeData));
            }
        }

        for (ParticleKeyframeData keyframeData : keyframes.particles()) {
            if (animationTime >= keyframeData.startTime() && executedKeyframes.add(keyframeData) && particleKeyframeHandler != null) {
                particleKeyframeHandler.handle(new ParticleKeyframeEvent<>(animatable, animationTime, this, keyframeData));
            }
        }

        for (CustomInstructionKeyframeData keyframeData : keyframes.customInstructions()) {
            if (animationTime >= keyframeData.startTime() && executedKeyframes.add(keyframeData) && customInstructionKeyframeHandler != null) {
                customInstructionKeyframeHandler.handle(new CustomInstructionKeyframeEvent<>(animatable, animationTime, this, keyframeData));
            }
        }

        lastEventAnimationTick = animationTime;
    }

    private void resetExecutedKeyframes() {
        executedKeyframes.clear();
        lastEventAnimationTick = -1.0f;
    }

    @FunctionalInterface
    public interface SoundKeyframeHandler<A extends BBAnimatable> {
        void handle(SoundKeyframeEvent<A> event);
    }

    @FunctionalInterface
    public interface ParticleKeyframeHandler<A extends BBAnimatable> {
        void handle(ParticleKeyframeEvent<A> event);
    }

    @FunctionalInterface
    public interface CustomInstructionKeyframeHandler<A extends BBAnimatable> {
        void handle(CustomInstructionKeyframeEvent<A> event);
    }

    public void saveBoneSnapshot(String boneName, Vec3f rotation, Vec3f position, Vec3f scale) {
        boneSnapshots.put(boneName, new BoneSnapshot(rotation, position, scale));
    }

    public record BoneAnimationResult(Vec3f rotation, Vec3f position, Vec3f scale, boolean isBlended) {
    }

    private record BoneSnapshot(Vec3f rotation, Vec3f position, Vec3f scale) {
        private static BoneSnapshot identity() {
            return new BoneSnapshot(Vec3f.ZERO, Vec3f.ZERO, Vec3f.ONE);
        }

        private BoneSnapshot lerp(BoneSnapshot target, float progress) {
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
}

