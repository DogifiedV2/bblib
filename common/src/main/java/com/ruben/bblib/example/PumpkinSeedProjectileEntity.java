package com.ruben.bblib.example;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.ControllerRegistrar;
import com.ruben.bblib.api.animatable.RawAnimation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;

public class PumpkinSeedProjectileEntity extends AbstractAnimatableEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final float GRAVITY = 0.03f;
    private static final int MAX_LIFE = 80;

    private int ownerId = -1;

    protected PumpkinSeedProjectileEntity(EntityType<? extends PumpkinSeedProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawnFromBoss(ServerLevel level, PumpkinBossTestEntity owner, LivingEntity target) {
        PumpkinSeedProjectileEntity projectile = new PumpkinSeedProjectileEntity(TestEntities.PUMPKIN_SEED_PROJECTILE.get(), level);
        Vec3 spawnPos = owner.position().add(0.0, 1.8, 0.0);
        Vec3 targetPos = target.position().add(0.0, target.getBbHeight() * 0.45, 0.0);
        Vec3 launch = targetPos.subtract(spawnPos).normalize().scale(1.15).add(0.0, 0.15, 0.0);

        projectile.ownerId = owner.getId();
        projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        projectile.setDeltaMovement(launch);
        projectile.setYRot((float) (Mth.atan2(launch.x, launch.z) * (180.0f / Math.PI)));
        level.addFreshEntity(projectile);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 deltaMovement = this.getDeltaMovement();
        Vec3 start = this.position();
        Vec3 end = start.add(deltaMovement);

        if (this.level.isClientSide) {
            this.level.addParticle(new DustParticleOptions(new Vector3f(1.0f, 0.61f, 0.17f), 1.0f),
                    this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            this.level.addParticle(new DustParticleOptions(new Vector3f(0.17f, 0.85f, 0.52f), 1.0f),
                    this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        } else {
            LivingEntity hitEntity = findHitEntity(end);
            if (hitEntity != null) {
                onHitEntity(hitEntity);
                return;
            }

            BlockHitResult blockHitResult = this.level.clip(new ClipContext(start, end,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHitResult.getType() != BlockHitResult.Type.MISS) {
                this.discard();
                return;
            }
        }

        this.setPos(end.x, end.y, end.z);
        this.setDeltaMovement(deltaMovement.add(0.0, -GRAVITY, 0.0));

        if (this.tickCount >= MAX_LIFE) {
            this.discard();
        }
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> state.setAndContinue(IDLE)));
    }

    private LivingEntity findHitEntity(Vec3 end) {
        AABB box = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(0.35);
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living.isAlive() && living.getId() != this.ownerId)) {
            double distance = living.distanceToSqr(end);
            if (distance < closestDistance) {
                closest = living;
                closestDistance = distance;
            }
        }

        return closest;
    }

    private void onHitEntity(LivingEntity target) {
        Entity owner = this.level.getEntity(this.ownerId);
        if (owner instanceof PumpkinBossTestEntity boss && this.level instanceof ServerLevel serverLevel) {
            boss.applySeedCurse(serverLevel, target);
        }
        this.discard();
    }
}
