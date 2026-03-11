package com.ruben.bblib.example;

import com.ruben.bblib.internal.BBLibCommon;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class TestEntities {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BBLibCommon.MOD_ID, Registry.ENTITY_TYPE_REGISTRY);

    public static final RegistrySupplier<EntityType<TestEntity>> TEST_ENTITY =
            ENTITY_TYPES.register("test_entity", () ->
                    EntityType.Builder.of(TestEntity::new, MobCategory.CREATURE)
                            .sized(1.4f, 0.9f)
                            .build("test_entity"));

    public static final RegistrySupplier<EntityType<PumpkinBossTestEntity>> PUMPKIN_BOSS_TEST =
            ENTITY_TYPES.register("pumpkin_boss_test", () ->
                    EntityType.Builder.of(PumpkinBossTestEntity::new, MobCategory.MONSTER)
                            .sized(1.6f, 4.8f)
                            .build("pumpkin_boss_test"));

    public static final RegistrySupplier<EntityType<PumpkinSeedProjectileEntity>> PUMPKIN_SEED_PROJECTILE =
            ENTITY_TYPES.register("pumpkin_seed_projectile", () ->
                    EntityType.Builder.of(PumpkinSeedProjectileEntity::new, MobCategory.MISC)
                            .sized(0.6f, 0.6f)
                            .build("pumpkin_seed_projectile"));

    public static final RegistrySupplier<EntityType<RootsVfxEntity>> ROOTS_VFX =
            ENTITY_TYPES.register("roots_vfx", () ->
                    EntityType.Builder.of(RootsVfxEntity::new, MobCategory.MISC)
                            .sized(2.5f, 2.5f)
                            .build("roots_vfx"));

    public static final RegistrySupplier<EntityType<VinesProjectileEntity>> VINES_PROJECTILE =
            ENTITY_TYPES.register("vines_projectile", () ->
                    EntityType.Builder.of(VinesProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .build("vines_projectile"));

    public static void register() {
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(TEST_ENTITY, TestEntity::createAttributes);
        EntityAttributeRegistry.register(PUMPKIN_BOSS_TEST, PumpkinBossTestEntity::createAttributes);
    }
}

