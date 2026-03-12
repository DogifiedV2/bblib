package com.ruben.bblib.api.model.data;

import org.jetbrains.annotations.Nullable;

public record BillboardData(
        String uuid,
        String name,
        boolean visible,
        Vec3f origin,
        Vec2f size,
        Vec2f offset,
        BillboardFacingMode facingMode,
        @Nullable FaceData frontFace
) {
}
