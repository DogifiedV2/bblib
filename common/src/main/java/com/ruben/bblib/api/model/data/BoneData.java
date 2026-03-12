package com.ruben.bblib.api.model.data;

import java.util.List;

public record BoneData(
        String uuid,
        String name,
        Vec3f origin,
        Vec3f rotation,
        List<String> cubeUuids,
        List<String> billboardUuids,
        List<BoneData> children,
        ModelNodeKind kind
) {

    public BoneData(String uuid, String name, Vec3f origin, Vec3f rotation,
                    List<String> cubeUuids, List<BoneData> children) {
        this(uuid, name, origin, rotation, cubeUuids, List.of(), children, ModelNodeKind.BONE);
    }
}

