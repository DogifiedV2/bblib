package com.ruben.bblib.api.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ruben.bblib.api.molang.MolangParser;
import com.ruben.bblib.api.molang.MolangVec3;
import com.ruben.bblib.internal.parser.ParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BBAnimationParser {

    private BBAnimationParser() {
    }

    public static List<BBAnimation> parseAnimations(JsonArray animationsArray, boolean flipSigns) {
        return parseAnimations(animationsArray, flipSigns, new ParseResult("animations"));
    }

    public static List<BBAnimation> parseAnimations(JsonArray animationsArray, boolean flipSigns, ParseResult result) {
        List<BBAnimation> animations = new ArrayList<>();

        if (animationsArray == null) {
            return animations;
        }

        for (JsonElement element : animationsArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            String animName = "unnamed";
            try {
                JsonObject obj = element.getAsJsonObject();
                animName = obj.has("name") ? obj.get("name").getAsString() : "unnamed";
                BBAnimation animation = parseAnimation(obj, flipSigns);
                if (animation != null) {
                    animations.add(animation);
                }
            } catch (Exception e) {
                result.warn("Failed to parse animation '" + animName + "': " + e.getMessage());
            }
        }

        return animations;
    }

    private static BBAnimation parseAnimation(JsonObject animObj, boolean flipSigns) {
        String uuid = animObj.has("uuid") ? animObj.get("uuid").getAsString() : "";
        String name = animObj.has("name") ? animObj.get("name").getAsString() : "unnamed";

        String loopStr = animObj.has("loop") ? animObj.get("loop").getAsString() : "once";
        BBAnimation.LoopMode loopMode = BBAnimation.LoopMode.fromString(loopStr);

        float length = animObj.has("length") ? animObj.get("length").getAsFloat() : 1.0f;

        BBAnimation animation = new BBAnimation(uuid, name, loopMode, length);

        if (animObj.has("animators")) {
            JsonObject animators = animObj.getAsJsonObject("animators");
            for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                String boneUuid = entry.getKey();
                JsonObject animatorObj = entry.getValue().getAsJsonObject();

                BBBoneAnimator boneAnimator = parseBoneAnimator(boneUuid, animatorObj, flipSigns);
                if (boneAnimator != null) {
                    animation.addBoneAnimator(boneAnimator);
                }
            }
        }

        return animation;
    }

    private static BBBoneAnimator parseBoneAnimator(String boneUuid, JsonObject animatorObj, boolean flipSigns) {
        String boneName = animatorObj.has("name") ? animatorObj.get("name").getAsString() : boneUuid;
        String type = animatorObj.has("type") ? animatorObj.get("type").getAsString() : "bone";

        if (!"bone".equals(type)) {
            return null;
        }

        BBBoneAnimator animator = new BBBoneAnimator(boneUuid, boneName);

        if (animatorObj.has("keyframes")) {
            JsonArray keyframesArray = animatorObj.getAsJsonArray("keyframes");
            for (JsonElement kfElement : keyframesArray) {
                if (!kfElement.isJsonObject()) {
                    continue;
                }

                BBKeyframe keyframe = parseKeyframe(kfElement.getAsJsonObject(), flipSigns);
                if (keyframe != null) {
                    animator.addKeyframe(keyframe);
                }
            }
        }

        animator.sortKeyframes();
        return animator;
    }

    private static BBKeyframe parseKeyframe(JsonObject kfObj, boolean flipSigns) {
        String channelStr = kfObj.has("channel") ? kfObj.get("channel").getAsString() : "rotation";
        BBKeyframe.Channel channel = switch (channelStr.toLowerCase()) {
            case "position" -> BBKeyframe.Channel.POSITION;
            case "scale" -> BBKeyframe.Channel.SCALE;
            default -> BBKeyframe.Channel.ROTATION;
        };

        float time = kfObj.has("time") ? kfObj.get("time").getAsFloat() : 0;

        String interpolationStr = kfObj.has("interpolation") ? kfObj.get("interpolation").getAsString() : "linear";
        BBKeyframe.Interpolation interpolation = BBKeyframe.Interpolation.fromString(interpolationStr);

        MolangVec3 value = MolangVec3.zero();
        if (kfObj.has("data_points")) {
            JsonArray dataPoints = kfObj.getAsJsonArray("data_points");
            if (!dataPoints.isEmpty()) {
                JsonObject point = dataPoints.get(0).getAsJsonObject();
                value = parseDataPoint(point, channel, flipSigns);
            }
        }

        return new BBKeyframe(time, channel, value, interpolation);
    }

    private static MolangVec3 parseDataPoint(JsonObject point, BBKeyframe.Channel channel, boolean flipSigns) {
        String x = getValueAsString(point, "x");
        String y = getValueAsString(point, "y");
        String z = getValueAsString(point, "z");

        if (flipSigns) {
            switch (channel) {
                case POSITION -> x = flipSign(x);
                case ROTATION -> {
                    x = flipSign(x);
                    y = flipSign(y);
                }
                default -> {}
            }
        }

        boolean hasMolang = MolangParser.isMolangExpression(x) ||
                MolangParser.isMolangExpression(y) ||
                MolangParser.isMolangExpression(z);

        if (hasMolang) {
            return new MolangVec3(x, y, z);
        } else {
            return new MolangVec3(parseFloat(x), parseFloat(y), parseFloat(z));
        }
    }

    private static String flipSign(String value) {
        try {
            double d = Double.parseDouble(value);
            return Double.toString(-d);
        } catch (NumberFormatException e) {
            return "-(" + value + ")";
        }
    }

    private static String getValueAsString(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return "0";
        }

        JsonElement element = obj.get(key);
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return String.valueOf(element.getAsFloat());
            } else if (element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            }
        }
        return "0";
    }

    private static float parseFloat(String str) {
        if (str == null || str.isBlank()) {
            return 0;
        }
        try {
            return Float.parseFloat(str.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

