package com.ruben.bblib.example;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.ControllerRegistrar;
import com.ruben.bblib.api.animatable.RawAnimation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class RootsVfxEntity extends AbstractAnimatableEntity {
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(RootsVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(RootsVfxEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ANCHOR_ID =
            SynchedEntityData.defineId(RootsVfxEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation TRAP = RawAnimation.begin().thenPlayAndHold("trap");

    protected RootsVfxEntity(EntityType<? extends RootsVfxEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static void spawnWalk(ServerLevel level, double x, double y, double z) {
        RootsVfxEntity entity = new RootsVfxEntity(TestEntities.ROOTS_VFX.get(), level);
        entity.setAnimationVariant(AnimationVariant.WALK, 14);
        entity.setPos(x, y - 0.2, z);
        level.addFreshEntity(entity);
    }

    public static void spawnTrap(ServerLevel level, double x, double y, double z) {
        RootsVfxEntity entity = new RootsVfxEntity(TestEntities.ROOTS_VFX.get(), level);
        entity.setAnimationVariant(AnimationVariant.TRAP, 60);
        entity.setPos(x, y, z);
        level.addFreshEntity(entity);
    }

    public static void spawnTrap(ServerLevel level, LivingEntity anchor) {
        RootsVfxEntity entity = new RootsVfxEntity(TestEntities.ROOTS_VFX.get(), level);
        entity.setAnimationVariant(AnimationVariant.TRAP, 60);
        entity.entityData.set(DATA_ANCHOR_ID, anchor.getId());
        entity.setPos(anchor.getX(), anchor.getY() - 0.2, anchor.getZ());
        level.addFreshEntity(entity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VARIANT, AnimationVariant.WALK.ordinal());
        builder.define(DATA_LIFETIME, 14);
        builder.define(DATA_ANCHOR_ID, -1);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0.0, 0.0, 0.0);

        Entity anchor = this.level().getEntity(this.entityData.get(DATA_ANCHOR_ID));
        if (anchor instanceof LivingEntity living && living.isAlive()) {
            this.setPos(living.getX(), living.getY() - 0.2, living.getZ());

            if (!this.level().isClientSide() && getAnimationVariant() == AnimationVariant.TRAP) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 11, false, false, true));

                Vec3 movement = living.getDeltaMovement();
                double verticalSpeed = movement.y > 0.0 ? 0.0 : Math.max(movement.y, -0.08);
                living.setDeltaMovement(movement.x * 0.15, verticalSpeed, movement.z * 0.15);
                living.fallDistance = 0.0f;
            }
        }

        if (this.tickCount >= this.entityData.get(DATA_LIFETIME)) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(getAnimationVariant() == AnimationVariant.TRAP ? TRAP : WALK)));
    }

    private void setAnimationVariant(AnimationVariant variant, int lifetime) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
        this.entityData.set(DATA_LIFETIME, lifetime);
    }

    private AnimationVariant getAnimationVariant() {
        int ordinal = this.entityData.get(DATA_VARIANT);
        AnimationVariant[] values = AnimationVariant.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return AnimationVariant.WALK;
        }
        return values[ordinal];
    }

    private enum AnimationVariant {
        WALK,
        TRAP
    }
}
