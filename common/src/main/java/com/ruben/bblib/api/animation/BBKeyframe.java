package com.ruben.bblib.api.animation;

import com.ruben.bblib.api.model.Vec3f;
import com.ruben.bblib.api.molang.MolangContext;
import com.ruben.bblib.api.molang.MolangVec3;

public record BBKeyframe(
        float time,
        Channel channel,
        MolangVec3 molangValue,
        Interpolation interpolation
) implements Comparable<BBKeyframe> {

    public Vec3f value() {
        return molangValue.evaluate(new MolangContext());
    }

    public Vec3f evaluate(MolangContext context) {
        return molangValue.evaluate(context);
    }

    public boolean hasMolang() {
        return molangValue.hasMolang();
    }

    public enum Channel {
        ROTATION,
        POSITION,
        SCALE
    }

    public enum Interpolation {
        LINEAR,
        SMOOTH,
        CATMULLROM,
        STEP;

        public static Interpolation fromString(String str) {
            return switch (str.toLowerCase()) {
                case "smooth", "catmullrom" -> CATMULLROM;
                case "step" -> STEP;
                default -> LINEAR;
            };
        }
    }

    @Override
    public int compareTo(BBKeyframe other) {
        return Float.compare(this.time, other.time);
    }
}

