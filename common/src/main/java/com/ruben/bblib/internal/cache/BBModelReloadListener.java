package com.ruben.bblib.internal.cache;

import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.internal.client.texture.BBTextureManager;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.internal.parser.BBModelParser;
import com.ruben.bblib.internal.parser.ParseResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Environment(EnvType.CLIENT)
public final class BBModelReloadListener implements PreparableReloadListener {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BBLibCommon.MOD_ID, "bbmodels");
    public static final BBModelReloadListener INSTANCE = new BBModelReloadListener();

    private static final String BBMODELS_DIRECTORY = "bbmodels";
    private static final String BBMODEL_EXTENSION = ".bbmodel";

    private BBModelReloadListener() {
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(
                        () -> loadAllModels(resourceManager), backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(models -> {
                    BBTextureManager.getInstance().clearTextures();
                    BBModelCache.setModels(models);
                    BBLibCommon.LOGGER.info("Loaded {} bbmodel(s)", models.size());
                }, gameExecutor);
    }

    private static Map<ResourceLocation, ModelData> loadAllModels(ResourceManager resourceManager) {
        Map<ResourceLocation, ModelData> models = new HashMap<>();

        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                BBMODELS_DIRECTORY, path -> path.getPath().endsWith(BBMODEL_EXTENSION));

        BBLibCommon.LOGGER.info("BBModelReloadListener scanning '{}' directory, found {} resource(s)", BBMODELS_DIRECTORY, resources.size());
        for (ResourceLocation key : resources.keySet()) {
            BBLibCommon.LOGGER.info("  Found resource: {}", key);
        }

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation fileLocation = entry.getKey();
            ResourceLocation modelId = extractModelId(fileLocation);

            try (InputStream inputStream = entry.getValue().open()) {
                String jsonContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                ParseResult result = BBModelParser.parse(modelId.toString(), jsonContent);

                for (String warning : result.getWarnings()) {
                    BBLibCommon.LOGGER.warn("[{}] {}", modelId, warning);
                }
                for (String error : result.getErrors()) {
                    BBLibCommon.LOGGER.error("[{}] {}", modelId, error);
                }

                if (result.getModel() != null) {
                    models.put(modelId, result.getModel());
                }
            } catch (Exception e) {
                BBLibCommon.LOGGER.error("Failed to load bbmodel '{}'", modelId, e);
            }
        }

        return models;
    }

    private static ResourceLocation extractModelId(ResourceLocation fileLocation) {
        String path = fileLocation.getPath();
        String modelName = path.substring(
                BBMODELS_DIRECTORY.length() + 1,
                path.length() - BBMODEL_EXTENSION.length());
        return ResourceLocation.fromNamespaceAndPath(fileLocation.getNamespace(), modelName);
    }
}

