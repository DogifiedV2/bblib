package com.ruben.bblib.api.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ruben.bblib.api.BBLibApi;
import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.api.animation.BBAnimation;
import com.ruben.bblib.api.animation.BBBoneAnimator;
import com.ruben.bblib.api.animatable.AnimatableInstanceCache;
import com.ruben.bblib.api.animatable.AnimatableManager;
import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.AnimationState;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.api.model.data.BoneData;
import com.ruben.bblib.api.model.data.CubeData;
import com.ruben.bblib.api.model.data.FaceData;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.TextureData;
import com.ruben.bblib.api.model.data.UV;
import com.ruben.bblib.api.model.data.Vec3f;
import com.ruben.bblib.internal.client.texture.MissingModelTexture;
import com.ruben.bblib.internal.renderer.BBRenderType;
import com.ruben.bblib.api.molang.EntityContext;
import com.ruben.bblib.api.molang.MolangContext;
import com.ruben.bblib.internal.renderer.FaceUvUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class BBEntityRenderer<T extends Entity & BBAnimatable> extends EntityRenderer<T> {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;

    protected final BBModel<T> model;

    public BBEntityRenderer(EntityRendererProvider.Context context, BBModel<T> model) {
        super(context);
        this.model = model;
    }

    public BBModel<T> getModel() {
        return model;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ModelData modelData = model.getModelData(entity);
        if (modelData == null) {
            BBLibCommon.LOGGER.warn("No model data found for {}, cache has {} models",
                    model.getModelResource(entity), BBLibApi.getLoadedModelCount());
            renderMissingModel(poseStack, bufferSource, packedLight);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        if (modelData.textures().isEmpty()) {
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }

        poseStack.pushPose();

        applyEntityRotations(entity, poseStack, partialTick);
        int packedOverlay = getOverlay(entity, partialTick);

        double currentTick = entity.tickCount + partialTick;
        AnimatableInstanceCache cache = entity.getAnimatableInstanceCache();
        AnimatableManager<T> manager = cache.getManagerForId(entity.getId());

        if (manager.getFirstTickTime() == -1) {
            manager.startedAt(currentTick);
        }

        boolean isMoving = isEntityMoving(entity, partialTick);
        AnimationState<T> animationState = new AnimationState<>(entity, partialTick, isMoving);
        double renderTick = currentTick - manager.getFirstTickTime();
        animationState.animationTick = renderTick;

        for (AnimationController<T> controller : manager.getAnimationControllers().values()) {
            animationState.animationTick = renderTick;
            controller.process(animationState, modelData, currentTick);
        }

        manager.updatedAt(currentTick);
        if (manager.isFirstTick()) {
            manager.finishFirstTick();
        }

        MolangContext molangContext = buildMolangContext(entity, (float) renderTick, partialTick);

        renderResolvedModel(model, entity, modelData, model.getModelResource(entity), poseStack, bufferSource, packedLight,
                packedOverlay, manager, null, (float) renderTick, molangContext, currentTick);
        renderPostModelExtras(entity, poseStack, bufferSource, packedLight, packedOverlay,
                (float) renderTick, molangContext, currentTick);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        ModelData modelData = model.getModelData(entity);
        if (modelData == null) {
            return MissingModelTexture.getLocation();
        }

        if (modelData.textures().isEmpty()) {
            ResourceLocation overrideTexture = model.getTextureResource(entity);
            return overrideTexture != null ? overrideTexture : MissingModelTexture.getLocation();
        }

        return resolveTextureLocation(model, entity, modelData, model.getModelResource(entity), 0, modelData.textures().get(0));
    }

    protected int getOverlay(T entity, float partialTick) {
        if (entity instanceof LivingEntity living) {
            float hurtTime = (float) living.hurtTime - partialTick;
            return OverlayTexture.pack(
                    OverlayTexture.u(hurtTime < 0 ? 0 : hurtTime / 10.0f),
                    OverlayTexture.v(living.hurtTime > 0 || living.deathTime > 0));
        }
        return OverlayTexture.NO_OVERLAY;
    }

    protected float getMotionAnimThreshold(T entity) {
        return 0.015f;
    }

    protected boolean isEntityMoving(T entity, float partialTick) {
        Vec3 velocity = entity.getDeltaMovement();
        float averageLateralVelocity = (float) ((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0);

        if (entity instanceof LivingEntity living) {
            float limbSwingAmount = living.walkAnimation.speed(partialTick);
            return averageLateralVelocity >= getMotionAnimThreshold(entity) && limbSwingAmount != 0.0f;
        }

        return averageLateralVelocity >= getMotionAnimThreshold(entity);
    }

    protected boolean isShaking(T entity) {
        return entity.isFullyFrozen();
    }

    protected void applyEntityRotations(T entity, PoseStack poseStack, float partialTick) {
        float bodyRotation = entity instanceof LivingEntity living
                ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
                : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());

        if (isShaking(entity)) {
            bodyRotation += (float) (Math.cos(entity.tickCount * 3.25d) * Math.PI * 0.4d);
        }

        if (!entity.hasPose(Pose.SLEEPING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyRotation));
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (living.deathTime > 0) {
            float deathProgress = (living.deathTime + partialTick - 1.0f) / 20.0f * 1.6f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathProgress), 1.0f) * 90.0f));
        } else if (living.isAutoSpinAttack()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f - living.getXRot()));
            poseStack.mulPose(Axis.YP.rotationDegrees((living.tickCount + partialTick) * -75.0f));
        } else if (entity.hasPose(Pose.SLEEPING)) {
            Direction bedOrientation = living.getBedOrientation();
            poseStack.mulPose(Axis.YP.rotationDegrees(bedOrientation != null ? sleepDirectionToRotation(bedOrientation) : bodyRotation));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0f));
        } else if (LivingEntityRenderer.isEntityUpsideDown(living)) {
            poseStack.translate(0.0f, living.getBbHeight() + 0.1f, 0.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
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

    private MolangContext buildMolangContext(T entity, float animTime, float partialTick) {
        EntityContext entityContext = createEntityContext(entity);
        return new MolangContext()
                .withAnimTime(animTime)
                .withPartialTick(partialTick)
                .withEntityContext(entityContext);
    }

    private EntityContext createEntityContext(T entity) {
        if (entity instanceof LivingEntity living) {
            return new EntityContext() {
                @Override public double getHealth() { return living.getHealth(); }
                @Override public double getMaxHealth() { return living.getMaxHealth(); }
                @Override public double getGroundSpeed() {
                    Vec3 velocity = living.getDeltaMovement();
                    return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                }
                @Override public double getVerticalSpeed() {
                    return living.getDeltaMovement().y;
                }
                @Override public boolean isOnGround() { return living.onGround(); }
                @Override public boolean isInWater() { return living.isInWater(); }
                @Override public boolean isMoving() {
                    Vec3 velocity = living.getDeltaMovement();
                    float averageLateralVelocity = (float) ((Math.abs(velocity.x) + Math.abs(velocity.z)) / 2.0);
                    return averageLateralVelocity >= getMotionAnimThreshold(entity);
                }
                @Override public double getHurtTime() { return living.hurtTime; }
                @Override public double getDeathTime() { return living.deathTime; }
            };
        }
        return EntityContext.EMPTY;
    }

    protected void renderBoneAttachments(T entity, BoneData bone, ModelData modelData, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                         float animationTime, MolangContext molangContext, double currentTick) {
    }

    protected void renderPostModelExtras(T entity, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, float animationTime,
                                         MolangContext molangContext, double currentTick) {
    }

    protected final void renderAdditionalModel(BBModel<T> additionalModel, T entity, PoseStack poseStack,
                                               MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                               @Nullable String animationName, float animationTime,
                                               MolangContext molangContext, double currentTick) {
        ModelData modelData = additionalModel.getModelData(entity);
        if (modelData == null || modelData.textures().isEmpty()) {
            return;
        }

        BBAnimation animation = animationName != null ? modelData.getAnimation(animationName) : null;
        renderResolvedModel(additionalModel, entity, modelData, additionalModel.getModelResource(entity), poseStack,
                bufferSource, packedLight, packedOverlay, null, animation, animationTime, molangContext, currentTick);
    }

    private void renderResolvedModel(BBModel<T> renderModel, T entity, ModelData modelData, ResourceLocation modelId,
                                     PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                     @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                     float animationTime, MolangContext molangContext, double currentTick) {
        if (modelData.freeFormat()) {
            renderFreeFormat(renderModel, entity, modelData, modelId, poseStack, bufferSource, packedLight, packedOverlay,
                    manager, animation, animationTime, molangContext, currentTick);
        } else {
            renderModdedFormat(renderModel, entity, modelData, modelId, poseStack, bufferSource, packedLight, packedOverlay,
                    manager, animation, animationTime, molangContext, currentTick);
        }
    }

    // ===== Modded format rendering =====

    private void renderModdedFormat(BBModel<T> renderModel, T entity, ModelData modelData, ResourceLocation modelId,
                                    PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                    @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                    float animationTime, MolangContext molangContext, double currentTick) {
        TextureData primaryTexture = modelData.textures().get(0);
        ResourceLocation textureLocation = resolveTextureLocation(renderModel, entity, modelData, modelId, 0, primaryTexture);
        RenderType renderType = BBRenderType.entityCutoutNoCull(textureLocation);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float texWidth = modelData.textureWidth();
        float texHeight = modelData.textureHeight();

        for (BoneData bone : modelData.rootBones()) {
            renderBoneModded(renderModel, entity, bone, modelData, modelId, poseStack, bufferSource, vertexConsumer,
                    packedLight, packedOverlay, texWidth, texHeight, manager, animation,
                    animationTime, molangContext, false, currentTick);
        }

        int glowmaskIndex = modelData.findGlowmaskTextureIndex(0);
        if (glowmaskIndex < 0) {
            return;
        }

        TextureData glowmaskData = modelData.textures().get(glowmaskIndex);
        ResourceLocation glowmaskLocation = resolveTextureLocation(renderModel, entity, modelData, modelId, glowmaskIndex, glowmaskData);

        RenderType emissiveRenderType = BBRenderType.entityTranslucentEmissive(glowmaskLocation);
        VertexConsumer emissiveConsumer = bufferSource.getBuffer(emissiveRenderType);

        for (BoneData bone : modelData.rootBones()) {
            renderBoneModded(renderModel, entity, bone, modelData, modelId, poseStack, bufferSource, emissiveConsumer,
                    LightTexture.FULL_BRIGHT, packedOverlay,
                    texWidth, texHeight, manager, animation,
                    animationTime, molangContext, true, currentTick);
        }
    }

    private void renderBoneModded(BBModel<T> renderModel, T entity, BoneData bone, ModelData modelData, ResourceLocation modelId,
                                  PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer vertexConsumer,
                                  int packedLight, int packedOverlay, float texWidth, float texHeight,
                                  @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                  float animationTime, MolangContext molangContext, boolean emissivePass,
                                  double currentTick) {
        if (entity.isBoneHidden(bone.name())) {
            return;
        }

        poseStack.pushPose();

        applyBoneTransform(bone, poseStack, manager, animation, animationTime, molangContext, currentTick);

        if (!emissivePass) {
            renderBoneAttachments(entity, bone, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                    animationTime, molangContext, currentTick);
        }

        Map<String, CubeData> cubes = modelData.cubes();

        for (String cubeUuid : bone.cubeUuids()) {
            CubeData cube = cubes.get(cubeUuid);
            if (cube != null) {
                renderCubeModded(cube, poseStack, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight);
            }
        }

        for (BoneData child : bone.children()) {
            renderBoneModded(renderModel, entity, child, modelData, modelId, poseStack, bufferSource, vertexConsumer,
                    packedLight, packedOverlay, texWidth, texHeight, manager, animation,
                    animationTime, molangContext, emissivePass, currentTick);
        }

        poseStack.popPose();
    }

    private void renderCubeModded(CubeData cube, PoseStack poseStack, VertexConsumer vertexConsumer,
                                   int packedLight, int packedOverlay, float texWidth, float texHeight) {
        poseStack.pushPose();

        Vec3f origin = cube.origin();
        poseStack.translate(origin.x() * PIXEL_SCALE, origin.y() * PIXEL_SCALE, origin.z() * PIXEL_SCALE);

        Vec3f rotation = cube.rotation();
        if (rotation.z() != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
        if (rotation.y() != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        if (rotation.x() != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));

        poseStack.translate(-origin.x() * PIXEL_SCALE, -origin.y() * PIXEL_SCALE, -origin.z() * PIXEL_SCALE);

        float inflate = cube.inflate() * PIXEL_SCALE;
        float x1 = cube.from().x() * PIXEL_SCALE - inflate;
        float y1 = cube.from().y() * PIXEL_SCALE - inflate;
        float z1 = cube.from().z() * PIXEL_SCALE - inflate;
        float x2 = cube.to().x() * PIXEL_SCALE + inflate;
        float y2 = cube.to().y() * PIXEL_SCALE + inflate;
        float z2 = cube.to().z() * PIXEL_SCALE + inflate;

        PoseStack.Pose pose = poseStack.last();

        renderFaceModded(cube, CubeData.Face.NORTH, pose, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight,
                x2, y1, z1, x1, y2, z1, 0, 0, -1);
        renderFaceModded(cube, CubeData.Face.SOUTH, pose, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight,
                x1, y1, z2, x2, y2, z2, 0, 0, 1);
        renderFaceModded(cube, CubeData.Face.EAST, pose, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight,
                x2, y1, z2, x2, y2, z1, 1, 0, 0);
        renderFaceModded(cube, CubeData.Face.WEST, pose, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight,
                x1, y1, z1, x1, y2, z2, -1, 0, 0);
        renderFaceModded(cube, CubeData.Face.UP, pose, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight,
                x1, y2, z1, x2, y2, z2, 0, 1, 0);
        renderFaceModded(cube, CubeData.Face.DOWN, pose, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight,
                x1, y1, z2, x2, y1, z1, 0, -1, 0);

        poseStack.popPose();
    }

    private void renderFaceModded(CubeData cube, CubeData.Face face, PoseStack.Pose pose,
                                   VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                                   float texWidth, float texHeight,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float nx, float ny, float nz) {
        FaceData faceData = cube.faces().get(face);
        if (faceData == null) {
            return;
        }

        UV uv = faceData.uv();
        float u1 = uv.u1() / texWidth;
        float v1 = uv.v1() / texHeight;
        float u2 = uv.u2() / texWidth;
        float v2 = uv.v2() / texHeight;

        switch (face) {
            case NORTH -> {
                vertex(vertexConsumer, pose, x1, y1, z1, u1, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z1, u2, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z1, u2, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z1, u1, v1, nx, ny, nz, packedLight, packedOverlay);
            }
            case SOUTH -> {
                vertex(vertexConsumer, pose, x1, y1, z2, u1, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z2, u1, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z2, u2, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z2, u2, v2, nx, ny, nz, packedLight, packedOverlay);
            }
            case EAST -> {
                vertex(vertexConsumer, pose, x2, y1, z1, u1, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z2, u2, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z2, u2, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z1, u1, v1, nx, ny, nz, packedLight, packedOverlay);
            }
            case WEST -> {
                vertex(vertexConsumer, pose, x1, y1, z2, u2, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y1, z1, u1, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z1, u1, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z2, u2, v1, nx, ny, nz, packedLight, packedOverlay);
            }
            case UP -> {
                vertex(vertexConsumer, pose, x1, y2, z1, u1, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z1, u2, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z2, u2, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z2, u1, v2, nx, ny, nz, packedLight, packedOverlay);
            }
            case DOWN -> {
                vertex(vertexConsumer, pose, x1, y1, z2, u1, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z2, u2, v2, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z1, u2, v1, nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y1, z1, u1, v1, nx, ny, nz, packedLight, packedOverlay);
            }
        }
    }

    // ===== Free format rendering =====

    private void renderFreeFormat(BBModel<T> renderModel, T entity, ModelData modelData, ResourceLocation modelId, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                  @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                  float animationTime, MolangContext molangContext, double currentTick) {
        for (BoneData bone : modelData.rootBones()) {
            renderBoneFree(renderModel, entity, bone, modelData, modelId, poseStack, bufferSource, packedLight, packedOverlay,
                    manager, animation, animationTime, molangContext, false, currentTick);
        }

        int glowmaskIndex = modelData.findGlowmaskTextureIndex(0);
        if (glowmaskIndex < 0) {
            return;
        }

        for (BoneData bone : modelData.rootBones()) {
            renderBoneFree(renderModel, entity, bone, modelData, modelId, poseStack, bufferSource,
                    LightTexture.FULL_BRIGHT, packedOverlay,
                    manager, animation, animationTime, molangContext, true, currentTick);
        }
    }

    private void renderBoneFree(BBModel<T> renderModel, T entity, BoneData bone, ModelData modelData, ResourceLocation modelId,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay,
                                @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                float animationTime, MolangContext molangContext, boolean emissivePass,
                                double currentTick) {
        if (entity.isBoneHidden(bone.name())) {
            return;
        }

        poseStack.pushPose();

        applyBoneTransform(bone, poseStack, manager, animation, animationTime, molangContext, currentTick);

        if (!emissivePass) {
            renderBoneAttachments(entity, bone, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                    animationTime, molangContext, currentTick);
        }

        Map<String, CubeData> cubes = modelData.cubes();
        List<TextureData> textures = modelData.textures();
        float texWidth = modelData.textureWidth();
        float texHeight = modelData.textureHeight();

        for (String cubeUuid : bone.cubeUuids()) {
            CubeData cube = cubes.get(cubeUuid);
            if (cube != null) {
                renderCubeFree(renderModel, entity, modelData, cube, modelId, textures, poseStack, bufferSource,
                        packedLight, packedOverlay, texWidth, texHeight, emissivePass);
            }
        }

        for (BoneData child : bone.children()) {
            renderBoneFree(renderModel, entity, child, modelData, modelId, poseStack, bufferSource, packedLight, packedOverlay,
                    manager, animation, animationTime, molangContext, emissivePass, currentTick);
        }

        poseStack.popPose();
    }

    private void renderCubeFree(BBModel<T> renderModel, T entity, ModelData modelData, CubeData cube, ResourceLocation modelId, List<TextureData> textures,
                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay, float defaultTexWidth, float defaultTexHeight,
                                 boolean emissivePass) {
        if (!cube.visible()) {
            return;
        }

        poseStack.pushPose();

        Vec3f origin = cube.origin();
        poseStack.translate(origin.x() * PIXEL_SCALE, origin.y() * PIXEL_SCALE, origin.z() * PIXEL_SCALE);

        Vec3f rotation = cube.rotation();
        if (rotation.z() != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
        if (rotation.y() != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        if (rotation.x() != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));

        poseStack.translate(-origin.x() * PIXEL_SCALE, -origin.y() * PIXEL_SCALE, -origin.z() * PIXEL_SCALE);

        float inflate = cube.inflate() * PIXEL_SCALE;
        float x1 = cube.from().x() * PIXEL_SCALE - inflate;
        float y1 = cube.from().y() * PIXEL_SCALE - inflate;
        float z1 = cube.from().z() * PIXEL_SCALE - inflate;
        float x2 = cube.to().x() * PIXEL_SCALE + inflate;
        float y2 = cube.to().y() * PIXEL_SCALE + inflate;
        float z2 = cube.to().z() * PIXEL_SCALE + inflate;

        float threshold = 0.0001f;
        boolean zeroX = Math.abs(x2 - x1) < threshold;
        boolean zeroY = Math.abs(y2 - y1) < threshold;
        boolean zeroZ = Math.abs(z2 - z1) < threshold;

        Map<CubeData.Face, FaceData> faces = cube.faces();
        PoseStack.Pose pose = poseStack.last();

        if (zeroX) {
            if (faces.containsKey(CubeData.Face.EAST)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.EAST, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x2, y1, z2, x2, y2, z1, 1, 0, 0, emissivePass);
            } else if (faces.containsKey(CubeData.Face.WEST)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.WEST, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y1, z1, x1, y2, z2, -1, 0, 0, emissivePass);
            }
        } else if (zeroY) {
            if (faces.containsKey(CubeData.Face.UP)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.UP, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y2, z1, x2, y2, z2, 0, 1, 0, emissivePass);
            } else if (faces.containsKey(CubeData.Face.DOWN)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.DOWN, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y1, z1, 0, -1, 0, emissivePass);
            }
        } else if (zeroZ) {
            if (faces.containsKey(CubeData.Face.SOUTH)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.SOUTH, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y2, z2, 0, 0, 1, emissivePass);
            } else if (faces.containsKey(CubeData.Face.NORTH)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.NORTH, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x2, y1, z1, x1, y2, z1, 0, 0, -1, emissivePass);
            }
        } else {
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.NORTH, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x2, y1, z1, x1, y2, z1, 0, 0, -1, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.SOUTH, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y2, z2, 0, 0, 1, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.EAST, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x2, y1, z2, x2, y2, z1, 1, 0, 0, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.WEST, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y1, z1, x1, y2, z2, -1, 0, 0, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.UP, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y2, z1, x2, y2, z2, 0, 1, 0, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.DOWN, modelId, textures, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y1, z1, 0, -1, 0, emissivePass);
        }

        poseStack.popPose();
    }

    private void renderFaceFree(BBModel<T> renderModel, T entity, ModelData modelData, CubeData cube, CubeData.Face face, ResourceLocation modelId,
                                 List<TextureData> textures, PoseStack.Pose pose, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay, float defaultTexWidth, float defaultTexHeight,
                                 float x1, float y1, float z1, float x2, float y2, float z2,
                                 float nx, float ny, float nz, boolean emissivePass) {
        FaceData faceData = cube.faces().get(face);
        if (faceData == null) {
            return;
        }

        if (textures.isEmpty()) {
            return;
        }

        int textureIndex = faceData.textureIndex();
        if (textureIndex < 0 || textureIndex >= textures.size()) {
            textureIndex = 0;
        }

        TextureData textureData = textures.get(textureIndex);
        ResourceLocation textureLocation = resolveTextureLocation(renderModel, entity, modelData, modelId, textureIndex, textureData);

        RenderType renderType;
        if (emissivePass) {
            renderType = BBRenderType.entityTranslucentEmissive(textureLocation);
        } else {
            renderType = BBRenderType.entityCutoutNoCull(textureLocation);
        }
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float texWidth = textureData.width() > 0 ? textureData.width() : defaultTexWidth;
        float texHeight = textureData.height() > 0 ? textureData.height() : defaultTexHeight;

        float[] uvs = FaceUvUtil.buildFaceVertexUvs(face, faceData.uv(), faceData.rotation(), texWidth, texHeight);

        switch (face) {
            case NORTH -> {
                vertex(vertexConsumer, pose, x1, y1, z1, uvs[0], uvs[1], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z1, uvs[2], uvs[3], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z1, uvs[4], uvs[5], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z1, uvs[6], uvs[7], nx, ny, nz, packedLight, packedOverlay);
            }
            case SOUTH -> {
                vertex(vertexConsumer, pose, x1, y1, z2, uvs[0], uvs[1], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z2, uvs[2], uvs[3], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z2, uvs[4], uvs[5], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z2, uvs[6], uvs[7], nx, ny, nz, packedLight, packedOverlay);
            }
            case EAST -> {
                vertex(vertexConsumer, pose, x2, y1, z1, uvs[0], uvs[1], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z2, uvs[2], uvs[3], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z2, uvs[4], uvs[5], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z1, uvs[6], uvs[7], nx, ny, nz, packedLight, packedOverlay);
            }
            case WEST -> {
                vertex(vertexConsumer, pose, x1, y1, z2, uvs[0], uvs[1], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y1, z1, uvs[2], uvs[3], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z1, uvs[4], uvs[5], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z2, uvs[6], uvs[7], nx, ny, nz, packedLight, packedOverlay);
            }
            case UP -> {
                vertex(vertexConsumer, pose, x1, y2, z1, uvs[0], uvs[1], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z1, uvs[2], uvs[3], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y2, z2, uvs[4], uvs[5], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y2, z2, uvs[6], uvs[7], nx, ny, nz, packedLight, packedOverlay);
            }
            case DOWN -> {
                vertex(vertexConsumer, pose, x1, y1, z2, uvs[0], uvs[1], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z2, uvs[2], uvs[3], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x2, y1, z1, uvs[4], uvs[5], nx, ny, nz, packedLight, packedOverlay);
                vertex(vertexConsumer, pose, x1, y1, z1, uvs[6], uvs[7], nx, ny, nz, packedLight, packedOverlay);
            }
        }
    }

    // ===== Shared utilities =====

    private void applyBoneTransform(BoneData bone, PoseStack poseStack,
                                    @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                    float animationTime, MolangContext molangContext, double currentTick) {
        Vec3f origin = bone.origin();
        Vec3f rotation = bone.rotation();
        Vec3f position = Vec3f.ZERO;
        Vec3f scale = Vec3f.ONE;

        if (manager != null) {
            Vec3f combinedRotation = Vec3f.ZERO;
            Vec3f combinedPosition = Vec3f.ZERO;
            Vec3f combinedScale = Vec3f.ONE;
            boolean hasControllerAnimation = false;

            for (AnimationController<T> controller : manager.getAnimationControllers().values()) {
                AnimationController.BoneAnimationResult result =
                        controller.computeBoneAnimation(bone.name(), bone.uuid(), molangContext, currentTick);

                if (result == null) {
                    continue;
                }

                hasControllerAnimation = true;
                combinedRotation = combinedRotation.add(result.rotation());
                combinedPosition = combinedPosition.add(result.position());
                combinedScale = multiplyScale(combinedScale, result.scale());

                if (!result.isBlended()) {
                    controller.saveBoneSnapshot(bone.name(), result.rotation(), result.position(), result.scale());
                }
            }

            if (hasControllerAnimation) {
                rotation = rotation.add(combinedRotation);
                position = combinedPosition;
                scale = combinedScale;
            }
        } else if (animation != null) {
            BBBoneAnimator boneAnimator = animation.getBoneAnimator(bone.name(), bone.uuid());
            if (boneAnimator != null) {
                Vec3f animRotation = boneAnimator.getRotationAt(animationTime, molangContext);
                Vec3f animPosition = boneAnimator.getPositionAt(animationTime, molangContext);
                Vec3f animScale = boneAnimator.getScaleAt(animationTime, molangContext);

                rotation = new Vec3f(
                        rotation.x() + (animRotation != null ? animRotation.x() : 0.0f),
                        rotation.y() + (animRotation != null ? animRotation.y() : 0.0f),
                        rotation.z() + (animRotation != null ? animRotation.z() : 0.0f)
                );
                position = animPosition != null ? animPosition : Vec3f.ZERO;
                scale = animScale != null ? animScale : Vec3f.ONE;
            }
        }

        poseStack.translate(
                (origin.x() + position.x()) * PIXEL_SCALE,
                (origin.y() + position.y()) * PIXEL_SCALE,
                (origin.z() + position.z()) * PIXEL_SCALE
        );

        if (rotation.z() != 0) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
        if (rotation.y() != 0) poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
        if (rotation.x() != 0) poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));

        if (scale.x() != 1 || scale.y() != 1 || scale.z() != 1) {
            poseStack.scale(scale.x(), scale.y(), scale.z());
        }

        poseStack.translate(-origin.x() * PIXEL_SCALE, -origin.y() * PIXEL_SCALE, -origin.z() * PIXEL_SCALE);
    }

    private ResourceLocation resolveTextureLocation(BBModel<T> renderModel, T entity, ModelData modelData,
                                                    ResourceLocation modelId, int textureIndex, TextureData textureData) {
        ResourceLocation overrideTexture = renderModel.getTextureResource(entity, textureIndex, textureData);
        if (overrideTexture != null) {
            return overrideTexture;
        }

        return BBLibApi.createEmbeddedTexture(modelId, textureIndex, textureData);
    }

    private Vec3f multiplyScale(Vec3f left, Vec3f right) {
        return new Vec3f(left.x() * right.x(), left.y() * right.y(), left.z() * right.z());
    }

    private void renderMissingModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        ResourceLocation missingTexture = MissingModelTexture.getLocation();
        RenderType renderType = BBRenderType.entityCutoutNoCull(missingTexture);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float size = 0.5f;
        PoseStack.Pose pose = poseStack.last();
        int overlay = OverlayTexture.NO_OVERLAY;

        // North
        vertex(vertexConsumer, pose, -size, 0, -size, 0, 1, 0, 0, -1, packedLight, overlay);
        vertex(vertexConsumer, pose, size, 0, -size, 1, 1, 0, 0, -1, packedLight, overlay);
        vertex(vertexConsumer, pose, size, size * 2, -size, 1, 0, 0, 0, -1, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, size * 2, -size, 0, 0, 0, 0, -1, packedLight, overlay);

        // South
        vertex(vertexConsumer, pose, -size, 0, size, 0, 1, 0, 0, 1, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, size * 2, size, 0, 0, 0, 0, 1, packedLight, overlay);
        vertex(vertexConsumer, pose, size, size * 2, size, 1, 0, 0, 0, 1, packedLight, overlay);
        vertex(vertexConsumer, pose, size, 0, size, 1, 1, 0, 0, 1, packedLight, overlay);

        // East
        vertex(vertexConsumer, pose, size, 0, -size, 0, 1, 1, 0, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, 0, size, 1, 1, 1, 0, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, size * 2, size, 1, 0, 1, 0, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, size * 2, -size, 0, 0, 1, 0, 0, packedLight, overlay);

        // West
        vertex(vertexConsumer, pose, -size, 0, size, 0, 1, -1, 0, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, 0, -size, 1, 1, -1, 0, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, size * 2, -size, 1, 0, -1, 0, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, size * 2, size, 0, 0, -1, 0, 0, packedLight, overlay);

        // Up
        vertex(vertexConsumer, pose, -size, size * 2, -size, 0, 0, 0, 1, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, size * 2, -size, 1, 0, 0, 1, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, size * 2, size, 1, 1, 0, 1, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, size * 2, size, 0, 1, 0, 1, 0, packedLight, overlay);

        // Down
        vertex(vertexConsumer, pose, -size, 0, size, 0, 1, 0, -1, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, 0, size, 1, 1, 0, -1, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, size, 0, -size, 1, 0, 0, -1, 0, packedLight, overlay);
        vertex(vertexConsumer, pose, -size, 0, -size, 0, 0, 0, -1, 0, packedLight, overlay);

        poseStack.popPose();
    }

    private void vertex(VertexConsumer vertexConsumer, PoseStack.Pose pose,
                         float x, float y, float z, float u, float v,
                         float nx, float ny, float nz, int packedLight, int packedOverlay) {
        vertexConsumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }
}

