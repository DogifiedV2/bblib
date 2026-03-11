package com.ruben.bblib.api.model.data;

public record TextureData(
        String id,
        String name,
        int width,
        int height,
        byte[] imageData
) {
}

