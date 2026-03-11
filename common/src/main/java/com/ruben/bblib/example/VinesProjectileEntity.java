package com.ruben.bblib.example;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class VinesProjectileEntity extends Entity {
    private static final int MAX_LIFE = 40;
    private static final double SPEED = 0.55;

    private int ownerId = -1;
    private int targetId = -1;
    private Vec3 targetPosition = Vec3.ZERO;

    protected VinesProjectileEntity(EntityType<? extends VinesProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawnFromBoss(ServerLevel level, PumpkinBossTestEntity owner, LivingEntity target) {
        VinesProjectileEntity projectile = new VinesProjectileEntity(TestEntities.VINES_PROJECTILE.get(), level);
        Vec3 spawnPos = new Vec3(owner.getX(), owner.getY(), owner.getZ());
        Vec3 targetPos = new Vec3(target.getX(), target.getY(), target.getZ());
        Vec3 horizontal = targetPos.subtract(spawnPos).multiply(1.0, 0.0, 1.0).normalize().scale(0.55);

        projectile.ownerId = owner.getId();
        projectile.targetId = target.getId();
        projectile.targetPosition = targetPos;
        projectile.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        projectile.setDeltaMovement(horizontal);
        projectile.setYRot((float) (Mth.atan2(horizontal.x, horizontal.z) * (180.0f / Math.PI)));
        level.addFreshEntity(projectile);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag compoundTag) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag compoundTag) {
    }

    @Override
    public void tick() {
        super.tick();

        LivingEntity target = getTrackedTarget();
        if (target != null && target.isAlive()) {
            this.targetPosition = new Vec3(target.getX(), target.getY(), target.getZ());

            Vec3 horizontalDirection = this.targetPosition.subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (horizontalDirection.lengthSqr() > 1.0E-6) {
                this.setDeltaMovement(horizontalDirection.normalize().scale(SPEED));
            }
        }

        Vec3 nextPosition = this.position().add(this.getDeltaMovement());
        if (target != null && target.isAlive()) {
            double desiredY = target.getY();
            double maxStepUp = 0.5;
            double adjustedY = Mth.clamp(desiredY, this.getY() - 0.25, this.getY() + maxStepUp);
            nextPosition = new Vec3(nextPosition.x, adjustedY, nextPosition.z);
        }
        this.setPos(nextPosition.x, nextPosition.y, nextPosition.z);

        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount % 2 == 0) {
                RootsVfxEntity.spawnWalk(serverLevel,
                        this.getX() + Mth.nextDouble(this.random, -1.5, 1.5),
                        this.getY(),
                        this.getZ() + Mth.nextDouble(this.random, -1.5, 1.5));
            }

            LivingEntity hitEntity = findHitEntity();
            if (hitEntity != null || hasReachedTarget(target) || this.tickCount >= MAX_LIFE) {
                triggerImpact(serverLevel, hitEntity);
                return;
            }
        } else {
            this.level().addParticle(new DustParticleOptions(new Vector3f(0.45f, 0.44f, 0.11f), 1.0f),
                    this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(new DustParticleOptions(new Vector3f(1.0f, 0.61f, 0.17f), 1.0f),
                    this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return false;
    }

    private LivingEntity findHitEntity() {
        AABB box = this.getBoundingBox().inflate(1.5, 1.5, 1.5);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box,
                living -> living.isAlive() && living.getId() != this.ownerId)) {
            return living;
        }
        return null;
    }

    private LivingEntity getTrackedTarget() {
        Entity entity = this.level().getEntity(this.targetId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private boolean hasReachedTarget(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return this.distanceToSqr(this.targetPosition) <= 1.5 * 1.5;
        }

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        double horizontalDistanceSqr = (dx * dx) + (dz * dz);
        double verticalDistance = Math.abs(target.getY() - this.getY());
        return horizontalDistanceSqr <= 1.5 * 1.5 && verticalDistance <= 2.0;
    }

    private void triggerImpact(ServerLevel serverLevel, LivingEntity hitEntity) {
        Entity owner = serverLevel.getEntity(this.ownerId);
        float damage = owner instanceof PumpkinBossTestEntity boss
                ? (float) boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) + 6.0f
                : 16.0f;

        if (hitEntity == null) {
            hitEntity = getTrackedTarget();
        }

        if (hitEntity != null && hitEntity.isAlive()) {
            RootsVfxEntity.spawnTrap(serverLevel, hitEntity);
            hitEntity.hurt(serverLevel.damageSources().mobAttack(owner instanceof LivingEntity mob ? mob : null), damage);
            hitEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 11));
        } else {
            RootsVfxEntity.spawnTrap(serverLevel, this.getX(), this.getY(), this.getZ());
        }

        AABB impactBox = new AABB(
                this.getX() - 2.0, this.getY() - 0.5, this.getZ() - 2.0,
                this.getX() + 2.0, this.getY() + 2.5, this.getZ() + 2.0
        );

        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, impactBox,
                living -> living.isAlive() && living.getId() != this.ownerId)) {
            if (living == hitEntity) {
                continue;
            }
            living.hurt(serverLevel.damageSources().mobAttack(owner instanceof LivingEntity mob ? mob : null), damage);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        }

        this.discard();
    }
}
