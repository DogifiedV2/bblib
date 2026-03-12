package com.ruben.bblib.api.model.data;

public enum ModelNodeKind {
    BONE,
    LOCATOR,
    NULL_OBJECT,
    CAMERA;

    public boolean isLocatorLike() {
        return this != BONE;
    }
}
