package com.ruben.bblib.api.model.animation;

import com.ruben.bblib.api.model.data.Vec3f;
import org.jetbrains.annotations.Nullable;

public final class BoneRenderState {

    private final String boneName;
    private Vec3f rotationOffset = Vec3f.ZERO;
    private Vec3f positionOffset = Vec3f.ZERO;
    private Vec3f scaleMultiplier = Vec3f.ONE;
    private Boolean visibleOverride;

    public BoneRenderState(String boneName) {
        this.boneName = boneName;
    }

    public String getBoneName() {
        return boneName;
    }

    public Vec3f getRotationOffset() {
        return rotationOffset;
    }

    public BoneRenderState setRotationOffset(Vec3f rotationOffset) {
        this.rotationOffset = rotationOffset;
        return this;
    }

    public BoneRenderState addRotation(Vec3f rotationOffset) {
        this.rotationOffset = this.rotationOffset.add(rotationOffset);
        return this;
    }

    public BoneRenderState addRotation(float x, float y, float z) {
        return addRotation(new Vec3f(x, y, z));
    }

    public Vec3f getPositionOffset() {
        return positionOffset;
    }

    public BoneRenderState setPositionOffset(Vec3f positionOffset) {
        this.positionOffset = positionOffset;
        return this;
    }

    public BoneRenderState addPosition(Vec3f positionOffset) {
        this.positionOffset = this.positionOffset.add(positionOffset);
        return this;
    }

    public BoneRenderState addPosition(float x, float y, float z) {
        return addPosition(new Vec3f(x, y, z));
    }

    public Vec3f getScaleMultiplier() {
        return scaleMultiplier;
    }

    public BoneRenderState setScaleMultiplier(Vec3f scaleMultiplier) {
        this.scaleMultiplier = scaleMultiplier;
        return this;
    }

    public BoneRenderState multiplyScale(Vec3f scaleMultiplier) {
        this.scaleMultiplier = new Vec3f(
                this.scaleMultiplier.x() * scaleMultiplier.x(),
                this.scaleMultiplier.y() * scaleMultiplier.y(),
                this.scaleMultiplier.z() * scaleMultiplier.z()
        );
        return this;
    }

    public BoneRenderState multiplyScale(float x, float y, float z) {
        return multiplyScale(new Vec3f(x, y, z));
    }

    public BoneRenderState show() {
        this.visibleOverride = Boolean.TRUE;
        return this;
    }

    public BoneRenderState hide() {
        this.visibleOverride = Boolean.FALSE;
        return this;
    }

    public BoneRenderState clearVisibilityOverride() {
        this.visibleOverride = null;
        return this;
    }

    @Nullable
    public Boolean getVisibleOverride() {
        return visibleOverride;
    }

    public boolean isVisible(boolean defaultVisible) {
        return visibleOverride != null ? visibleOverride : defaultVisible;
    }
}
