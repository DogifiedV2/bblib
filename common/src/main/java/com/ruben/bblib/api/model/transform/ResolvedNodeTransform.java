package com.ruben.bblib.api.model.transform;

import net.minecraft.world.phys.Vec3;

public record ResolvedNodeTransform(Vec3 modelPosition, Vec3 worldPosition) {
}
