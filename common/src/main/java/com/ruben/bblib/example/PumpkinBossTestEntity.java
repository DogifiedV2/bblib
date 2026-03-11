package com.ruben.bblib.example;

import com.ruben.bblib.api.BBLibApi;
import com.ruben.bblib.api.animatable.AnimatableInstanceCache;
import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animatable.ControllerRegistrar;
import com.ruben.bblib.api.animatable.RawAnimation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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

public class PumpkinBossTestEntity extends Monster implements BBAnimatable {
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
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("hurt");
    private static final RawAnimation DEATH = RawAnimation.begin().thenPlayAndHold("death");

    private final AnimatableInstanceCache cache = BBLibApi.createCache(this);

    private AnimationKey currentAnimationKey = AnimationKey.IDLE;

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

        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_IN_BATTLE, this.getTarget() != null);

            int attackAnimationTicks = this.entityData.get(DATA_ATTACK_ANIMATION_TICKS);
            if (attackAnimationTicks > 0) {
                this.entityData.set(DATA_ATTACK_ANIMATION_TICKS, attackAnimationTicks - 1);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean didAttack = super.doHurtTarget(target);

        if (didAttack) {
            AttackVariant attackVariant = this.random.nextBoolean() ? AttackVariant.PRIMARY : AttackVariant.SECONDARY;
            this.entityData.set(DATA_ATTACK_VARIANT, attackVariant.ordinal());
            this.entityData.set(DATA_ATTACK_ANIMATION_TICKS, 36);
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
                nextKey = attackVariant == AttackVariant.PRIMARY ? AnimationKey.ATTACK : AnimationKey.ATTACK_ALT;
                nextAnimation = attackVariant == AttackVariant.PRIMARY ? ATTACK : ATTACK_ALT;
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

    @Override
    public double getTick(Object object) {
        return this.tickCount;
    }

    private enum AttackVariant {
        PRIMARY,
        SECONDARY;

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
        HURT,
        DEATH
    }
}

