package com.ruben.bblib.api.model.data;

import java.util.List;

public record BoneData(
        String uuid,
        String name,
        Vec3f origin,
        Vec3f rotation,
        List<String> cubeUuids,
        List<BoneData> children
) {
}

