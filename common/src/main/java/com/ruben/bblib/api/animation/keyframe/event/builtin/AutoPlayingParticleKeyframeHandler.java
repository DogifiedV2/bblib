package com.ruben.bblib.api.animation.keyframe.event.builtin;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animation.keyframe.event.ParticleKeyframeEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class AutoPlayingParticleKeyframeHandler<T extends BBAnimatable>
        implements AnimationController.ParticleKeyframeHandler<T> {

    @Override
    public void handle(ParticleKeyframeEvent<T> event) {
        if (!(event.getAnimatable() instanceof Entity entity)) {
            return;
        }

        String effectId = event.getKeyframeData().effect();
        if (effectId == null || effectId.isBlank()) {
            return;
        }

        ParticleOptions particle = parseParticle(effectId);
        if (particle == null) {
            return;
        }

        Vec3 position = getParticlePosition(entity);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            minecraft.level.addParticle(particle, position.x, position.y, position.z, 0, 0, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private ParticleOptions parseParticle(String effectId) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(effectId);
            if (location == null) {
                return null;
            }

            ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(location);
            if (particleType instanceof ParticleOptions options) {
                return options;
            }

            if (particleType != null) {
                return (ParticleOptions) particleType;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private Vec3 getParticlePosition(Entity entity) {
        return new Vec3(
                entity.getX() + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                entity.getY() + entity.getBbHeight() * 0.5 + (entity.getRandom().nextDouble() - 0.5) * 0.5,
                entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * 0.5
        );
    }
}
