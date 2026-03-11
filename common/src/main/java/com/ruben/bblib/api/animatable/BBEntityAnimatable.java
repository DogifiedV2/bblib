package com.ruben.bblib.api.animatable;

import net.minecraft.world.entity.Entity;

public interface BBEntityAnimatable extends BBAnimatable {

    @Override
    default double getTick(Object object) {
        return ((Entity) this).tickCount;
    }
}
