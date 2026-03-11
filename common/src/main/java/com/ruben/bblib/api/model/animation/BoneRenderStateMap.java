package com.ruben.bblib.api.model.animation;

import com.ruben.bblib.api.model.data.BoneData;
import com.ruben.bblib.api.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BoneRenderStateMap {

    private final Map<String, BoneRenderState> boneStates;

    private BoneRenderStateMap(Map<String, BoneRenderState> boneStates) {
        this.boneStates = boneStates;
    }

    public static BoneRenderStateMap create(ModelData modelData) {
        Map<String, BoneRenderState> states = new LinkedHashMap<>();
        for (BoneData rootBone : modelData.rootBones()) {
            collect(rootBone, states);
        }
        return new BoneRenderStateMap(states);
    }

    private static void collect(BoneData bone, Map<String, BoneRenderState> states) {
        states.put(bone.name(), new BoneRenderState(bone.name()));
        for (BoneData child : bone.children()) {
            collect(child, states);
        }
    }

    @Nullable
    public BoneRenderState getBone(String boneName) {
        return boneStates.get(boneName);
    }

    public boolean hasBone(String boneName) {
        return boneStates.containsKey(boneName);
    }

    public Collection<BoneRenderState> getBones() {
        return boneStates.values();
    }
}
