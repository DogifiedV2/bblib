package com.ruben.bblib.api.molang;

import java.util.HashMap;
import java.util.Map;

public class MolangContext {

    private float animTime;
    private float partialTick;
    private EntityContext entityContext;
    private final Map<String, MolangValue> queries;

    public MolangContext() {
        this.animTime = 0;
        this.partialTick = 0;
        this.entityContext = EntityContext.EMPTY;
        this.queries = new HashMap<>();
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

    public MolangContext withQuery(String name, double value) {
        return withQuery(name, ctx -> value);
    }

    public MolangContext withQuery(String name, MolangValue value) {
        queries.put(normalizeQueryName(name), value);
        return this;
    }

    public MolangContext withoutQuery(String name) {
        queries.remove(normalizeQueryName(name));
        return this;
    }

    public boolean hasQuery(String name) {
        return queries.containsKey(normalizeQueryName(name));
    }

    public MolangValue getQueryValue(String name) {
        return queries.get(normalizeQueryName(name));
    }

    public MolangContext copy() {
        MolangContext copy = new MolangContext()
                .withAnimTime(animTime)
                .withPartialTick(partialTick)
                .withEntityContext(entityContext);
        copy.queries.putAll(queries);
        return copy;
    }

    public static String normalizeQueryName(String name) {
        return name.trim().toLowerCase().replace("query.", "q.");
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

