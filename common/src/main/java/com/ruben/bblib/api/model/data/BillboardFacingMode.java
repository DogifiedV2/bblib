package com.ruben.bblib.api.model.data;

public enum BillboardFacingMode {
    LOOKAT,
    LOOKAT_Y,
    ROTATE,
    ROTATE_Y;

    public boolean yOnly() {
        return this == LOOKAT_Y || this == ROTATE_Y;
    }

    public static BillboardFacingMode fromSerialized(String value) {
        if (value == null || value.isBlank()) {
            return LOOKAT;
        }
        return switch (value) {
            case "lookat_y" -> LOOKAT_Y;
            case "rotate" -> ROTATE;
            case "rotate_y" -> ROTATE_Y;
            default -> LOOKAT;
        };
    }
}
