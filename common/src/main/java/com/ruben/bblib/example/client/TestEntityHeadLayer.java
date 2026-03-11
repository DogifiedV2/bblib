package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.animation.BoneRenderState;
import com.ruben.bblib.api.renderer.layer.BBRenderContext;
import com.ruben.bblib.api.renderer.layer.BBRenderLayer;
import com.ruben.bblib.example.TestEntity;

public final class TestEntityHeadLayer extends BBRenderLayer<TestEntity> {

    public TestEntityHeadLayer(TestEntityRenderer renderer) {
        super(renderer);
    }

    @Override
    public void preRender(BBRenderContext<TestEntity> renderContext) {
        BoneRenderState head = renderContext.getBoneRenderState("head");
        if (head == null) {
            return;
        }

        if (renderContext.getEntity().hurtTime > 0) {
            head.multiplyScale(1.08f, 1.08f, 1.08f);
        }
    }
}
