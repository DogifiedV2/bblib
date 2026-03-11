package com.ruben.bblib.api.model;

import com.ruben.bblib.api.animation.BBAnimation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ModelData(
        String id,
        String name,
        int textureWidth,
        int textureHeight,
        Map<String, CubeData> cubes,
        List<BoneData> rootBones,
        List<TextureData> textures,
        List<BBAnimation> animations,
        boolean freeFormat
) {

    public ModelData(String id, String name, int textureWidth, int textureHeight,
                     Map<String, CubeData> cubes, List<BoneData> rootBones,
                     List<TextureData> textures) {
        this(id, name, textureWidth, textureHeight, cubes, rootBones, textures, new ArrayList<>(), false);
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

