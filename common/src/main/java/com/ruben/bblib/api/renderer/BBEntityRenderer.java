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
import com.ruben.bblib.api.animatable.data.DataTickets;
import com.ruben.bblib.api.animatable.data.EntityRenderData;
import com.ruben.bblib.api.model.BBModel;
import com.ruben.bblib.api.model.animation.BoneRenderState;
import com.ruben.bblib.api.model.animation.BoneRenderStateMap;
import com.ruben.bblib.api.model.data.BillboardData;
import com.ruben.bblib.api.model.data.BoneData;
import com.ruben.bblib.api.model.data.CubeData;
import com.ruben.bblib.api.model.data.FaceData;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.TextureData;
import com.ruben.bblib.api.model.data.UV;
import com.ruben.bblib.api.model.data.Vec3f;
import com.ruben.bblib.api.model.transform.ResolvedNodeTransform;
import com.ruben.bblib.internal.client.texture.MissingModelTexture;
import com.ruben.bblib.internal.model.transform.BBModelTransformResolver;
import com.ruben.bblib.internal.renderer.BBRenderType;
import com.ruben.bblib.api.molang.EntityContext;
import com.ruben.bblib.api.molang.MolangContext;
import com.ruben.bblib.api.renderer.layer.BBRenderContext;
import com.ruben.bblib.api.renderer.layer.BBRenderLayer;
import com.ruben.bblib.api.renderer.layer.BBRenderLayersContainer;
import com.ruben.bblib.internal.renderer.FaceUvUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class BBEntityRenderer<T extends Entity & BBAnimatable> extends EntityRenderer<T> {

    private static final float PIXEL_SCALE = 1.0f / 16.0f;

    protected final BBModel<T> model;
    private final BBRenderLayersContainer<T> renderLayers = new BBRenderLayersContainer<>();

    public BBEntityRenderer(EntityRendererProvider.Context context, BBModel<T> model) {
        super(context);
        this.model = model;
    }

    public BBModel<T> getModel() {
        return model;
    }

    @Nullable
    public ResolvedNodeTransform getLocatorTransform(T entity, ModelData modelData, String locatorName,
                                                     AnimatableManager<T> manager, MolangContext molangContext,
                                                     double currentTick, float partialTick,
                                                     @Nullable BoneRenderStateMap boneRenderStates) {
        return BBModelTransformResolver.resolveLocatorTransform(
                entity, modelData, locatorName, manager, molangContext, currentTick, partialTick, boneRenderStates
        );
    }

    @Nullable
    public ResolvedNodeTransform getBoneTransform(T entity, ModelData modelData, String boneName,
                                                  AnimatableManager<T> manager, MolangContext molangContext,
                                                  double currentTick, float partialTick,
                                                  @Nullable BoneRenderStateMap boneRenderStates) {
        return BBModelTransformResolver.resolveBoneTransform(
                entity, modelData, boneName, manager, molangContext, currentTick, partialTick, boneRenderStates
        );
    }

    protected final void addRenderLayer(BBRenderLayer<T> renderLayer) {
        renderLayers.addLayer(renderLayer);
    }

    public List<BBRenderLayer<T>> getRenderLayers() {
        return renderLayers.getRenderLayers();
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
        AnimationState<T> animationState = createAnimationState(entity, modelData, manager, partialTick, isMoving, currentTick);
        double renderTick = currentTick - manager.getFirstTickTime();
        animationState.animationTick = renderTick;

        model.addAdditionalStateData(entity, animationState);

        for (AnimationController<T> controller : manager.getAnimationControllers().values()) {
            animationState.animationTick = renderTick;
            controller.process(animationState, modelData, currentTick);
        }

        manager.updatedAt(currentTick);
        if (manager.isFirstTick()) {
            manager.finishFirstTick();
        }

        MolangContext molangContext = buildMolangContext(entity, (float) renderTick, partialTick);
        animationState.setData(DataTickets.MOLANG_CONTEXT, molangContext);
        model.applyMolangQueries(entity, animationState, molangContext);

        BoneRenderStateMap boneRenderStates = BoneRenderStateMap.create(modelData);
        model.setCustomAnimations(entity, animationState, boneRenderStates);

        BBRenderContext<T> renderContext = new BBRenderContext<>(
                this, model, entity, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                manager, animationState, boneRenderStates, molangContext, partialTick, (float) renderTick, currentTick
        );

        for (BBRenderLayer<T> renderLayer : renderLayers.getRenderLayers()) {
            renderLayer.preRender(renderContext);
        }

        renderResolvedModel(model, renderContext, model.getModelResource(entity), manager, null, true);
        renderPostModelExtras(entity, poseStack, bufferSource, packedLight, packedOverlay,
                (float) renderTick, molangContext, currentTick);

        for (BBRenderLayer<T> renderLayer : renderLayers.getRenderLayers()) {
            renderLayer.render(renderContext);
        }

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

    private AnimationState<T> createAnimationState(T entity, ModelData modelData, AnimatableManager<T> manager,
                                                   float partialTick, boolean isMoving, double currentTick) {
        AnimationState<T> animationState = new AnimationState<>(entity, partialTick, isMoving);
        manager.applyDataToState(animationState);
        animationState.setData(DataTickets.TICK, currentTick);
        animationState.setData(DataTickets.PARTIAL_TICK, partialTick);
        animationState.setData(DataTickets.MOVING, isMoving);
        animationState.setData(DataTickets.MODEL_DATA, modelData);
        animationState.setData(DataTickets.ENTITY, entity);
        animationState.setData(DataTickets.ENTITY_RENDER_DATA, createEntityRenderData(entity, partialTick));
        return animationState;
    }

    private EntityRenderData createEntityRenderData(T entity, float partialTick) {
        float bodyYaw = entity instanceof LivingEntity living
                ? Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot)
                : Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float headYaw = entity instanceof LivingEntity living
                ? Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot)
                : bodyYaw;
        float headPitch = entity instanceof LivingEntity living
                ? Mth.lerp(partialTick, living.xRotO, living.getXRot())
                : Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        return new EntityRenderData(bodyYaw, headYaw, headPitch);
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
        BBRenderContext<T> renderContext = new BBRenderContext<>(
                this, additionalModel, entity, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                entity.getAnimatableInstanceCache().getManagerForId(entity.getId()),
                new AnimationState<>(entity, 0, false), BoneRenderStateMap.create(modelData),
                molangContext, 0, animationTime, currentTick
        );
        renderResolvedModel(additionalModel, renderContext, additionalModel.getModelResource(entity), null, animation, false);
    }

    private void renderResolvedModel(BBModel<T> renderModel, BBRenderContext<T> renderContext, ResourceLocation modelId,
                                     @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                     boolean fireLayerCallbacks) {
        ModelData modelData = renderContext.getModelData();
        if (modelData.freeFormat()) {
            renderFreeFormat(renderModel, renderContext, modelId, manager, animation, fireLayerCallbacks);
        } else {
            renderModdedFormat(renderModel, renderContext, modelId, manager, animation, fireLayerCallbacks);
        }
    }

    // ===== Modded format rendering =====

    private void renderModdedFormat(BBModel<T> renderModel, BBRenderContext<T> renderContext, ResourceLocation modelId,
                                    @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                    boolean fireLayerCallbacks) {
        T entity = renderContext.getEntity();
        ModelData modelData = renderContext.getModelData();
        PoseStack poseStack = renderContext.getPoseStack();
        MultiBufferSource bufferSource = renderContext.getBufferSource();
        int packedLight = renderContext.getPackedLight();
        int packedOverlay = renderContext.getPackedOverlay();
        ResolvedTexture primaryTexture = resolveRenderableTexture(renderModel, entity, modelData, modelId, 0, false);
        if (primaryTexture == null) {
            return;
        }

        RenderType renderType = BBRenderType.entityCutoutNoCull(primaryTexture.location());
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float texWidth = primaryTexture.uvWidth() > 0 ? primaryTexture.uvWidth() : modelData.textureWidth();
        float texHeight = primaryTexture.uvHeight() > 0 ? primaryTexture.uvHeight() : modelData.textureHeight();

        for (BoneData bone : modelData.rootBones()) {
            renderBoneModded(renderModel, renderContext, bone, modelId, vertexConsumer,
                    texWidth, texHeight, manager, animation, false, fireLayerCallbacks);
        }

        int glowmaskIndex = modelData.findGlowmaskTextureIndex(0);
        if (glowmaskIndex < 0) {
            return;
        }

        ResolvedTexture glowmaskTexture = resolveRenderableTexture(renderModel, entity, modelData, modelId, 0, true);
        if (glowmaskTexture == null) {
            return;
        }

        RenderType emissiveRenderType = BBRenderType.entityTranslucentEmissive(glowmaskTexture.location());
        VertexConsumer emissiveConsumer = bufferSource.getBuffer(emissiveRenderType);

        float glowmaskWidth = glowmaskTexture.uvWidth() > 0 ? glowmaskTexture.uvWidth() : texWidth;
        float glowmaskHeight = glowmaskTexture.uvHeight() > 0 ? glowmaskTexture.uvHeight() : texHeight;

        for (BoneData bone : modelData.rootBones()) {
            renderBoneModded(renderModel, renderContext, bone, modelId, emissiveConsumer,
                    glowmaskWidth, glowmaskHeight, manager, animation, true,
                    LightTexture.FULL_BRIGHT, packedOverlay, fireLayerCallbacks);
        }
    }

    private void renderBoneModded(BBModel<T> renderModel, BBRenderContext<T> renderContext, BoneData bone, ResourceLocation modelId,
                                  VertexConsumer vertexConsumer, float texWidth, float texHeight,
                                  @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                  boolean emissivePass, boolean fireLayerCallbacks) {
        renderBoneModded(renderModel, renderContext, bone, modelId, vertexConsumer, texWidth, texHeight,
                manager, animation, emissivePass, renderContext.getPackedLight(), renderContext.getPackedOverlay(),
                fireLayerCallbacks);
    }

    private void renderBoneModded(BBModel<T> renderModel, BBRenderContext<T> renderContext, BoneData bone, ResourceLocation modelId,
                                  VertexConsumer vertexConsumer, float texWidth, float texHeight,
                                  @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                  boolean emissivePass, int packedLight, int packedOverlay, boolean fireLayerCallbacks) {
        T entity = renderContext.getEntity();
        ModelData modelData = renderContext.getModelData();
        PoseStack poseStack = renderContext.getPoseStack();
        MultiBufferSource bufferSource = renderContext.getBufferSource();

        if (!shouldRenderBone(entity, bone.name(), renderContext.getBoneRenderState(bone.name()))) {
            return;
        }

        poseStack.pushPose();

        applyBoneTransform(bone, poseStack, renderContext, manager, animation);

        if (!emissivePass) {
            renderBoneAttachments(entity, bone, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                    renderContext.getAnimationTime(), renderContext.getMolangContext(), renderContext.getCurrentTick());
        }

        Map<String, CubeData> cubes = modelData.cubes();

        for (String cubeUuid : bone.cubeUuids()) {
            CubeData cube = cubes.get(cubeUuid);
            if (cube != null) {
                renderCubeModded(cube, poseStack, vertexConsumer, packedLight, packedOverlay, texWidth, texHeight);
            }
        }

        for (BoneData child : bone.children()) {
            renderBoneModded(renderModel, renderContext, child, modelId, vertexConsumer,
                    texWidth, texHeight, manager, animation, emissivePass, packedLight, packedOverlay, fireLayerCallbacks);
        }

        if (!emissivePass && fireLayerCallbacks) {
            for (BBRenderLayer<T> renderLayer : renderLayers.getRenderLayers()) {
                renderLayer.renderForBone(renderContext, bone);
            }
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

    private void renderFreeFormat(BBModel<T> renderModel, BBRenderContext<T> renderContext, ResourceLocation modelId,
                                  @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                  boolean fireLayerCallbacks) {
        ModelData modelData = renderContext.getModelData();
        for (BoneData bone : modelData.rootBones()) {
            renderBoneFree(renderModel, renderContext, bone, modelId,
                    manager, animation, false, renderContext.getPackedLight(), renderContext.getPackedOverlay(),
                    fireLayerCallbacks);
        }

        if (!hasAnyGlowmaskTexture(modelData)) {
            return;
        }

        for (BoneData bone : modelData.rootBones()) {
            renderBoneFree(renderModel, renderContext, bone, modelId,
                    manager, animation, true, LightTexture.FULL_BRIGHT, renderContext.getPackedOverlay(),
                    fireLayerCallbacks);
        }
    }

    private void renderBoneFree(BBModel<T> renderModel, BBRenderContext<T> renderContext, BoneData bone, ResourceLocation modelId,
                                @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation,
                                boolean emissivePass, int packedLight, int packedOverlay, boolean fireLayerCallbacks) {
        T entity = renderContext.getEntity();
        ModelData modelData = renderContext.getModelData();
        PoseStack poseStack = renderContext.getPoseStack();
        MultiBufferSource bufferSource = renderContext.getBufferSource();

        if (!shouldRenderBone(entity, bone.name(), renderContext.getBoneRenderState(bone.name()))) {
            return;
        }

        poseStack.pushPose();

        applyBoneTransform(bone, poseStack, renderContext, manager, animation);

        if (!emissivePass) {
            renderBoneAttachments(entity, bone, modelData, poseStack, bufferSource, packedLight, packedOverlay,
                    renderContext.getAnimationTime(), renderContext.getMolangContext(), renderContext.getCurrentTick());
        }

        Map<String, CubeData> cubes = modelData.cubes();
        Map<String, BillboardData> billboards = modelData.billboards();
        float texWidth = modelData.textureWidth();
        float texHeight = modelData.textureHeight();

        for (String cubeUuid : bone.cubeUuids()) {
            CubeData cube = cubes.get(cubeUuid);
            if (cube != null) {
                renderCubeFree(renderModel, entity, modelData, cube, modelId, poseStack, bufferSource,
                        packedLight, packedOverlay, texWidth, texHeight, emissivePass);
            }
        }

        for (String billboardUuid : bone.billboardUuids()) {
            BillboardData billboard = billboards.get(billboardUuid);
            if (billboard != null) {
                renderBillboardFree(renderModel, entity, modelData, billboard, modelId, poseStack, bufferSource,
                        packedLight, packedOverlay, texWidth, texHeight, emissivePass);
            }
        }

        for (BoneData child : bone.children()) {
            renderBoneFree(renderModel, renderContext, child, modelId, manager, animation, emissivePass,
                    packedLight, packedOverlay, fireLayerCallbacks);
        }

        if (!emissivePass && fireLayerCallbacks) {
            for (BBRenderLayer<T> renderLayer : renderLayers.getRenderLayers()) {
                renderLayer.renderForBone(renderContext, bone);
            }
        }

        poseStack.popPose();
    }

    private void renderCubeFree(BBModel<T> renderModel, T entity, ModelData modelData, CubeData cube,
                                ResourceLocation modelId,
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
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.EAST, modelId, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x2, y1, z2, x2, y2, z1, 1, 0, 0, emissivePass);
            } else if (faces.containsKey(CubeData.Face.WEST)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.WEST, modelId, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y1, z1, x1, y2, z2, -1, 0, 0, emissivePass);
            }
        } else if (zeroY) {
            if (faces.containsKey(CubeData.Face.UP)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.UP, modelId, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y2, z1, x2, y2, z2, 0, 1, 0, emissivePass);
            } else if (faces.containsKey(CubeData.Face.DOWN)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.DOWN, modelId, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y1, z1, 0, -1, 0, emissivePass);
            }
        } else if (zeroZ) {
            if (faces.containsKey(CubeData.Face.SOUTH)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.SOUTH, modelId, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y2, z2, 0, 0, 1, emissivePass);
            } else if (faces.containsKey(CubeData.Face.NORTH)) {
                renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.NORTH, modelId, pose, bufferSource, packedLight, packedOverlay,
                        defaultTexWidth, defaultTexHeight, x2, y1, z1, x1, y2, z1, 0, 0, -1, emissivePass);
            }
        } else {
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.NORTH, modelId, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x2, y1, z1, x1, y2, z1, 0, 0, -1, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.SOUTH, modelId, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y2, z2, 0, 0, 1, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.EAST, modelId, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x2, y1, z2, x2, y2, z1, 1, 0, 0, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.WEST, modelId, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y1, z1, x1, y2, z2, -1, 0, 0, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.UP, modelId, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y2, z1, x2, y2, z2, 0, 1, 0, emissivePass);
            renderFaceFree(renderModel, entity, modelData, cube, CubeData.Face.DOWN, modelId, pose, bufferSource, packedLight, packedOverlay,
                    defaultTexWidth, defaultTexHeight, x1, y1, z2, x2, y1, z1, 0, -1, 0, emissivePass);
        }

        poseStack.popPose();
    }

    private void renderFaceFree(BBModel<T> renderModel, T entity, ModelData modelData, CubeData cube,
                                CubeData.Face face, ResourceLocation modelId, PoseStack.Pose pose,
                                MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                float defaultTexWidth, float defaultTexHeight, float x1, float y1, float z1,
                                float x2, float y2, float z2, float nx, float ny, float nz,
                                boolean emissivePass) {
        FaceData faceData = cube.faces().get(face);
        if (faceData == null) {
            return;
        }

        ResolvedTexture resolvedTexture = resolveRenderableTexture(renderModel, entity, modelData, modelId,
                faceData.textureIndex(), emissivePass);
        if (resolvedTexture == null) {
            return;
        }

        RenderType renderType;
        if (emissivePass) {
            renderType = BBRenderType.entityTranslucentEmissive(resolvedTexture.location());
        } else {
            renderType = BBRenderType.entityCutoutNoCull(resolvedTexture.location());
        }
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float texWidth = resolvedTexture.uvWidth() > 0 ? resolvedTexture.uvWidth() : defaultTexWidth;
        float texHeight = resolvedTexture.uvHeight() > 0 ? resolvedTexture.uvHeight() : defaultTexHeight;

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

    private void renderBillboardFree(BBModel<T> renderModel, T entity, ModelData modelData, BillboardData billboard,
                                     ResourceLocation modelId, PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay, float defaultTexWidth,
                                     float defaultTexHeight, boolean emissivePass) {
        if (!billboard.visible() || billboard.frontFace() == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                billboard.origin().x() * PIXEL_SCALE,
                billboard.origin().y() * PIXEL_SCALE,
                billboard.origin().z() * PIXEL_SCALE
        );

        applyBillboardFacing(entity, poseStack, billboard);

        FaceData faceData = billboard.frontFace();
        ResolvedTexture resolvedTexture = resolveRenderableTexture(renderModel, entity, modelData, modelId,
                faceData.textureIndex(), emissivePass);
        if (resolvedTexture == null) {
            poseStack.popPose();
            return;
        }

        RenderType renderType = emissivePass
                ? BBRenderType.entityTranslucentEmissive(resolvedTexture.location())
                : BBRenderType.entityCutoutNoCull(resolvedTexture.location());
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        float texWidth = resolvedTexture.uvWidth() > 0 ? resolvedTexture.uvWidth() : defaultTexWidth;
        float texHeight = resolvedTexture.uvHeight() > 0 ? resolvedTexture.uvHeight() : defaultTexHeight;
        float[] uvs = FaceUvUtil.buildFaceVertexUvs(CubeData.Face.SOUTH, faceData.uv(), faceData.rotation(), texWidth, texHeight);

        float halfWidth = billboard.size().x() * 0.5f * PIXEL_SCALE;
        float halfHeight = billboard.size().y() * 0.5f * PIXEL_SCALE;
        float offsetX = billboard.offset().x() * PIXEL_SCALE;
        float offsetY = billboard.offset().y() * PIXEL_SCALE;
        float left = offsetX - halfWidth;
        float right = offsetX + halfWidth;
        float bottom = offsetY - halfHeight;
        float top = offsetY + halfHeight;
        PoseStack.Pose pose = poseStack.last();

        vertex(vertexConsumer, pose, left, bottom, 0.0f, uvs[0], uvs[1], 0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
        vertex(vertexConsumer, pose, left, top, 0.0f, uvs[2], uvs[3], 0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
        vertex(vertexConsumer, pose, right, top, 0.0f, uvs[4], uvs[5], 0.0f, 0.0f, 1.0f, packedLight, packedOverlay);
        vertex(vertexConsumer, pose, right, bottom, 0.0f, uvs[6], uvs[7], 0.0f, 0.0f, 1.0f, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void applyBillboardFacing(T entity, PoseStack poseStack, BillboardData billboard) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null || minecraft.gameRenderer.getMainCamera() == null) {
            return;
        }

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 relativeCameraPosition = cameraPosition.subtract(entity.getX(), entity.getY(), entity.getZ());

        Matrix4f inversePose = new Matrix4f(poseStack.last().pose());
        if (Math.abs(inversePose.determinant()) < 1.0E-6f) {
            return;
        }
        inversePose.invert();

        Vector4f localCamera = new Vector4f(
                (float) relativeCameraPosition.x,
                (float) relativeCameraPosition.y,
                (float) relativeCameraPosition.z,
                1.0f
        );
        inversePose.transform(localCamera);

        float horizontalDistance = Mth.sqrt(localCamera.x * localCamera.x + localCamera.z * localCamera.z);
        float yaw = (float) Math.toDegrees(Math.atan2(localCamera.x, localCamera.z));
        float pitch = horizontalDistance < 0.0001f
                ? (localCamera.y >= 0 ? -90.0f : 90.0f)
                : (float) -Math.toDegrees(Math.atan2(localCamera.y, horizontalDistance));

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (!billboard.facingMode().yOnly()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        }
    }

    // ===== Shared utilities =====

    private void applyBoneTransform(BoneData bone, PoseStack poseStack, BBRenderContext<T> renderContext,
                                    @Nullable AnimatableManager<T> manager, @Nullable BBAnimation animation) {
        Vec3f origin = bone.origin();
        Vec3f rotation = bone.rotation();
        Vec3f position = Vec3f.ZERO;
        Vec3f scale = Vec3f.ONE;
        float animationTime = renderContext.getAnimationTime();
        MolangContext molangContext = renderContext.getMolangContext();
        double currentTick = renderContext.getCurrentTick();

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

        BoneRenderState boneRenderState = renderContext.getBoneRenderState(bone.name());
        if (boneRenderState != null) {
            rotation = rotation.add(boneRenderState.getRotationOffset());
            position = position.add(boneRenderState.getPositionOffset());
            scale = multiplyScale(scale, boneRenderState.getScaleMultiplier());
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

        if (textureData.isAnimated()) {
            TextureData.AnimationFrame animationFrame = textureData.resolveAnimationFrame(
                    getTextureAnimationTick(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false))
            );
            return BBLibApi.createEmbeddedTexture(modelId, textureIndex, textureData, animationFrame);
        }

        return BBLibApi.createEmbeddedTexture(modelId, textureIndex, textureData);
    }

    @Nullable
    private ResolvedTexture resolveRenderableTexture(BBModel<T> renderModel, T entity, ModelData modelData,
                                                     ResourceLocation modelId, int textureIndex,
                                                     boolean emissivePass) {
        List<TextureData> textures = modelData.textures();
        if (textures.isEmpty()) {
            return null;
        }

        int resolvedTextureIndex = textureIndex;
        if (resolvedTextureIndex < 0) {
            return null;
        }
        if (resolvedTextureIndex >= textures.size()) {
            resolvedTextureIndex = 0;
        }

        if (emissivePass) {
            resolvedTextureIndex = modelData.findGlowmaskTextureIndex(resolvedTextureIndex);
            if (resolvedTextureIndex < 0) {
                return null;
            }
        }

        TextureData textureData = textures.get(resolvedTextureIndex);
        ResourceLocation textureLocation = resolveTextureLocation(renderModel, entity, modelData, modelId,
                resolvedTextureIndex, textureData);
        return new ResolvedTexture(
                resolvedTextureIndex,
                textureData,
                textureLocation,
                textureData.uvWidthOrDefault(),
                textureData.uvHeightOrDefault()
        );
    }

    private double getTextureAnimationTick(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return partialTick;
        }
        return minecraft.level.getGameTime() + partialTick;
    }

    private boolean hasAnyGlowmaskTexture(ModelData modelData) {
        for (int i = 0; i < modelData.textures().size(); i++) {
            if (modelData.findGlowmaskTextureIndex(i) >= 0) {
                return true;
            }
        }
        return false;
    }

    private Vec3f multiplyScale(Vec3f left, Vec3f right) {
        return new Vec3f(left.x() * right.x(), left.y() * right.y(), left.z() * right.z());
    }

    private boolean shouldRenderBone(T entity, String boneName, @Nullable BoneRenderState boneRenderState) {
        boolean defaultVisible = !entity.isBoneHidden(boneName);
        return boneRenderState == null || boneRenderState.isVisible(defaultVisible);
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

    private record ResolvedTexture(int textureIndex, TextureData textureData, ResourceLocation location,
                                   float uvWidth, float uvHeight) {
    }
}

