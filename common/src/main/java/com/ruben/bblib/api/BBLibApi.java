package com.ruben.bblib.api;

import com.ruben.bblib.api.animatable.AnimatableInstanceCache;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.model.ModelData;
import com.ruben.bblib.api.util.HitboxCalculator;

public final class BBLibApi {

    private BBLibApi() {
    }

    public static AnimatableInstanceCache createCache(BBAnimatable animatable) {
        return new AnimatableInstanceCache(animatable);
    }

    public static HitboxCalculator.HitboxResult calculateHitbox(ModelData modelData) {
        return HitboxCalculator.calculateFromModel(modelData);
    }
}
