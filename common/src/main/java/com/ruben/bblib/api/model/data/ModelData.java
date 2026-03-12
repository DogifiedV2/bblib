package com.ruben.bblib.api.model.data;

import com.ruben.bblib.api.animation.BBAnimation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ModelData(
        String id,
        String name,
        int textureWidth,
        int textureHeight,
        Map<String, CubeData> cubes,
        Map<String, BillboardData> billboards,
        List<BoneData> rootBones,
        List<LocatorData> locators,
        Map<String, LocatorData> locatorsByUuid,
        Map<String, LocatorData> locatorsByName,
        List<TextureData> textures,
        List<BBAnimation> animations,
        boolean freeFormat
) {

    public ModelData(String id, String name, int textureWidth, int textureHeight,
                     Map<String, CubeData> cubes, List<BoneData> rootBones,
                     List<TextureData> textures) {
        this(id, name, textureWidth, textureHeight, cubes, Map.of(), rootBones,
                new ArrayList<>(), Map.of(), Map.of(), textures, new ArrayList<>(), false);
    }

    public BBAnimation getAnimation(String name) {
        for (BBAnimation anim : animations) {
            if (anim.getName().equals(name)) {
                return anim;
            }
        }
        return null;
    }

    public List<String> getAnimationNames() {
        return animations.stream().map(BBAnimation::getName).toList();
    }

    @Nullable
    public LocatorData getLocator(String name) {
        return locatorsByName.get(name);
    }

    @Nullable
    public LocatorData getLocatorByUuid(String uuid) {
        return locatorsByUuid.get(uuid);
    }

    public boolean hasLocator(String name) {
        return locatorsByName.containsKey(name);
    }

    public int findGlowmaskTextureIndex(int baseTextureIndex) {
        if (baseTextureIndex < 0 || baseTextureIndex >= textures.size()) {
            return -1;
        }

        String baseName = textures.get(baseTextureIndex).name();
        String glowmaskName = getGlowmaskName(baseName);

        for (int i = 0; i < textures.size(); i++) {
            if (textures.get(i).name().equalsIgnoreCase(glowmaskName)) {
                return i;
            }
        }
        return -1;
    }

    private String getGlowmaskName(String textureName) {
        String lowerName = textureName.toLowerCase();
        if (lowerName.endsWith(".png")) {
            return textureName.substring(0, textureName.length() - 4) + "_glowmask.png";
        }
        return textureName + "_glowmask";
    }
}

