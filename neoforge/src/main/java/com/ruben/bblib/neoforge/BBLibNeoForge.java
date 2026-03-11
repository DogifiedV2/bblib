package com.ruben.bblib.neoforge;

import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.internal.cache.BBModelReloadListener;
import com.ruben.bblib.internal.client.BBLibClient;
import com.ruben.bblib.example.TestEntities;
import com.ruben.bblib.example.client.PumpkinBossTestEntityRenderer;
import com.ruben.bblib.example.client.TestEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@Mod(BBLibCommon.MOD_ID)
public final class BBLibNeoForge {
    public BBLibNeoForge() {
        BBLibCommon.init();
    }

    @EventBusSubscriber(modid = BBLibCommon.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ClientEvents {
        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(TestEntities.TEST_ENTITY.get(), TestEntityRenderer::new);
            event.registerEntityRenderer(TestEntities.PUMPKIN_BOSS_TEST.get(), PumpkinBossTestEntityRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(BBModelReloadListener.INSTANCE);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(BBLibClient::init);
        }
    }
}

