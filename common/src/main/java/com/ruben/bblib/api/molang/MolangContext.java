package com.ruben.bblib.api.molang;

public class MolangContext {

    private float animTime;
    private float partialTick;
    private EntityContext entityContext;

    public MolangContext() {
        this.animTime = 0;
        this.partialTick = 0;
        this.entityContext = EntityContext.EMPTY;
    }

    public MolangContext withAnimTime(float animTime) {
        this.animTime = animTime;
        return this;
    }

    public MolangContext withPartialTick(float partialTick) {
        this.partialTick = partialTick;
        return this;
    }

    public MolangContext withEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
        return this;
    }

    public float getAnimTime() {
        return animTime;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public EntityContext getEntityContext() {
        return entityContext;
    }

    public double getHealth() {
        return entityContext.getHealth();
    }

    public double getMaxHealth() {
        return entityContext.getMaxHealth();
    }

    public double getGroundSpeed() {
        return entityContext.getGroundSpeed();
    }

    public double getVerticalSpeed() {
        return entityContext.getVerticalSpeed();
    }

    public boolean isOnGround() {
        return entityContext.isOnGround();
    }

    public boolean isInWater() {
        return entityContext.isInWater();
    }

    public boolean isMoving() {
        return entityContext.isMoving();
    }

    public double getHurtTime() {
        return entityContext.getHurtTime();
    }

    public double getDeathTime() {
        return entityContext.getDeathTime();
    }
}

