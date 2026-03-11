package com.ruben.bblib.api.animation.keyframe.event.builtin;

import com.ruben.bblib.api.animatable.AnimationController;
import com.ruben.bblib.api.animatable.BBAnimatable;
import com.ruben.bblib.api.animation.keyframe.event.SoundKeyframeEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;

@Environment(EnvType.CLIENT)
public class AutoPlayingSoundKeyframeHandler<T extends BBAnimatable>
        implements AnimationController.SoundKeyframeHandler<T> {

    @Override
    public void handle(SoundKeyframeEvent<T> event) {
        if (!(event.getAnimatable() instanceof Entity entity)) {
            return;
        }

        String[] segments = event.getKeyframeData().effect().split("\\|");
        ResourceLocation soundId = ResourceLocation.tryParse(segments[0]);
        if (soundId == null) {
            return;
        }

        SoundEvent soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundId);
        if (soundEvent == null) {
            return;
        }

        float volume = segments.length > 1 ? parseFloat(segments[1], 1.0f) : 1.0f;
        float pitch = segments.length > 2 ? parseFloat(segments[2], 1.0f) : 1.0f;
        SoundSource soundSource = entity instanceof Enemy ? SoundSource.HOSTILE : entity.getSoundSource();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            minecraft.level.playLocalSound(entity.getX(), entity.getY(), entity.getZ(),
                    soundEvent, soundSource, volume, pitch, false);
        }
    }

    private float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
