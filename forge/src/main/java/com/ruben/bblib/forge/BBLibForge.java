package com.ruben.bblib.forge;

import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.internal.cache.BBModelReloadListener;
import com.ruben.bblib.internal.client.BBLibClient;
import com.ruben.bblib.example.TestEntities;
import com.ruben.bblib.example.client.InvisibleEntityRenderer;
import com.ruben.bblib.example.client.PumpkinBossTestEntityRenderer;
import com.ruben.bblib.example.client.PumpkinSeedProjectileRenderer;
import com.ruben.bblib.example.client.RootsVfxEntityRenderer;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BBLibCommon.MOD_ID)
public final class BBLibForge {
    public BBLibForge() {
        EventBuses.registerModEventBus(BBLibCommon.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        BBLibCommon.init();
    }

    @EventBusSubscriber(modid = BBLibCommon.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(TestEntities.PUMPKIN_BOSS_TEST.get(), PumpkinBossTestEntityRenderer::new);
            event.registerEntityRenderer(TestEntities.PUMPKIN_SEED_PROJECTILE.get(), PumpkinSeedProjectileRenderer::new);
            event.registerEntityRenderer(TestEntities.ROOTS_VFX.get(), RootsVfxEntityRenderer::new);
            event.registerEntityRenderer(TestEntities.VINES_PROJECTILE.get(), InvisibleEntityRenderer::new);
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(BBLibClient::init);
        }

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(BBModelReloadListener.INSTANCE);
        }
    }
}
