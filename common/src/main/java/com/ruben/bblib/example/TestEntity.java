package com.ruben.bblib.example;

import com.ruben.bblib.api.BBLibApi;
import com.ruben.bblib.api.animatable.AnimatableInstanceCache;
import com.ruben.bblib.api.animatable.BBEntityAnimatable;
import com.ruben.bblib.api.animatable.ControllerRegistrar;
import com.ruben.bblib.api.animatable.DefaultAnimations;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class TestEntity extends PathfinderMob implements BBEntityAnimatable {

    private final AnimatableInstanceCache cache = BBLibApi.createCache(this);

    protected TestEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController(this, "main", 5,
                DefaultAnimations.WALK, DefaultAnimations.IDLE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

