package com.ruben.bblib.internal.model.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.ruben.bblib.api.animatable.AnimatableManager;
import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.model.animation.BoneRenderState;
import com.ruben.bblib.api.model.animation.BoneRenderStateMap;
import com.ruben.bblib.api.model.data.BoneData;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.ModelNodeKind;
import com.ruben.bblib.api.model.data.Vec3f;
import com.ruben.bblib.api.model.transform.ResolvedNodeTransform;
import com.ruben.bblib.api.molang.MolangContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class BBModelTransformResolver {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;

    private BBModelTransformResolver() {
    }

    @Nullable
    public static ResolvedNodeTransform resolveLocatorTransform(
            Entity entity, ModelData modelData, String locatorName, AnimatableManager<?> manager,
            MolangContext molangContext, double currentTick, float partialTick,
            @Nullable BoneRenderStateMap boneRenderStates) {
        return resolveNodeTransform(entity, modelData, locatorName, null, manager, null,
                molangContext, currentTick, partialTick, boneRenderStates, true);
    }

    @Nullable
    public static ResolvedNodeTransform resolveBoneTransform(
            Entity entity, ModelData modelData, String boneName, AnimatableManager<?> manager,
            MolangContext molangContext, double currentTick, float partialTick,
            @Nullable BoneRenderStateMap boneRenderStates) {
        return resolveNodeTransform(entity, modelData, boneName, ModelNodeKind.BONE, manager, null,
                molangContext, currentTick, partialTick, boneRenderStates, false);
    }

    @Nullable
    public static ResolvedNodeTransform resolveLocatorTransform(
            Entity entity, ModelData modelData, String locatorName, AnimationController<?> controller,
            MolangContext molangContext, double currentTick, float partialTick) {
        return resolveNodeTransform(entity, modelData, locatorName, null, null, controller,
                molangContext, currentTick, partialTick, null, true);
    }

    @Nullable
    public static ResolvedNodeTransform resolveBoneTransform(
            Entity entity, ModelData modelData, String boneName, AnimationController<?> controller,
            MolangContext molangContext, double currentTick, float partialTick) {
        return resolveNodeTransform(entity, modelData, boneName, ModelNodeKind.BONE, null, controller,
                molangContext, currentTick, partialTick, null, false);
    }

    @Nullable
    private static ResolvedNodeTransform resolveNodeTransform(
            Entity entity, ModelData modelData, String nodeName, @Nullable ModelNodeKind requiredKind,
            @Nullable AnimatableManager<?> manager, @Nullable AnimationController<?> controller,
            MolangContext molangContext, double currentTick, float partialTick,
            @Nullable BoneRenderStateMap boneRenderStates, boolean locatorLookup) {
        if (nodeName == null || nodeName.isBlank()) {
            return null;
        }

        if (locatorLookup && !modelData.hasLocator(nodeName)) {
            return null;
        }

        PoseStack modelPoseStack = new PoseStack();
        PoseStack worldPoseStack = new PoseStack();
        worldPoseStack.translate(entity.getX(), entity.getY(), entity.getZ());
        applyEntityRotations(entity, worldPoseStack, partialTick);

        NodeResolutionContext context = new NodeResolutionContext(
                molangContext, currentTick, manager, controller, boneRenderStates
        );

        for (BoneData rootBone : modelData.rootBones()) {
            ResolvedNodeTransform resolved = resolveNodeTransformRecursive(
                    rootBone, nodeName, requiredKind, modelPoseStack, worldPoseStack, context, locatorLookup
            );
            if (resolved != null) {
                return resolved;
            }
        }

        return null;
    }

    @Nullable
    private static ResolvedNodeTransform resolveNodeTransformRecursive(
            BoneData node, String nodeName, @Nullable ModelNodeKind requiredKind,
            PoseStack modelPoseStack, PoseStack worldPoseStack, NodeResolutionContext context,
            boolean locatorLookup) {
        modelPoseStack.pushPose();
        worldPoseStack.pushPose();

        applyNodeTransform(node, modelPoseStack, context);
        applyNodeTransform(node, worldPoseStack, context);

        if (matchesNode(node, nodeName, requiredKind, locatorLookup)) {
            ResolvedNodeTransform resolved = captureTransform(node, modelPoseStack, worldPoseStack);
            modelPoseStack.popPose();
            worldPoseStack.popPose();
            return resolved;
        }

        for (BoneData child : node.children()) {
            ResolvedNodeTransform resolved = resolveNodeTransformRecursive(
                    child, nodeName, requiredKind, modelPoseStack, worldPoseStack, context, locatorLookup
            );
            if (resolved != null) {
                modelPoseStack.popPose();
                worldPoseStack.popPose();
                return resolved;
            }
        }

        modelPoseStack.popPose();
        worldPoseStack.popPose();
        return null;
    }

    private static boolean matchesNode(BoneData node, String nodeName, @Nullable ModelNodeKind requiredKind,
                                       boolean locatorLookup) {
        if (!node.name().equals(nodeName)) {
            return false;
        }
        if (requiredKind != null) {
            return node.kind() == requiredKind;
        }
        return locatorLookup ? node.kind().isLocatorLike() : true;
    }

    private static void applyNodeTransform(BoneData bone, PoseStack poseStack, NodeResolutionContext context) {
        ResolvedAnimationTransform transform = resolveAnimationTransform(bone, context);

        Vec3f origin = bone.origin();
        Vec3f rotation = bone.rotation().add(transform.rotation());
        Vec3f position = transform.position();
        Vec3f scale = transform.scale();

        poseStack.translate(
                (origin.x() + position.x()) * PIXEL_SCALE,
                (origin.y() + position.y()) * PIXEL_SCALE,
                (origin.z() + position.z()) * PIXEL_SCALE
        );

        if (rotation.z() != 0) poseStack.mulPose(Vector3f.ZP.rotationDegrees(rotation.z()));
        if (rotation.y() != 0) poseStack.mulPose(Vector3f.YP.rotationDegrees(rotation.y()));
        if (rotation.x() != 0) poseStack.mulPose(Vector3f.XP.rotationDegrees(rotation.x()));

        if (scale.x() != 1 || scale.y() != 1 || scale.z() != 1) {
            poseStack.scale(scale.x(), scale.y(), scale.z());
        }

        poseStack.translate(-origin.x() * PIXEL_SCALE, -origin.y() * PIXEL_SCALE, -origin.z() * PIXEL_SCALE);
    }

    private static ResolvedAnimationTransform resolveAnimationTransform(
            BoneData bone, NodeResolutionContext context) {
        Vec3f rotation = Vec3f.ZERO;
        Vec3f position = Vec3f.ZERO;
        Vec3f scale = Vec3f.ONE;

        if (context.manager() != null) {
            Vec3f combinedRotation = Vec3f.ZERO;
            Vec3f combinedPosition = Vec3f.ZERO;
            Vec3f combinedScale = Vec3f.ONE;
            boolean hasControllerAnimation = false;

            for (AnimationController<?> controller : context.manager().getAnimationControllers().values()) {
                AnimationController.BoneAnimationResult result =
                        controller.computeBoneAnimation(bone.name(), bone.uuid(), context.molangContext(), context.currentTick());

                if (result == null) {
                    continue;
                }

                hasControllerAnimation = true;
                combinedRotation = combinedRotation.add(result.rotation());
                combinedPosition = combinedPosition.add(result.position());
                combinedScale = multiplyScale(combinedScale, result.scale());
            }

            if (hasControllerAnimation) {
                rotation = combinedRotation;
                position = combinedPosition;
                scale = combinedScale;
            }
        } else if (context.controller() != null) {
            AnimationController.BoneAnimationResult result =
                    context.controller().computeBoneAnimation(bone.name(), bone.uuid(), context.molangContext(), context.currentTick());
            if (result != null) {
                rotation = result.rotation();
                position = result.position();
                scale = result.scale();
            }
        }

        if (context.boneRenderStates() != null) {
            BoneRenderState boneRenderState = context.boneRenderStates().getBone(bone.name());
            if (boneRenderState != null) {
                rotation = rotation.add(boneRenderState.getRotationOffset());
                position = position.add(boneRenderState.getPositionOffset());
                scale = multiplyScale(scale, boneRenderState.getScaleMultiplier());
            }
        }

        return new ResolvedAnimationTransform(rotation, position, scale);
    }

    private static ResolvedNodeTransform captureTransform(BoneData bone, PoseStack modelPoseStack, PoseStack worldPoseStack) {
        Vec3 pivot = new Vec3(
                bone.origin().x() * PIXEL_SCALE,
                bone.origin().y() * PIXEL_SCALE,
                bone.origin().z() * PIXEL_SCALE
        );
        return new ResolvedNodeTransform(
                transformPoint(modelPoseStack, pivot),
                transformPoint(worldPoseStack, pivot)
        );
    }

    private static Vec3 transformPoint(PoseStack poseStack, Vec3 point) {
        Vector4f vector = new Vector4f((float) point.x, (float) point.y, (float) point.z, 1.0f);
        vector.transform(poseStack.last().pose());
        return new Vec3(vector.x(), vector.y(), vector.z());
    }

    private static Vec3f multiplyScale(Vec3f left, Vec3f right) {
        return new Vec3f(left.x() * right.x(), left.y() * right.y(), left.z() * right.z());
    }

    private static void applyEntityRotations(Entity entity, PoseStack poseStack, float partialTick) {
        float bodyRotation = entity instanceof LivingEntity living
                ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
                : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        if (entity.isFullyFrozen()) {
            bodyRotation += (float) (Math.cos(entity.tickCount * 3.25d) * Math.PI * 0.4d);
        }

        if (entity.getPose() != Pose.SLEEPING) {
            poseStack.mulPose(Vector3f.YP.rotationDegrees(180.0f - bodyRotation));
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (living.deathTime > 0) {
            float deathProgress = (living.deathTime + partialTick - 1.0f) / 20.0f * 1.6f;
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(Math.min(Mth.sqrt(deathProgress), 1.0f) * 90.0f));
        } else if (living.isAutoSpinAttack()) {
            poseStack.mulPose(Vector3f.XP.rotationDegrees(-90.0f - living.getXRot()));
            poseStack.mulPose(Vector3f.YP.rotationDegrees((living.tickCount + partialTick) * -75.0f));
        } else if (entity.getPose() == Pose.SLEEPING) {
            Direction bedOrientation = living.getBedOrientation();
            poseStack.mulPose(Vector3f.YP.rotationDegrees(bedOrientation != null ? sleepDirectionToRotation(bedOrientation) : bodyRotation));
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0f));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(270.0f));
        } else if (LivingEntityRenderer.isEntityUpsideDown(living)) {
            poseStack.translate(0.0f, living.getBbHeight() + 0.1f, 0.0f);
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180.0f));
        }
    }

    private static float sleepDirectionToRotation(Direction facing) {
        return switch (facing) {
            case SOUTH -> 90.0f;
            case WEST -> 0.0f;
            case NORTH -> 270.0f;
            case EAST -> 180.0f;
            default -> 0.0f;
        };
    }

    private record ResolvedAnimationTransform(Vec3f rotation, Vec3f position, Vec3f scale) {
    }

    private record NodeResolutionContext(
            MolangContext molangContext,
            double currentTick,
            @Nullable AnimatableManager<?> manager,
            @Nullable AnimationController<?> controller,
            @Nullable BoneRenderStateMap boneRenderStates
    ) {
    }
}
