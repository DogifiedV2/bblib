package com.ruben.bblib.api.model.data;

import org.jetbrains.annotations.Nullable;

public record LocatorData(
        String uuid,
        String name,
        Vec3f origin,
        Vec3f rotation,
        ModelNodeKind kind,
        @Nullable String parentUuid,
        @Nullable String parentName
) {
}
