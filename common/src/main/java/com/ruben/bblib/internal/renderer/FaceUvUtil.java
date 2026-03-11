package com.ruben.bblib.internal.renderer;

import com.ruben.bblib.api.model.data.CubeData;
import com.ruben.bblib.api.model.data.UV;

public final class FaceUvUtil {

    private FaceUvUtil() {
    }

    public static float[] buildFaceVertexUvs(CubeData.Face face, UV uv, int rotation, float texWidth, float texHeight) {
        float u1 = uv.u1() / texWidth;
        float v1 = uv.v1() / texHeight;
        float u2 = uv.u2() / texWidth;
        float v2 = uv.v2() / texHeight;

        int rotationSteps = normalizeRotationSteps(rotation);
        float[] vertexUvs = new float[8];

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            int baseIndex = getBaseUvIndex(face, vertexIndex);
            int shiftedIndex = (baseIndex + rotationSteps) % 4;

            float u = (shiftedIndex == 0 || shiftedIndex == 1) ? u1 : u2;
            float v = (shiftedIndex == 0 || shiftedIndex == 3) ? v1 : v2;

            vertexUvs[vertexIndex * 2] = u;
            vertexUvs[vertexIndex * 2 + 1] = v;
        }

        return vertexUvs;
    }

    private static int normalizeRotationSteps(int rotation) {
        int normalized = Math.floorMod(rotation, 360);
        if (normalized % 90 != 0) {
            return 0;
        }
        return normalized / 90;
    }

    private static int getBaseUvIndex(CubeData.Face face, int vertexIndex) {
        return switch (face) {
            case NORTH, EAST, DOWN -> switch (vertexIndex) {
                case 0 -> 1;
                case 1 -> 2;
                case 2 -> 3;
                default -> 0;
            };
            case SOUTH -> switch (vertexIndex) {
                case 0 -> 1;
                case 1 -> 0;
                case 2 -> 3;
                default -> 2;
            };
            case WEST -> switch (vertexIndex) {
                case 0 -> 2;
                case 1 -> 1;
                case 2 -> 0;
                default -> 3;
            };
            case UP -> switch (vertexIndex) {
                case 0 -> 0;
                case 1 -> 3;
                case 2 -> 2;
                default -> 1;
            };
        };
    }
}

