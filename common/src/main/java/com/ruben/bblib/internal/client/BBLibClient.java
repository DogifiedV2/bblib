package com.ruben.bblib.internal.client;

import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.example.TestEntities;
import com.ruben.bblib.example.client.PumpkinBossTestEntityRenderer;
import com.ruben.bblib.example.client.TestEntityRenderer;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BBLibClient {

    private BBLibClient() {
    }

    public static void init() {
        BBLibCommon.LOGGER.info("BBLib client initialized");
    }

    public static void registerRenderers() {
        EntityRendererRegistry.register(TestEntities.TEST_ENTITY, TestEntityRenderer::new);
        EntityRendererRegistry.register(TestEntities.PUMPKIN_BOSS_TEST, PumpkinBossTestEntityRenderer::new);
    }
}

