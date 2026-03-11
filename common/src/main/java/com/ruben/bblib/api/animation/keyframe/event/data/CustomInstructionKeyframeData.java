package com.ruben.bblib.api.animation.keyframe.event.data;

import java.util.List;

public record CustomInstructionKeyframeData(double startTime, List<String> instructions) implements KeyframeData {
}
