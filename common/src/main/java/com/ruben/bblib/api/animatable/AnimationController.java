package com.ruben.bblib.api.animatable;

import com.ruben.bblib.api.animation.BBAnimation;
import com.ruben.bblib.api.animation.BBBoneAnimator;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.Vec3f;
import com.ruben.bblib.api.molang.MolangContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimationController<T extends BBAnimatable> {

    @FunctionalInterface
    public interface AnimationPredicate<T extends BBAnimatable> {
        PlayState handle(AnimationState<T> state);
    }

    private final T animatable;
    private final String name;
    private final int transitionLengthTicks;
    private final AnimationPredicate<T> predicate;

    private RawAnimation currentRawAnimation;
    private int currentStageIndex;
    private BBAnimation currentAnimation;
    private boolean animationRunning;

    private double animationStartTick;
    private double transitionStartTick;
    private boolean isTransitioning;
    private final Map<String, BoneSnapshot> boneSnapshots = new HashMap<>();

    private float animationSpeed = 1.0f;
    private float currentAnimationTick;

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

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    public void setAnimationSpeed(float speed) {
        this.animationSpeed = speed;
    }

    public void setAnimation(RawAnimation animation) {
        if (animation == null || animation.getAnimationStages().isEmpty()) {
            return;
        }

        if (animation.equals(currentRawAnimation)) {
            return;
        }

        if (animationRunning && transitionLengthTicks > 0) {
            captureCurrentBoneSnapshots();
        }

        currentRawAnimation = animation;
        currentStageIndex = 0;
        currentAnimation = null;
        animationRunning = true;
    }

    public void forceAnimationReset() {
        currentRawAnimation = null;
        currentStageIndex = 0;
        currentAnimation = null;
        animationRunning = false;
        animationStartTick = 0;
        isTransitioning = false;
        currentAnimationTick = 0;
        boneSnapshots.clear();
    }

    public void process(AnimationState<T> state, ModelData modelData, double currentTick) {
        state.withController(this);

        PlayState playState = predicate.handle(state);

        if (playState == PlayState.STOP) {
            if (animationRunning) {
                captureCurrentBoneSnapshots();
                animationRunning = false;
                currentAnimation = null;
            }
            currentAnimationTick = 0;
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
                        animationStartTick = currentTick;
                        state.animationTick = 0;
                    } else {
                        animationRunning = false;
                        currentAnimation = null;
                        state.animationTick = animationLength;
                    }
                } else {
                    state.animationTick = elapsedSeconds;
                }
            }
        }

        currentAnimationTick = (float) state.animationTick;
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

