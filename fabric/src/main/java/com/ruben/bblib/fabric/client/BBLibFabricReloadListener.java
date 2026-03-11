package com.ruben.bblib.fabric.client;

import com.ruben.bblib.internal.cache.BBModelReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class BBLibFabricReloadListener implements IdentifiableResourceReloadListener {
    public static final BBLibFabricReloadListener INSTANCE = new BBLibFabricReloadListener();

    private BBLibFabricReloadListener() {
    }

    @Override
    public ResourceLocation getFabricId() {
        return BBModelReloadListener.ID;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return BBModelReloadListener.INSTANCE.reload(
                barrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
    }
}

