package com.ruben.bblib.api.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ruben.bblib.api.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.ParticleKeyframeData;
import com.ruben.bblib.api.animation.keyframe.event.data.SoundKeyframeData;
import com.ruben.bblib.api.molang.MolangParser;
import com.ruben.bblib.api.molang.MolangVec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class BBAnimationParser {

    private BBAnimationParser() {
    }

    public static List<BBAnimation> parseAnimations(JsonArray animationsArray, boolean flipSigns) {
        return parseAnimations(animationsArray, flipSigns, warning -> {
        });
    }

    public static List<BBAnimation> parseAnimations(JsonArray animationsArray, boolean flipSigns, Consumer<String> warningConsumer) {
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
                warningConsumer.accept("Failed to parse animation '" + animName + "': " + e.getMessage());
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
                String type = animatorObj.has("type") ? animatorObj.get("type").getAsString() : "bone";

                if ("effect".equalsIgnoreCase(type)) {
                    parseEffectAnimator(animatorObj, animation);
                    continue;
                }

                BBBoneAnimator boneAnimator = parseBoneAnimator(boneUuid, animatorObj, flipSigns);
                if (boneAnimator != null) {
                    animation.addBoneAnimator(boneAnimator);
                }
            }
        }
        animation.sortKeyframes();

        return animation;
    }

    private static void parseEffectAnimator(JsonObject animatorObj, BBAnimation animation) {
        if (!animatorObj.has("keyframes")) {
            return;
        }

        JsonArray keyframes = animatorObj.getAsJsonArray("keyframes");
        for (JsonElement keyframeElement : keyframes) {
            if (!keyframeElement.isJsonObject()) {
                continue;
            }

            JsonObject keyframe = keyframeElement.getAsJsonObject();
            String channel = getOptionalValueAsString(keyframe, "channel");

            switch (channel.toLowerCase()) {
                case "sound" -> parseEffectSoundKeyframe(keyframe, animation);
                case "particle" -> parseEffectParticleKeyframe(keyframe, animation);
                case "timeline", "instruction", "instructions", "script" ->
                        parseEffectInstructionKeyframe(keyframe, animation);
                default -> {
                }
            }
        }
    }

    private static void parseEffectSoundKeyframe(JsonObject keyframeObj, BBAnimation animation) {
        double time = getTime(keyframeObj);

        forEachDataPoint(keyframeObj, point -> {
            String effect = getFirstNonBlank(point, "effect", "sound");
            if (!effect.isBlank()) {
                animation.addSoundKeyframe(new SoundKeyframeData(time, effect));
            }
        });
    }

    private static void parseEffectParticleKeyframe(JsonObject keyframeObj, BBAnimation animation) {
        double time = getTime(keyframeObj);

        forEachDataPoint(keyframeObj, point -> {
            String effect = getFirstNonBlank(point, "effect", "particle");
            if (effect.isBlank()) {
                return;
            }

            animation.addParticleKeyframe(new ParticleKeyframeData(
                    time,
                    effect,
                    getValueAsString(point, "locator"),
                    getFirstNonBlank(point, "script", "pre_effect_script")
            ));
        });
    }

    private static void parseEffectInstructionKeyframe(JsonObject keyframeObj, BBAnimation animation) {
        List<String> instructions = new ArrayList<>();

        if (keyframeObj.has("data_points")) {
            JsonArray dataPoints = keyframeObj.getAsJsonArray("data_points");
            for (JsonElement dataPoint : dataPoints) {
                instructions.addAll(parseInstructions(dataPoint));
            }
        }

        if (!instructions.isEmpty()) {
            animation.addCustomInstructionKeyframe(new CustomInstructionKeyframeData(
                    getTime(keyframeObj),
                    instructions
            ));
        }
    }

    private static List<String> parseInstructions(JsonElement element) {
        List<String> instructions = new ArrayList<>();

        if (element == null || element.isJsonNull()) {
            return instructions;
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                instructions.add(primitive.getAsString());
            }
            return instructions;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String script = getFirstNonBlank(object, "script", "instruction", "instructions");
            if (!script.isBlank()) {
                instructions.add(script);
            }

            JsonElement nestedInstructions = object.get("instructions");
            if (nestedInstructions != null && nestedInstructions.isJsonArray()) {
                for (JsonElement instruction : nestedInstructions.getAsJsonArray()) {
                    if (instruction.isJsonPrimitive() && instruction.getAsJsonPrimitive().isString()) {
                        instructions.add(instruction.getAsString());
                    }
                }
            }
            return instructions;
        }

        if (element.isJsonArray()) {
            for (JsonElement instruction : element.getAsJsonArray()) {
                instructions.addAll(parseInstructions(instruction));
            }
        }

        return instructions;
    }

    private static void forEachDataPoint(JsonObject keyframeObj, Consumer<JsonObject> consumer) {
        if (!keyframeObj.has("data_points")) {
            return;
        }

        JsonArray dataPoints = keyframeObj.getAsJsonArray("data_points");
        for (JsonElement dataPointElement : dataPoints) {
            if (dataPointElement.isJsonObject()) {
                consumer.accept(dataPointElement.getAsJsonObject());
            }
        }
    }

    private static double getTime(JsonObject keyframeObj) {
        if (!keyframeObj.has("time")) {
            return 0;
        }

        try {
            return keyframeObj.get("time").getAsDouble();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String getFirstNonBlank(JsonObject obj, String... keys) {
        for (String key : keys) {
            String value = getOptionalValueAsString(obj, key);
            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private static String getOptionalValueAsString(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return "";
        }

        JsonElement element = obj.get(key);
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isNumber()) {
                return String.valueOf(element.getAsFloat());
            } else if (element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            }
        }

        return "";
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

