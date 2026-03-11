package com.ruben.bblib.example;

import com.ruben.bblib.internal.BBLibCommon;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class TestEntities {

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BBLibCommon.MOD_ID, Registries.ENTITY_TYPE);

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

    public static void register() {
        ENTITY_TYPES.register();
        EntityAttributeRegistry.register(TEST_ENTITY, TestEntity::createAttributes);
        EntityAttributeRegistry.register(PUMPKIN_BOSS_TEST, PumpkinBossTestEntity::createAttributes);
    }
}

