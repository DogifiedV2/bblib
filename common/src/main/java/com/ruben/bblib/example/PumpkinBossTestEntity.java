package com.ruben.bblib.example;

import com.ruben.bblib.api.BBLibApi;
import com.ruben.bblib.api.animatable.AnimatableInstanceCache;
import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBEntityAnimatable;
import com.ruben.bblib.api.animatable.ControllerRegistrar;
import com.ruben.bblib.api.animatable.RawAnimation;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PumpkinBossTestEntity extends Monster implements BBEntityAnimatable {
    public static final int MELEE_ATTACK_DURATION = 36;
    public static final int SEED_ATTACK_DURATION = 80;
    public static final int SEED_RELEASE_TICK = 66;
    public static final int VINES_ATTACK_DURATION = 50;
    public static final int VINES_RELEASE_TICK = 33;

    private static final int SPECIAL_ATTACK_CHECK_INTERVAL = 30;
    private static final int SEED_ATTACK_COOLDOWN = 25 * 20;
    private static final int VINES_ATTACK_COOLDOWN = 20 * 20;
    private static final double SEED_ATTACK_MIN_RANGE_SQR = 16.0;
    private static final double SEED_ATTACK_MAX_RANGE_SQR = 196.0;
    private static final double VINES_ATTACK_MIN_RANGE_SQR = 9.0;
    private static final double VINES_ATTACK_MAX_RANGE_SQR = 144.0;

    private static final EntityDataAccessor<Boolean> DATA_IN_BATTLE =
            SynchedEntityData.defineId(PumpkinBossTestEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIMATION_TICKS =
            SynchedEntityData.defineId(PumpkinBossTestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_VARIANT =
            SynchedEntityData.defineId(PumpkinBossTestEntity.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation IDLE_BATTLE = RawAnimation.begin().thenLoop("idle_battle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation WALK_BATTLE = RawAnimation.begin().thenLoop("walk_battle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("attack");
    private static final RawAnimation ATTACK_ALT = RawAnimation.begin().thenPlay("attack2");
    private static final RawAnimation SEED_ATTACK = RawAnimation.begin().thenPlay("pumpkin_seed");
    private static final RawAnimation VINES_ATTACK = RawAnimation.begin().thenPlay("vines");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

    private final AnimatableInstanceCache cache = BBLibApi.createCache(this);
    private final Map<Integer, SeedCurseData> activeSeedCurses = new HashMap<>();

    private AnimationKey currentAnimationKey = AnimationKey.IDLE;
    private int seedAttackCooldown;
    private int vinesAttackCooldown;
    private int specialAttackCheckCooldown = SPECIAL_ATTACK_CHECK_INTERVAL;

    protected PumpkinBossTestEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 40;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_IN_BATTLE, false);
        builder.define(DATA_ATTACK_ANIMATION_TICKS, 0);
        builder.define(DATA_ATTACK_VARIANT, AttackVariant.PRIMARY.ordinal());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide()) {
            return;
        }

        this.entityData.set(DATA_IN_BATTLE, this.getTarget() != null);
        tickSeedCurses();

        if (this.seedAttackCooldown > 0) {
            this.seedAttackCooldown--;
        }
        if (this.vinesAttackCooldown > 0) {
            this.vinesAttackCooldown--;
        }
        if (this.specialAttackCheckCooldown > 0) {
            this.specialAttackCheckCooldown--;
        }

        int attackAnimationTicks = this.entityData.get(DATA_ATTACK_ANIMATION_TICKS);
        if (attackAnimationTicks > 0) {
            handleAttackTick(attackAnimationTicks);
            this.entityData.set(DATA_ATTACK_ANIMATION_TICKS, attackAnimationTicks - 1);
            return;
        }

        if (this.getTarget() != null && this.specialAttackCheckCooldown <= 0) {
            this.specialAttackCheckCooldown = SPECIAL_ATTACK_CHECK_INTERVAL;

            if (this.random.nextFloat() <= 0.5f) {
                tryStartSpecialAttack();
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.entityData.get(DATA_ATTACK_ANIMATION_TICKS) > 0) {
            return false;
        }

        boolean didAttack = super.doHurtTarget(target);
        if (didAttack) {
            AttackVariant attackVariant = this.random.nextBoolean() ? AttackVariant.PRIMARY : AttackVariant.SECONDARY;
            this.entityData.set(DATA_ATTACK_VARIANT, attackVariant.ordinal());
            this.entityData.set(DATA_ATTACK_ANIMATION_TICKS, MELEE_ATTACK_DURATION);
        }

        return didAttack;
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> {
            AnimationKey nextKey;
            RawAnimation nextAnimation;
            boolean inBattle = this.entityData.get(DATA_IN_BATTLE);
            int attackAnimationTicks = this.entityData.get(DATA_ATTACK_ANIMATION_TICKS);
            AttackVariant attackVariant = AttackVariant.fromOrdinal(this.entityData.get(DATA_ATTACK_VARIANT));

            if (this.isDeadOrDying()) {
                nextKey = AnimationKey.DEATH;
                nextAnimation = DEATH;
            } else if (this.hurtTime > 0) {
                nextKey = AnimationKey.HURT;
                nextAnimation = HURT;
            } else if (attackAnimationTicks > 0) {
                nextKey = switch (attackVariant) {
                    case PRIMARY -> AnimationKey.ATTACK;
                    case SECONDARY -> AnimationKey.ATTACK_ALT;
                    case SEED -> AnimationKey.SEED_ATTACK;
                    case VINES -> AnimationKey.VINES_ATTACK;
                };
                nextAnimation = switch (attackVariant) {
                    case PRIMARY -> ATTACK;
                    case SECONDARY -> ATTACK_ALT;
                    case SEED -> SEED_ATTACK;
                    case VINES -> VINES_ATTACK;
                };
            } else if (state.isMoving()) {
                nextKey = inBattle ? AnimationKey.WALK_BATTLE : AnimationKey.WALK;
                nextAnimation = inBattle ? WALK_BATTLE : WALK;
            } else {
                nextKey = inBattle ? AnimationKey.IDLE_BATTLE : AnimationKey.IDLE;
                nextAnimation = inBattle ? IDLE_BATTLE : IDLE;
            }

            if (this.currentAnimationKey != nextKey) {
                this.currentAnimationKey = nextKey;
                state.resetCurrentAnimation();
            }

            return state.setAndContinue(nextAnimation);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public void applySeedCurse(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
        this.activeSeedCurses.put(target.getId(), new SeedCurseData(target.getId(), 4, 20));

        level.sendParticles(new DustParticleOptions(new Vector3f(0.45f, 0.44f, 0.11f), 1.0f),
                target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
    }

    private void tickSeedCurses() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.activeSeedCurses.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<Integer, SeedCurseData>> iterator = this.activeSeedCurses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, SeedCurseData> entry = iterator.next();
            SeedCurseData seedCurseData = entry.getValue();
            Entity entity = serverLevel.getEntity(seedCurseData.targetId());

            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            seedCurseData.tick();
            if (!seedCurseData.shouldPulse()) {
                continue;
            }

            DamageSource damageSource = this.damageSources().mobAttack(this);
            float pulseDamage = (float) (target.getMaxHealth() * 0.04f);
            target.hurt(damageSource, pulseDamage);

            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.17f, 0.85f, 0.52f), 1.0f),
                    target.getX(), target.getY() + 0.5, target.getZ(), 60, 6.0, 0.1, 6.0, 0.0);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.HOSTILE, 0.7f, 0.7f);

            for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(6.0),
                    nearby -> nearby != this && nearby != target && nearby.isAlive())) {
                nearby.hurt(damageSource, 10.0f);
                nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 0));
            }

            if (seedCurseData.finishPulse()) {
                iterator.remove();
            }
        }
    }

    private void tryStartSpecialAttack() {
        boolean canSeed = canStartSeedAttack();
        boolean canVines = canStartVinesAttack();

        if (!canSeed && !canVines) {
            return;
        }

        if (canSeed && canVines) {
            if (this.random.nextBoolean()) {
                startSeedAttack();
            } else {
                startVinesAttack();
            }
            return;
        }

        if (canSeed) {
            startSeedAttack();
        } else {
            startVinesAttack();
        }
    }

    private boolean canStartSeedAttack() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (this.seedAttackCooldown > 0 || !this.hasLineOfSight(target)) {
            return false;
        }

        double distanceToTarget = this.distanceToSqr(target);
        return distanceToTarget >= SEED_ATTACK_MIN_RANGE_SQR && distanceToTarget <= SEED_ATTACK_MAX_RANGE_SQR;
    }

    private boolean canStartVinesAttack() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        if (this.vinesAttackCooldown > 0 || !this.hasLineOfSight(target)) {
            return false;
        }

        double distanceToTarget = this.distanceToSqr(target);
        return distanceToTarget >= VINES_ATTACK_MIN_RANGE_SQR && distanceToTarget <= VINES_ATTACK_MAX_RANGE_SQR;
    }

    private void startSeedAttack() {
        this.getNavigation().stop();
        this.entityData.set(DATA_ATTACK_VARIANT, AttackVariant.SEED.ordinal());
        this.entityData.set(DATA_ATTACK_ANIMATION_TICKS, SEED_ATTACK_DURATION);
        this.seedAttackCooldown = SEED_ATTACK_COOLDOWN;
    }

    private void startVinesAttack() {
        this.getNavigation().stop();
        this.entityData.set(DATA_ATTACK_VARIANT, AttackVariant.VINES.ordinal());
        this.entityData.set(DATA_ATTACK_ANIMATION_TICKS, VINES_ATTACK_DURATION);
        this.vinesAttackCooldown = VINES_ATTACK_COOLDOWN;
    }

    private void handleAttackTick(int attackAnimationTicks) {
        AttackVariant attackVariant = AttackVariant.fromOrdinal(this.entityData.get(DATA_ATTACK_VARIANT));
        int elapsedTicks = switch (attackVariant) {
            case VINES -> VINES_ATTACK_DURATION - attackAnimationTicks;
            case SEED -> SEED_ATTACK_DURATION - attackAnimationTicks;
            default -> MELEE_ATTACK_DURATION - attackAnimationTicks;
        };

        if (attackVariant == AttackVariant.SEED && elapsedTicks == SEED_RELEASE_TICK) {
            handleSeedRelease();
        } else if (attackVariant == AttackVariant.VINES && elapsedTicks == VINES_RELEASE_TICK) {
            handleVinesRelease();
        }

        if (attackVariant == AttackVariant.SEED || attackVariant == AttackVariant.VINES) {
            this.getNavigation().stop();
            this.setSprinting(false);
            lockFacingTarget();
        }
    }

    private void handleSeedRelease() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 0.6f, 0.5f);
        PumpkinSeedProjectileEntity.spawnFromBoss(serverLevel, this, target);
    }

    private void handleVinesRelease() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.7f, 0.8f);
        VinesProjectileEntity.spawnFromBoss(serverLevel, this, target);
    }

    private void lockFacingTarget() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        this.getLookControl().setLookAt(target, 30.0f, 30.0f);

        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0f / Math.PI)) - 90.0f;

        this.setYRot(yaw);
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
    }

    private enum AttackVariant {
        PRIMARY,
        SECONDARY,
        SEED,
        VINES;

        private static AttackVariant fromOrdinal(int ordinal) {
            AttackVariant[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return PRIMARY;
            }
            return values[ordinal];
        }
    }

    private enum AnimationKey {
        IDLE,
        IDLE_BATTLE,
        WALK,
        WALK_BATTLE,
        ATTACK,
        ATTACK_ALT,
        SEED_ATTACK,
        VINES_ATTACK,
        HURT,
        DEATH
    }

    private static final class SeedCurseData {
        private final int targetId;
        private int remainingPulses;
        private int ticksUntilPulse;

        private SeedCurseData(int targetId, int remainingPulses, int ticksUntilPulse) {
            this.targetId = targetId;
            this.remainingPulses = remainingPulses;
            this.ticksUntilPulse = ticksUntilPulse;
        }

        private int targetId() {
            return this.targetId;
        }

        private void tick() {
            this.ticksUntilPulse--;
        }

        private boolean shouldPulse() {
            return this.ticksUntilPulse <= 0;
        }

        private boolean finishPulse() {
            this.remainingPulses--;
            this.ticksUntilPulse = 20;
            return this.remainingPulses <= 0;
        }
    }
}
