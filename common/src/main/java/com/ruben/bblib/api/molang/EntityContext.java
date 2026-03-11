package com.ruben.bblib.api.molang;

public interface EntityContext {

    double getHealth();

    double getMaxHealth();

    double getGroundSpeed();

    double getVerticalSpeed();

    boolean isOnGround();

    boolean isInWater();

    boolean isMoving();

    double getHurtTime();

    double getDeathTime();

    EntityContext EMPTY = new EntityContext() {
        @Override public double getHealth() { return 0; }
        @Override public double getMaxHealth() { return 0; }
        @Override public double getGroundSpeed() { return 0; }
        @Override public double getVerticalSpeed() { return 0; }
        @Override public boolean isOnGround() { return false; }
        @Override public boolean isInWater() { return false; }
        @Override public boolean isMoving() { return false; }
        @Override public double getHurtTime() { return 0; }
        @Override public double getDeathTime() { return 0; }
    };
}

