package com.ruben.bblib.api.renderer.layer;

import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.model.data.BoneData;
import com.ruben.bblib.api.renderer.BBEntityRenderer;
import net.minecraft.world.entity.Entity;

public abstract class BBRenderLayer<T extends Entity & BBAnimatable> {

    private final BBEntityRenderer<T> renderer;

    protected BBRenderLayer(BBEntityRenderer<T> renderer) {
        this.renderer = renderer;
    }

    public BBEntityRenderer<T> getRenderer() {
        return renderer;
    }

    public void preRender(BBRenderContext<T> renderContext) {
    }

    public void render(BBRenderContext<T> renderContext) {
    }

    public void renderForBone(BBRenderContext<T> renderContext, BoneData bone) {
    }
}
