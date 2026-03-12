package com.ruben.bblib.api.model.data;

public record TextureData(
        String id,
        String name,
        int width,
        int height,
        int uvWidth,
        int uvHeight,
        int frameTime,
        String frameOrderType,
        String frameOrder,
        boolean frameInterpolate,
        byte[] imageData
) {
    public int uvWidthOrDefault() {
        return uvWidth > 0 ? uvWidth : width;
    }

    public int uvHeightOrDefault() {
        return uvHeight > 0 ? uvHeight : height;
    }

    public int frameTimeOrDefault() {
        return frameTime > 0 ? frameTime : 1;
    }

    public int frameCount() {
        int frameHeight = uvHeightOrDefault();
        if (frameHeight <= 0 || height <= 0) {
            return 1;
        }
        return Math.max(1, height / frameHeight);
    }

    public boolean isAnimated() {
        return frameCount() > 1;
    }

    public AnimationFrame resolveAnimationFrame(double animationTick) {
        int[] frames = resolveFrameSequence();
        if (frames.length == 0) {
            return new AnimationFrame(0, 0, 0.0f);
        }
        if (frames.length == 1) {
            return new AnimationFrame(frames[0], frames[0], 0.0f);
        }

        double frameProgress = Math.max(animationTick, 0.0d) / frameTimeOrDefault();
        int sequenceIndex = Math.floorMod((int) Math.floor(frameProgress), frames.length);
        int nextSequenceIndex = (sequenceIndex + 1) % frames.length;
        float interpolation = frameInterpolate ? (float) (frameProgress - Math.floor(frameProgress)) : 0.0f;

        return new AnimationFrame(frames[sequenceIndex], frames[nextSequenceIndex], interpolation);
    }

    private int[] resolveFrameSequence() {
        int frameCount = frameCount();
        if (frameCount <= 1) {
            return new int[]{0};
        }

        String orderType = frameOrderType != null ? frameOrderType : "loop";
        return switch (orderType) {
            case "backwards" -> buildBackwardsSequence(frameCount);
            case "back_and_forth" -> buildBackAndForthSequence(frameCount);
            case "custom" -> buildCustomSequence(frameCount);
            default -> buildLoopSequence(frameCount);
        };
    }

    private int[] buildLoopSequence(int frameCount) {
        int[] frames = new int[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = i;
        }
        return frames;
    }

    private int[] buildBackwardsSequence(int frameCount) {
        int[] frames = new int[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = frameCount - 1 - i;
        }
        return frames;
    }

    private int[] buildBackAndForthSequence(int frameCount) {
        if (frameCount == 2) {
            return new int[]{0, 1};
        }

        int[] frames = new int[frameCount * 2 - 2];
        int index = 0;
        for (int i = 0; i < frameCount; i++) {
            frames[index++] = i;
        }
        for (int i = frameCount - 2; i > 0; i--) {
            frames[index++] = i;
        }
        return frames;
    }

    private int[] buildCustomSequence(int frameCount) {
        if (frameOrder == null || frameOrder.isBlank()) {
            return buildLoopSequence(frameCount);
        }

        String[] parts = frameOrder.trim().split("[,\\s]+");
        int[] parsed = new int[parts.length];
        int count = 0;

        for (String part : parts) {
            try {
                int frame = Integer.parseInt(part);
                if (frame >= 0 && frame < frameCount) {
                    parsed[count++] = frame;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (count == 0) {
            return buildLoopSequence(frameCount);
        }

        int[] frames = new int[count];
        System.arraycopy(parsed, 0, frames, 0, count);
        return frames;
    }

    public record AnimationFrame(int frameIndex, int nextFrameIndex, float interpolation) {
    }
}

