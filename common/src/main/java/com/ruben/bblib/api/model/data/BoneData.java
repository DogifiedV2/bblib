package com.ruben.bblib.api.model.data;

import java.util.List;

public record BoneData(
        String uuid,
        String name,
        Vec3f origin,
        Vec3f rotation,
        List<String> cubeUuids,
        List<BoneData> children,
        ModelNodeKind kind
) {

    public BoneData(String uuid, String name, Vec3f origin, Vec3f rotation,
                    List<String> cubeUuids, List<BoneData> children) {
        this(uuid, name, origin, rotation, cubeUuids, children, ModelNodeKind.BONE);
    }
}

