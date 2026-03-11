package com.ruben.bblib.api.renderer.layer;

import com.ruben.bblib.api.animatable.BBAnimatable;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BBRenderLayersContainer<T extends Entity & BBAnimatable> {

    private final List<BBRenderLayer<T>> renderLayers = new ArrayList<>();

    public void addLayer(BBRenderLayer<T> renderLayer) {
        renderLayers.add(renderLayer);
    }

    public List<BBRenderLayer<T>> getRenderLayers() {
        return Collections.unmodifiableList(renderLayers);
    }
}
