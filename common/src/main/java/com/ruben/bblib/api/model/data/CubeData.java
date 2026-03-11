package com.ruben.bblib.api.model.data;

import java.util.Map;

public record CubeData(
        String uuid,
        String name,
        boolean visible,
        Vec3f from,
        Vec3f to,
        Vec3f origin,
        Vec3f rotation,
        float inflate,
        Map<Face, FaceData> faces
) {

    public enum Face {
        NORTH, EAST, SOUTH, WEST, UP, DOWN
    }
}

