package com.ruben.bblib.internal.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ruben.bblib.internal.BBLibCommon;
import com.ruben.bblib.api.animation.BBAnimation;
import com.ruben.bblib.api.animation.BBAnimationParser;
import com.ruben.bblib.api.model.data.BillboardData;
import com.ruben.bblib.api.model.data.BillboardFacingMode;
import com.ruben.bblib.api.model.data.BoneData;
import com.ruben.bblib.api.model.data.CubeData;
import com.ruben.bblib.api.model.data.FaceData;
import com.ruben.bblib.api.model.data.LocatorData;
import com.ruben.bblib.api.model.data.ModelNodeKind;
import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.model.data.TextureData;
import com.ruben.bblib.api.model.data.UV;
import com.ruben.bblib.api.model.data.Vec2f;
import com.ruben.bblib.api.model.data.Vec3f;

import java.util.*;
import java.util.stream.Collectors;

public final class BBModelParser {

    private BBModelParser() {
    }

    public static ParseResult parse(String modelId, String jsonContent) {
        ParseResult result = new ParseResult(modelId);

        try {
            ModelData modelData = doParse(modelId, jsonContent, result);
            result.setModel(modelData);
        } catch (Exception e) {
            result.error("Failed to parse model: " + e.getMessage());
            BBLibCommon.LOGGER.error("Failed to parse bbmodel '{}'", modelId, e);
        }

        return result;
    }

    private static ModelData doParse(String modelId, String jsonContent, ParseResult result) {
        JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();

        int formatMajorVersion = getFormatMajorVersion(root);
        boolean hasGroupsArray = root.has("groups") && root.getAsJsonArray("groups").size() > 0;
        boolean isFreeFormat = isFreeFormat(root);
        boolean flipAnimationSigns = isFreeFormat && formatMajorVersion < 5;

        String name = root.has("name") ? root.get("name").getAsString() : modelId;

        int textureWidth = 64;
        int textureHeight = 64;
        if (root.has("resolution")) {
            JsonObject resolution = root.getAsJsonObject("resolution");
            textureWidth = resolution.get("width").getAsInt();
            textureHeight = resolution.get("height").getAsInt();
        } else {
            result.warn("No resolution defined, defaulting to 64x64");
        }

        List<TextureData> textures = parseTextures(root.getAsJsonArray("textures"), result);

        JsonArray elementsArray = root.getAsJsonArray("elements");
        if (elementsArray == null) {
            result.warn("No elements array found in model");
        }
        ParsedElements parsedElements = parseElements(elementsArray, result, isFreeFormat);
        Map<String, CubeData> cubes = parsedElements.cubes();
        Map<String, BillboardData> billboards = parsedElements.billboards();
        Map<String, AnimatableElementData> animatableElements = parsedElements.animatableElements();

        JsonArray outlinerArray = root.getAsJsonArray("outliner");
        if (outlinerArray == null) {
            result.warn("No outliner (bone hierarchy) found in model");
        }

        List<BoneData> rootBones;
        if (hasGroupsArray) {
            Map<String, JsonObject> groupMap = parseGroups(root.getAsJsonArray("groups"));
            rootBones = parseOutliner(outlinerArray, groupMap, animatableElements, billboards);
        } else {
            rootBones = parseLegacyOutliner(outlinerArray, animatableElements, billboards);
        }

        List<BBAnimation> animations = BBAnimationParser.parseAnimations(root.getAsJsonArray("animations"), flipAnimationSigns, result::warn);
        ParsedLocators parsedLocators = collectLocators(rootBones, result);

        if (!animations.isEmpty()) {
            BBLibCommon.LOGGER.info("Loaded {} animations for model {} (format: v{}, {})",
                    animations.size(), modelId, formatMajorVersion, isFreeFormat ? "free" : "other");
        }

        return new ModelData(modelId, name, textureWidth, textureHeight, cubes, billboards, rootBones,
                parsedLocators.locators(), parsedLocators.locatorsByUuid(), parsedLocators.locatorsByName(),
                textures, animations, isFreeFormat);
    }

    private static int getFormatMajorVersion(JsonObject root) {
        if (!root.has("meta")) {
            return 4;
        }
        JsonObject meta = root.getAsJsonObject("meta");
        if (!meta.has("format_version")) {
            return 4;
        }
        String version = meta.get("format_version").getAsString();
        String[] versionSplit = version.split("\\.");
        if (versionSplit.length == 0) {
            return 4;
        }
        try {
            return Integer.parseInt(versionSplit[0]);
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    private static boolean isFreeFormat(JsonObject root) {
        if (!root.has("meta")) {
            return true;
        }
        JsonObject meta = root.getAsJsonObject("meta");
        if (!meta.has("model_format")) {
            return true;
        }
        return "free".equals(meta.get("model_format").getAsString());
    }

    private static ParsedElements parseElements(JsonArray elements, ParseResult result, boolean isFreeFormat) {
        Map<String, CubeData> cubes = new HashMap<>();
        Map<String, BillboardData> billboards = new HashMap<>();
        Map<String, AnimatableElementData> animatableElements = new HashMap<>();
        if (elements == null) {
            return new ParsedElements(cubes, billboards, animatableElements);
        }

        int skippedUnsupported = 0;
        List<String> degenerateFaceCubes = new ArrayList<>();

        for (JsonElement element : elements) {
            JsonObject obj = element.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "cube";
            switch (type) {
                case "cube" -> {
                }
                case "billboard" -> {
                    Optional<BillboardData> billboard = parseBillboardElement(obj);
                    if (billboard.isPresent()) {
                        BillboardData billboardData = billboard.get();
                        billboards.put(billboardData.uuid(), billboardData);
                    } else {
                        skippedUnsupported++;
                    }
                    continue;
                }
                default -> {
                    Optional<AnimatableElementData> animatableElement = parseAnimatableElement(obj, type);
                    if (animatableElement.isPresent()) {
                        AnimatableElementData animatable = animatableElement.get();
                        animatableElements.put(animatable.uuid(), animatable);
                    } else {
                        skippedUnsupported++;
                    }
                    continue;
                }
            }

            boolean visible = !obj.has("visibility") || obj.get("visibility").getAsBoolean();
            if (!visible && !isFreeFormat) {
                continue;
            }

            String uuid = obj.get("uuid").getAsString();
            String name = obj.has("name") ? obj.get("name").getAsString() : uuid;

            Vec3f from = parseVec3f(obj.getAsJsonArray("from"));
            Vec3f to = parseVec3f(obj.getAsJsonArray("to"));
            Vec3f origin = obj.has("origin") ? parseVec3f(obj.getAsJsonArray("origin")) : Vec3f.ZERO;
            Vec3f rotation = obj.has("rotation") ? parseVec3f(obj.getAsJsonArray("rotation")) : Vec3f.ZERO;
            float inflate = obj.has("inflate") ? obj.get("inflate").getAsFloat() : 0;

            Map<CubeData.Face, FaceData> faces = parseFaces(obj.getAsJsonObject("faces"), name, degenerateFaceCubes);

            cubes.put(uuid, new CubeData(uuid, name, visible, from, to, origin, rotation, inflate, faces));
        }

        if (skippedUnsupported > 0) {
            result.warn("Skipped " + skippedUnsupported + " unsupported element(s)");
        }

        if (!degenerateFaceCubes.isEmpty()) {
            Set<String> uniqueCubes = new LinkedHashSet<>(degenerateFaceCubes);
            int count = degenerateFaceCubes.size();
            String examples = uniqueCubes.stream().limit(3).collect(Collectors.joining(", "));
            String suffix = uniqueCubes.size() > 3 ? " (+" + (uniqueCubes.size() - 3) + " more)" : "";
            result.warn("Skipped " + count + " degenerate face(s) (zero-sized UV) on cubes: " + examples + suffix);
        }

        return new ParsedElements(cubes, billboards, animatableElements);
    }

    private static Optional<BillboardData> parseBillboardElement(JsonObject element) {
        if (!element.has("uuid")) {
            return Optional.empty();
        }

        String uuid = element.get("uuid").getAsString();
        String name = element.has("name") ? element.get("name").getAsString() : uuid;
        boolean visible = !element.has("visibility") || element.get("visibility").getAsBoolean();
        Vec3f origin = element.has("origin") ? parseVec3f(element.getAsJsonArray("origin")) : Vec3f.ZERO;
        Vec2f size = element.has("size") ? parseVec2f(element.getAsJsonArray("size")) : new Vec2f(2.0f, 2.0f);
        Vec2f offset = element.has("offset") ? parseVec2f(element.getAsJsonArray("offset")) : new Vec2f(0.0f, 0.0f);
        BillboardFacingMode facingMode = BillboardFacingMode.fromSerialized(
                element.has("facing_mode") ? element.get("facing_mode").getAsString() : null
        );

        FaceData frontFace = null;
        JsonObject facesObj = element.getAsJsonObject("faces");
        if (facesObj != null && facesObj.has("front")) {
            frontFace = parseBillboardFace(facesObj.getAsJsonObject("front"));
        }

        return Optional.of(new BillboardData(uuid, name, visible, origin, size, offset, facingMode, frontFace));
    }

    private static FaceData parseBillboardFace(JsonObject faceObj) {
        if (faceObj == null || !faceObj.has("texture") || faceObj.get("texture").isJsonNull()) {
            return null;
        }

        JsonArray uvArray = faceObj.getAsJsonArray("uv");
        UV uv = uvArray != null && uvArray.size() >= 4
                ? new UV(
                uvArray.get(0).getAsFloat(),
                uvArray.get(1).getAsFloat(),
                uvArray.get(2).getAsFloat(),
                uvArray.get(3).getAsFloat()
        )
                : new UV(0, 0, 0, 0);
        int textureIndex = parseTextureIndex(faceObj.get("texture"));
        int rotation = faceObj.has("rotation") && !faceObj.get("rotation").isJsonNull()
                ? faceObj.get("rotation").getAsInt()
                : 0;
        return textureIndex < 0 ? null : new FaceData(uv, textureIndex, rotation);
    }

    private static int parseTextureIndex(JsonElement textureElement) {
        if (textureElement == null || textureElement.isJsonNull()) {
            return -1;
        }
        if (textureElement.isJsonPrimitive() && textureElement.getAsJsonPrimitive().isBoolean()) {
            return textureElement.getAsBoolean() ? 0 : -1;
        }
        try {
            return textureElement.getAsInt();
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static Map<CubeData.Face, FaceData> parseFaces(JsonObject facesObj, String cubeName,
                                                            List<String> degenerateFaceCubes) {
        Map<CubeData.Face, FaceData> faces = new EnumMap<>(CubeData.Face.class);
        if (facesObj == null) {
            return faces;
        }

        for (CubeData.Face face : CubeData.Face.values()) {
            String faceKey = face.name().toLowerCase();
            if (facesObj.has(faceKey)) {
                JsonObject faceObj = facesObj.getAsJsonObject(faceKey);

                if (!faceObj.has("texture") || faceObj.get("texture").isJsonNull()) {
                    continue;
                }

                JsonArray uvArray = faceObj.getAsJsonArray("uv");
                float u1 = uvArray.get(0).getAsFloat();
                float v1 = uvArray.get(1).getAsFloat();
                float u2 = uvArray.get(2).getAsFloat();
                float v2 = uvArray.get(3).getAsFloat();

                if (isSimilar(u1, u2) || isSimilar(v1, v2)) {
                    degenerateFaceCubes.add(cubeName);
                    continue;
                }

                UV uv = new UV(u1, v1, u2, v2);
                int textureIndex = faceObj.get("texture").getAsInt();
                int rotation = faceObj.has("rotation") && !faceObj.get("rotation").isJsonNull()
                        ? faceObj.get("rotation").getAsInt()
                        : 0;
                faces.put(face, new FaceData(uv, textureIndex, rotation));
            }
        }

        return faces;
    }

    private static Map<String, JsonObject> parseGroups(JsonArray groups) {
        Map<String, JsonObject> groupMap = new HashMap<>();
        if (groups == null) {
            return groupMap;
        }

        for (JsonElement element : groups) {
            if (element.isJsonObject()) {
                JsonObject group = element.getAsJsonObject();
                String uuid = group.get("uuid").getAsString();
                groupMap.put(uuid, group);
            }
        }

        return groupMap;
    }

    private static List<BoneData> parseOutliner(JsonArray outliner, Map<String, JsonObject> groupMap,
                                                Map<String, AnimatableElementData> animatableElements,
                                                Map<String, BillboardData> billboards) {
        List<BoneData> bones = new ArrayList<>();
        if (outliner == null) {
            return bones;
        }

        for (JsonElement element : outliner) {
            BoneData bone = parseOutlinerEntry(element, groupMap, animatableElements, billboards);
            if (bone != null) {
                bones.add(bone);
            }
        }

        return bones;
    }

    private static BoneData parseOutlinerEntry(JsonElement element, Map<String, JsonObject> groupMap,
                                               Map<String, AnimatableElementData> animatableElements,
                                               Map<String, BillboardData> billboards) {
        if (element.isJsonPrimitive()) {
            return parseAnimatablePrimitive(element, animatableElements);
        }

        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject outlinerObj = element.getAsJsonObject();
        String uuid = outlinerObj.get("uuid").getAsString();

        JsonObject groupData = groupMap.get(uuid);
        String name;
        Vec3f origin;
        Vec3f rotation;

        if (groupData != null) {
            name = groupData.has("name") ? groupData.get("name").getAsString() : uuid;
            origin = groupData.has("origin") ? parseVec3f(groupData.getAsJsonArray("origin")) : Vec3f.ZERO;
            rotation = groupData.has("rotation") ? parseVec3f(groupData.getAsJsonArray("rotation")) : Vec3f.ZERO;
        } else {
            name = outlinerObj.has("name") ? outlinerObj.get("name").getAsString() : uuid;
            origin = outlinerObj.has("origin") ? parseVec3f(outlinerObj.getAsJsonArray("origin")) : Vec3f.ZERO;
            rotation = outlinerObj.has("rotation") ? parseVec3f(outlinerObj.getAsJsonArray("rotation")) : Vec3f.ZERO;
        }

        List<String> cubeUuids = new ArrayList<>();
        List<String> billboardUuids = new ArrayList<>();
        List<BoneData> children = new ArrayList<>();

        if (outlinerObj.has("children")) {
            JsonArray childrenArray = outlinerObj.getAsJsonArray("children");
            for (JsonElement child : childrenArray) {
                if (child.isJsonPrimitive()) {
                    BoneData animatableChild = parseAnimatablePrimitive(child, animatableElements);
                    if (animatableChild != null) {
                        children.add(animatableChild);
                    } else if (billboards.containsKey(child.getAsString())) {
                        billboardUuids.add(child.getAsString());
                    } else {
                        cubeUuids.add(child.getAsString());
                    }
                } else if (child.isJsonObject()) {
                    BoneData childBone = parseOutlinerEntry(child, groupMap, animatableElements, billboards);
                    if (childBone != null) {
                        children.add(childBone);
                    }
                }
            }
        }

        return new BoneData(uuid, name, origin, rotation, cubeUuids, billboardUuids, children, ModelNodeKind.BONE);
    }

    private static List<BoneData> parseLegacyOutliner(JsonArray outliner,
                                                      Map<String, AnimatableElementData> animatableElements,
                                                      Map<String, BillboardData> billboards) {
        List<BoneData> bones = new ArrayList<>();
        if (outliner == null) {
            return bones;
        }

        for (JsonElement element : outliner) {
            BoneData bone = parseLegacyOutlinerEntry(element, animatableElements, billboards);
            if (bone != null) {
                bones.add(bone);
            }
        }

        return bones;
    }

    private static BoneData parseLegacyOutlinerEntry(JsonElement element,
                                                     Map<String, AnimatableElementData> animatableElements,
                                                     Map<String, BillboardData> billboards) {
        if (element.isJsonPrimitive()) {
            return parseAnimatablePrimitive(element, animatableElements);
        }

        if (!element.isJsonObject()) {
            return null;
        }

        JsonObject obj = element.getAsJsonObject();
        String uuid = obj.get("uuid").getAsString();
        String name = obj.has("name") ? obj.get("name").getAsString() : uuid;
        Vec3f origin = obj.has("origin") ? parseVec3f(obj.getAsJsonArray("origin")) : Vec3f.ZERO;
        Vec3f rotation = obj.has("rotation") ? parseVec3f(obj.getAsJsonArray("rotation")) : Vec3f.ZERO;

        List<String> cubeUuids = new ArrayList<>();
        List<String> billboardUuids = new ArrayList<>();
        List<BoneData> children = new ArrayList<>();

        if (obj.has("children")) {
            JsonArray childrenArray = obj.getAsJsonArray("children");
            for (JsonElement child : childrenArray) {
                if (child.isJsonPrimitive()) {
                    BoneData animatableChild = parseAnimatablePrimitive(child, animatableElements);
                    if (animatableChild != null) {
                        children.add(animatableChild);
                    } else if (billboards.containsKey(child.getAsString())) {
                        billboardUuids.add(child.getAsString());
                    } else {
                        cubeUuids.add(child.getAsString());
                    }
                } else if (child.isJsonObject()) {
                    BoneData childBone = parseLegacyOutlinerEntry(child, animatableElements, billboards);
                    if (childBone != null) {
                        children.add(childBone);
                    }
                }
            }
        }

        return new BoneData(uuid, name, origin, rotation, cubeUuids, billboardUuids, children, ModelNodeKind.BONE);
    }

    private static Optional<AnimatableElementData> parseAnimatableElement(JsonObject element, String type) {
        ModelNodeKind kind = getAnimatableKind(type);
        if (kind == null) {
            return Optional.empty();
        }
        if (!element.has("uuid")) {
            return Optional.empty();
        }

        String uuid = element.get("uuid").getAsString();
        String name = element.has("name") ? element.get("name").getAsString() : uuid;

        Vec3f origin;
        if (element.has("origin")) {
            origin = parseVec3f(element.getAsJsonArray("origin"));
        } else if (element.has("position")) {
            origin = parseVec3f(element.getAsJsonArray("position"));
        } else {
            origin = Vec3f.ZERO;
        }

        Vec3f rotation = element.has("rotation") ? parseVec3f(element.getAsJsonArray("rotation")) : Vec3f.ZERO;
        return Optional.of(new AnimatableElementData(uuid, name, origin, rotation, kind));
    }

    private static BoneData parseAnimatablePrimitive(JsonElement element, Map<String, AnimatableElementData> animatableElements) {
        if (!element.isJsonPrimitive()) {
            return null;
        }
        AnimatableElementData animatable = animatableElements.get(element.getAsString());
        if (animatable == null) {
            return null;
        }
        return new BoneData(
                animatable.uuid(),
                animatable.name(),
                animatable.origin(),
                animatable.rotation(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                animatable.kind()
        );
    }

    private static ModelNodeKind getAnimatableKind(String type) {
        return switch (type) {
            case "locator" -> ModelNodeKind.LOCATOR;
            case "null_object" -> ModelNodeKind.NULL_OBJECT;
            case "camera" -> ModelNodeKind.CAMERA;
            default -> null;
        };
    }

    private static ParsedLocators collectLocators(List<BoneData> rootBones, ParseResult result) {
        List<LocatorData> locators = new ArrayList<>();
        Map<String, LocatorData> locatorsByUuid = new LinkedHashMap<>();
        Map<String, LocatorData> locatorsByName = new LinkedHashMap<>();

        for (BoneData rootBone : rootBones) {
            collectLocators(rootBone, null, locators, locatorsByUuid, locatorsByName, result);
        }

        return new ParsedLocators(locators, locatorsByUuid, locatorsByName);
    }

    private static void collectLocators(BoneData node, BoneData parent, List<LocatorData> locators,
                                        Map<String, LocatorData> locatorsByUuid,
                                        Map<String, LocatorData> locatorsByName,
                                        ParseResult result) {
        if (node.kind().isLocatorLike()) {
            LocatorData locator = new LocatorData(
                    node.uuid(),
                    node.name(),
                    node.origin(),
                    node.rotation(),
                    node.kind(),
                    parent != null ? parent.uuid() : null,
                    parent != null ? parent.name() : null
            );
            locators.add(locator);
            locatorsByUuid.put(locator.uuid(), locator);

            LocatorData previous = locatorsByName.putIfAbsent(locator.name(), locator);
            if (previous != null && !previous.uuid().equals(locator.uuid())) {
                result.warn("Duplicate locator name '" + locator.name() + "' detected; transform lookup will use the first definition");
            }
        }

        for (BoneData child : node.children()) {
            collectLocators(child, node, locators, locatorsByUuid, locatorsByName, result);
        }
    }

    private static List<TextureData> parseTextures(JsonArray textures, ParseResult result) {
        List<TextureData> textureList = new ArrayList<>();
        if (textures == null) {
            return textureList;
        }

        for (JsonElement element : textures) {
            JsonObject obj = element.getAsJsonObject();
            String id = obj.has("id") ? obj.get("id").getAsString() : obj.get("uuid").getAsString();
            String name = obj.has("name") ? obj.get("name").getAsString() : "texture";
            int width = obj.has("width") ? obj.get("width").getAsInt() : 64;
            int height = obj.has("height") ? obj.get("height").getAsInt() : 64;
            int uvWidth = obj.has("uv_width") ? obj.get("uv_width").getAsInt() : width;
            int uvHeight = obj.has("uv_height") ? obj.get("uv_height").getAsInt() : height;
            int frameTime = obj.has("frame_time") ? obj.get("frame_time").getAsInt() : 1;
            String frameOrderType = obj.has("frame_order_type") ? obj.get("frame_order_type").getAsString() : "loop";
            String frameOrder = obj.has("frame_order") ? obj.get("frame_order").getAsString() : "";
            boolean frameInterpolate = obj.has("frame_interpolate") && obj.get("frame_interpolate").getAsBoolean();

            byte[] imageData = new byte[0];
            if (obj.has("source")) {
                String source = obj.get("source").getAsString();
                if (source.startsWith("data:image/png;base64,")) {
                    String base64Data = source.substring("data:image/png;base64,".length());
                    imageData = Base64.getDecoder().decode(base64Data);
                } else {
                    result.warn("Texture '" + name + "' has unsupported source format");
                }
            } else {
                result.warn("Texture '" + name + "' has no embedded image data");
            }

            textureList.add(new TextureData(
                    id,
                    name,
                    width,
                    height,
                    uvWidth,
                    uvHeight,
                    frameTime,
                    frameOrderType,
                    frameOrder,
                    frameInterpolate,
                    imageData
            ));
        }

        return textureList;
    }

    private static Vec3f parseVec3f(JsonArray array) {
        if (array == null || array.size() < 3) {
            return Vec3f.ZERO;
        }
        return new Vec3f(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat()
        );
    }

    private static Vec2f parseVec2f(JsonArray array) {
        if (array == null || array.size() < 2) {
            return new Vec2f(0, 0);
        }
        return new Vec2f(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat()
        );
    }

    private static boolean isSimilar(float a, float b) {
        return Math.abs(a - b) < 0.001f;
    }

    private record AnimatableElementData(
            String uuid,
            String name,
            Vec3f origin,
            Vec3f rotation,
            ModelNodeKind kind
    ) {
    }

    private record ParsedElements(
            Map<String, CubeData> cubes,
            Map<String, BillboardData> billboards,
            Map<String, AnimatableElementData> animatableElements
    ) {
    }

    private record ParsedLocators(
            List<LocatorData> locators,
            Map<String, LocatorData> locatorsByUuid,
            Map<String, LocatorData> locatorsByName
    ) {
    }
}

