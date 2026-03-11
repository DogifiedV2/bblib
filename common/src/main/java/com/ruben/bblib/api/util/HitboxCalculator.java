package com.ruben.bblib.api.util;

import com.ruben.bblib.api.model.BoneData;
import com.ruben.bblib.api.model.CubeData;
import com.ruben.bblib.api.model.ModelData;
import com.ruben.bblib.api.model.Vec3f;

import java.util.Map;

public final class HitboxCalculator {

    private static final float BLOCKBENCH_UNITS_TO_BLOCKS = 1.0f / 16.0f;
    private static final float DEFAULT_EYE_HEIGHT_RATIO = 0.85f;

    private HitboxCalculator() {
    }

    public record HitboxResult(float width, float height, float eyeHeight) {
    }

    public static HitboxResult calculateFromModel(ModelData modelData) {
        if (modelData == null || modelData.rootBones().isEmpty()) {
            return new HitboxResult(0.6f, 1.8f, 1.53f);
        }

        BoundsAccumulator bounds = new BoundsAccumulator();

        for (BoneData rootBone : modelData.rootBones()) {
            processBoneHierarchy(rootBone, modelData.cubes(), Transform.IDENTITY, bounds);
        }

        if (!bounds.hasData()) {
            return new HitboxResult(0.6f, 1.8f, 1.53f);
        }

        float minX = bounds.minX * BLOCKBENCH_UNITS_TO_BLOCKS;
        float maxX = bounds.maxX * BLOCKBENCH_UNITS_TO_BLOCKS;
        float minY = bounds.minY * BLOCKBENCH_UNITS_TO_BLOCKS;
        float maxY = bounds.maxY * BLOCKBENCH_UNITS_TO_BLOCKS;
        float minZ = bounds.minZ * BLOCKBENCH_UNITS_TO_BLOCKS;
        float maxZ = bounds.maxZ * BLOCKBENCH_UNITS_TO_BLOCKS;

        float widthX = maxX - minX;
        float widthZ = maxZ - minZ;
        float width = Math.max(widthX, widthZ);
        float height = maxY - minY;

        width = Math.max(0.1f, width);
        height = Math.max(0.1f, height);

        float eyeHeight = height * DEFAULT_EYE_HEIGHT_RATIO;

        return new HitboxResult(width, height, eyeHeight);
    }

    private static void processBoneHierarchy(BoneData bone, Map<String, CubeData> cubeMap,
                                              Transform parentTransform, BoundsAccumulator bounds) {
        Transform boneTransform = parentTransform.chain(bone.origin(), bone.rotation());

        for (String cubeUuid : bone.cubeUuids()) {
            CubeData cube = cubeMap.get(cubeUuid);
            if (cube != null) {
                processCube(cube, boneTransform, bounds);
            }
        }

        for (BoneData child : bone.children()) {
            processBoneHierarchy(child, cubeMap, boneTransform, bounds);
        }
    }

    private static void processCube(CubeData cube, Transform boneTransform, BoundsAccumulator bounds) {
        if (!cube.visible()) {
            return;
        }

        Vec3f from = cube.from();
        Vec3f to = cube.to();
        float inflate = cube.inflate();

        float x0 = from.x() - inflate;
        float y0 = from.y() - inflate;
        float z0 = from.z() - inflate;
        float x1 = to.x() + inflate;
        float y1 = to.y() + inflate;
        float z1 = to.z() + inflate;

        Vec3f[] localVertices = {
                new Vec3f(x0, y0, z0), new Vec3f(x1, y0, z0),
                new Vec3f(x0, y1, z0), new Vec3f(x1, y1, z0),
                new Vec3f(x0, y0, z1), new Vec3f(x1, y0, z1),
                new Vec3f(x0, y1, z1), new Vec3f(x1, y1, z1)
        };

        Vec3f cubeOrigin = cube.origin();
        Vec3f cubeRotation = cube.rotation();

        for (Vec3f vertex : localVertices) {
            Vec3f rotatedVertex = vertex;
            if (!isZeroRotation(cubeRotation)) {
                rotatedVertex = rotateAroundPoint(vertex, cubeOrigin, cubeRotation);
            }
            Vec3f worldVertex = boneTransform.apply(rotatedVertex);
            bounds.expand(worldVertex);
        }
    }

    private static Vec3f rotateAroundPoint(Vec3f point, Vec3f pivot, Vec3f rotationDegrees) {
        Vec3f translated = point.subtract(pivot);
        Vec3f rotated = rotateXYZ(translated, rotationDegrees);
        return rotated.add(pivot);
    }

    private static Vec3f rotateXYZ(Vec3f vec, Vec3f rotationDegrees) {
        double radX = Math.toRadians(rotationDegrees.x());
        double radY = Math.toRadians(rotationDegrees.y());
        double radZ = Math.toRadians(rotationDegrees.z());

        float x = vec.x();
        float y = vec.y();
        float z = vec.z();

        if (radX != 0) {
            float cosX = (float) Math.cos(radX);
            float sinX = (float) Math.sin(radX);
            float newY = y * cosX - z * sinX;
            float newZ = y * sinX + z * cosX;
            y = newY;
            z = newZ;
        }

        if (radY != 0) {
            float cosY = (float) Math.cos(radY);
            float sinY = (float) Math.sin(radY);
            float newX = x * cosY + z * sinY;
            float newZ = -x * sinY + z * cosY;
            x = newX;
            z = newZ;
        }

        if (radZ != 0) {
            float cosZ = (float) Math.cos(radZ);
            float sinZ = (float) Math.sin(radZ);
            float newX = x * cosZ - y * sinZ;
            float newY = x * sinZ + y * cosZ;
            x = newX;
            y = newY;
        }

        return new Vec3f(x, y, z);
    }

    private static boolean isZeroRotation(Vec3f rotation) {
        return rotation.x() == 0 && rotation.y() == 0 && rotation.z() == 0;
    }

    private record Transform(Vec3f origin, Vec3f rotation) {
        static final Transform IDENTITY = new Transform(Vec3f.ZERO, Vec3f.ZERO);

        Transform chain(Vec3f childOrigin, Vec3f childRotation) {
            Vec3f newOrigin = apply(childOrigin);
            Vec3f newRotation = new Vec3f(
                    rotation.x() + childRotation.x(),
                    rotation.y() + childRotation.y(),
                    rotation.z() + childRotation.z()
            );
            return new Transform(newOrigin, newRotation);
        }

        Vec3f apply(Vec3f point) {
            if (isZeroRotation(rotation)) {
                return point;
            }
            return rotateAroundPoint(point, origin, rotation);
        }
    }

    private static class BoundsAccumulator {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        boolean hasData = false;

        void expand(Vec3f point) {
            hasData = true;
            minX = Math.min(minX, point.x());
            minY = Math.min(minY, point.y());
            minZ = Math.min(minZ, point.z());
            maxX = Math.max(maxX, point.x());
            maxY = Math.max(maxY, point.y());
            maxZ = Math.max(maxZ, point.z());
        }

        boolean hasData() {
            return hasData;
        }
    }
}

