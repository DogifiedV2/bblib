package com.ruben.bblib.fabric.client;

import com.ruben.bblib.internal.client.BBLibClient;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.PackType;

public final class BBLibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(BBLibFabricReloadListener.INSTANCE);
        BBLibClient.registerRenderers();
        BBLibClient.init();
    }
}

