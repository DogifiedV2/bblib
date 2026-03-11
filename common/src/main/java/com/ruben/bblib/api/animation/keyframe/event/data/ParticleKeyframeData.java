package com.ruben.bblib.api.animation.keyframe.event.data;

public record ParticleKeyframeData(double startTime, String effect, String locator,
                                   String preEffectScript) implements KeyframeData {
}
