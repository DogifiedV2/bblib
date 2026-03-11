package com.ruben.bblib.api.model;

public record Vec3f(float x, float y, float z) {

    public static final Vec3f ZERO = new Vec3f(0, 0, 0);
    public static final Vec3f ONE = new Vec3f(1, 1, 1);

    public Vec3f add(Vec3f other) {
        return new Vec3f(x + other.x, y + other.y, z + other.z);
    }

    public Vec3f subtract(Vec3f other) {
        return new Vec3f(x - other.x, y - other.y, z - other.z);
    }

    public Vec3f scale(float factor) {
        return new Vec3f(x * factor, y * factor, z * factor);
    }
}

