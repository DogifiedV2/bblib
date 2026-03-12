package com.ruben.bblib.example.client;

import com.ruben.bblib.api.model.animation.BoneRenderState;
import com.ruben.bblib.api.model.transform.ResolvedNodeTransform;
import com.ruben.bblib.api.renderer.layer.BBRenderContext;
import com.ruben.bblib.api.renderer.layer.BBRenderLayer;
import com.ruben.bblib.example.TestEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class TestEntityHeadLayer extends BBRenderLayer<TestEntity> {

    private final Map<Integer, Integer> lastLocatorParticleTick = new HashMap<>();

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

    @Override
    public void render(BBRenderContext<TestEntity> renderContext) {
        ResolvedNodeTransform locator = renderContext.getLocatorTransform("locator");
        if (locator == null || Minecraft.getInstance().level == null) {
            return;
        }

        int entityId = renderContext.getEntity().getId();
        int currentTick = (int) renderContext.getCurrentTick();
        Integer lastTick = lastLocatorParticleTick.get(entityId);
        if (lastTick != null && lastTick == currentTick) {
            return;
        }

        lastLocatorParticleTick.put(entityId, currentTick);
        if ((currentTick & 1) != 0) {
            return;
        }

        Minecraft.getInstance().level.addParticle(
                ParticleTypes.END_ROD,
                locator.worldPosition().x,
                locator.worldPosition().y,
                locator.worldPosition().z,
                0.0,
                0.0,
                0.0
        );
    }
}
