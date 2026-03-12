package com.ruben.bblib.internal.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.api.model.data.TextureData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class BBTextureManager {

    private static final BBTextureManager INSTANCE = new BBTextureManager();

    private final Map<String, RegisteredTexture> registeredTextures = new ConcurrentHashMap<>();
    private final Map<String, NativeImage> sourceImages = new ConcurrentHashMap<>();
    private final Map<String, AnimatedTextureState> interpolatedTextures = new ConcurrentHashMap<>();

    private BBTextureManager() {
    }

    public static BBTextureManager getInstance() {
        return INSTANCE;
    }

    public ResourceLocation getOrCreateTexture(ResourceLocation modelId, int textureIndex, TextureData textureData) {
        if (!textureData.isAnimated()) {
            String key = buildKey(modelId, textureIndex);
            return registeredTextures.computeIfAbsent(key, k -> registerTexture(k, readImage(textureData.imageData()))).location();
        }

        return getOrCreateTexture(modelId, textureIndex, textureData, textureData.resolveAnimationFrame(0.0d));
    }

    public ResourceLocation getOrCreateTexture(ResourceLocation modelId, int textureIndex, TextureData textureData,
                                               TextureData.AnimationFrame animationFrame) {
        if (!textureData.isAnimated()) {
            return getOrCreateTexture(modelId, textureIndex, textureData);
        }

        String key = buildKey(modelId, textureIndex);
        if (textureData.frameInterpolate()) {
            return getOrCreateInterpolatedTexture(key, textureData, animationFrame);
        }

        int frameIndex = clampFrameIndex(animationFrame.frameIndex(), textureData.frameCount());
        String frameKey = key + "_frame_" + frameIndex;
        return registeredTextures.computeIfAbsent(frameKey,
                ignored -> registerTexture(frameKey, createFrameImage(key, textureData, frameIndex))).location();
    }

    public static ResourceLocation getDefaultTexture(String namespace, String modelPath) {
        ResourceLocation modelId = new ResourceLocation(namespace, modelPath);
        return getDefaultTexture(modelId);
    }

    public static ResourceLocation getDefaultTexture(ResourceLocation modelId) {
        return new ResourceLocation(BBLibCommon.MOD_ID, "dynamic/" + flattenKey(modelId) + "_0");
    }

    public void clearTextures() {
        for (RegisteredTexture texture : registeredTextures.values()) {
            Minecraft.getInstance().getTextureManager().release(texture.location());
        }
        registeredTextures.clear();

        for (NativeImage image : sourceImages.values()) {
            image.close();
        }
        sourceImages.clear();
        interpolatedTextures.clear();
    }

    private ResourceLocation getOrCreateInterpolatedTexture(String key, TextureData textureData,
                                                            TextureData.AnimationFrame animationFrame) {
        AnimatedTextureState state = interpolatedTextures.computeIfAbsent(key, ignored -> createInterpolatedState(key, textureData));
        if (state == null) {
            return MissingModelTexture.getLocation();
        }

        int currentFrame = clampFrameIndex(animationFrame.frameIndex(), textureData.frameCount());
        int nextFrame = clampFrameIndex(animationFrame.nextFrameIndex(), textureData.frameCount());
        int blendKey = Math.round(Math.max(0.0f, Math.min(1.0f, animationFrame.interpolation())) * 255.0f);

        if (state.currentFrame() != currentFrame || state.nextFrame() != nextFrame || state.blendKey() != blendKey) {
            updateInterpolatedTexture(state, textureData, currentFrame, nextFrame, blendKey / 255.0f);
        }

        return state.location();
    }

    private AnimatedTextureState createInterpolatedState(String key, TextureData textureData) {
        try {
            NativeImage workingImage = createFrameImage(key, textureData, 0);
            DynamicTexture dynamicTexture = new DynamicTexture(workingImage);
            ResourceLocation location = new ResourceLocation(BBLibCommon.MOD_ID, "dynamic/" + key + "_interp");
            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
            registeredTextures.put(key + "_interp", new RegisteredTexture(location, dynamicTexture));
            return new AnimatedTextureState(location, dynamicTexture, workingImage, key, -1, -1, -1);
        } catch (Exception e) {
            BBLibCommon.LOGGER.error("Failed to create interpolated texture for {}", key, e);
            return null;
        }
    }

    private void updateInterpolatedTexture(AnimatedTextureState state, TextureData textureData,
                                           int currentFrame, int nextFrame, float blend) {
        NativeImage currentImage = createFrameImage(state.sourceKey(), textureData, currentFrame);
        NativeImage nextImage = createFrameImage(state.sourceKey(), textureData, nextFrame);
        NativeImage workingImage = state.image();

        try {
            int width = workingImage.getWidth();
            int height = workingImage.getHeight();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int currentPixel = currentImage.getPixelRGBA(x, y);
                    int nextPixel = nextImage.getPixelRGBA(x, y);
                    workingImage.setPixelRGBA(x, y, blendPixel(currentPixel, nextPixel, blend));
                }
            }
            state.texture().upload();
            state.setState(currentFrame, nextFrame, Math.round(blend * 255.0f));
        } finally {
            currentImage.close();
            nextImage.close();
        }
    }

    private RegisteredTexture registerTexture(String key, NativeImage image) {
        try {
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation location = new ResourceLocation(BBLibCommon.MOD_ID, "dynamic/" + key);
            Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
            return new RegisteredTexture(location, dynamicTexture);
        } catch (Exception e) {
            BBLibCommon.LOGGER.error("Failed to create texture for {}", key, e);
            image.close();
            return new RegisteredTexture(MissingModelTexture.getLocation(), null);
        }
    }

    private NativeImage createFrameImage(String key, TextureData textureData, int frameIndex) {
        NativeImage sourceImage = getOrLoadSourceImage(key, textureData);
        int frameWidth = Math.min(textureData.uvWidthOrDefault(), sourceImage.getWidth());
        int frameHeight = Math.min(textureData.uvHeightOrDefault(), sourceImage.getHeight());
        int sourceY = Math.min(frameIndex * frameHeight, Math.max(0, sourceImage.getHeight() - frameHeight));

        NativeImage frameImage = new NativeImage(frameWidth, frameHeight, false);
        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                frameImage.setPixelRGBA(x, y, sourceImage.getPixelRGBA(x, sourceY + y));
            }
        }
        return frameImage;
    }

    private NativeImage getOrLoadSourceImage(String key, TextureData textureData) {
        return sourceImages.computeIfAbsent(key, ignored -> readImage(textureData.imageData()));
    }

    private NativeImage readImage(byte[] imageData) {
        try {
            return NativeImage.read(new ByteArrayInputStream(imageData));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read embedded texture image", e);
        }
    }

    private static int clampFrameIndex(int frameIndex, int frameCount) {
        if (frameCount <= 0) {
            return 0;
        }
        return Math.floorMod(frameIndex, frameCount);
    }

    private static int blendPixel(int currentPixel, int nextPixel, float blend) {
        if (blend <= 0.0f) {
            return currentPixel;
        }
        if (blend >= 1.0f) {
            return nextPixel;
        }

        int a = blendChannel((currentPixel >> 24) & 0xFF, (nextPixel >> 24) & 0xFF, blend);
        int b = blendChannel((currentPixel >> 16) & 0xFF, (nextPixel >> 16) & 0xFF, blend);
        int g = blendChannel((currentPixel >> 8) & 0xFF, (nextPixel >> 8) & 0xFF, blend);
        int r = blendChannel(currentPixel & 0xFF, nextPixel & 0xFF, blend);

        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int blendChannel(int currentValue, int nextValue, float blend) {
        return Math.round(currentValue + ((nextValue - currentValue) * blend));
    }

    private static String buildKey(ResourceLocation modelId, int textureIndex) {
        return flattenKey(modelId) + "_" + textureIndex;
    }

    private static String flattenKey(ResourceLocation modelId) {
        return modelId.getNamespace() + "_" + modelId.getPath().replace('/', '_');
    }

    private record RegisteredTexture(ResourceLocation location, DynamicTexture texture) {
    }

    private static final class AnimatedTextureState {
        private final ResourceLocation location;
        private final DynamicTexture texture;
        private final NativeImage image;
        private final String sourceKey;
        private int currentFrame;
        private int nextFrame;
        private int blendKey;

        private AnimatedTextureState(ResourceLocation location, DynamicTexture texture, NativeImage image,
                                     String sourceKey,
                                     int currentFrame, int nextFrame, int blendKey) {
            this.location = location;
            this.texture = texture;
            this.image = image;
            this.sourceKey = sourceKey;
            this.currentFrame = currentFrame;
            this.nextFrame = nextFrame;
            this.blendKey = blendKey;
        }

        private ResourceLocation location() {
            return location;
        }

        private DynamicTexture texture() {
            return texture;
        }

        private NativeImage image() {
            return image;
        }

        private String sourceKey() {
            return sourceKey;
        }

        private int currentFrame() {
            return currentFrame;
        }

        private int nextFrame() {
            return nextFrame;
        }

        private int blendKey() {
            return blendKey;
        }

        private void setState(int currentFrame, int nextFrame, int blendKey) {
            this.currentFrame = currentFrame;
            this.nextFrame = nextFrame;
            this.blendKey = blendKey;
        }
    }
}

