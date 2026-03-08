package com.swag.infiniteblocks.utils;

import java.awt.Color;
import java.util.List;

public final class GradientUtil {

    private GradientUtil() {}
    public static String applyGradient(String text, List<Color> colors) {
        if (text == null || text.isEmpty() || colors == null || colors.size() < 2) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int length = text.length();
        int segments = colors.size() - 1;

        for (int i = 0; i < length; i++) {
            // progress from 0.0 to 1.0 across the full string
            float progress = (length == 1) ? 0f : (float) i / (length - 1);

            // determine which color segment we're inside
            int segmentIndex = Math.min((int) (progress * segments), segments - 1);
            float segmentStart = (float) segmentIndex / segments;
            float segmentEnd = (float) (segmentIndex + 1) / segments;
            float segmentProgress = (progress - segmentStart) / (segmentEnd - segmentStart);

            Color start = colors.get(segmentIndex);
            Color end = colors.get(segmentIndex + 1);

            int r = Math.round(start.getRed() + (end.getRed() - start.getRed()) * segmentProgress);
            int g = Math.round(start.getGreen() + (end.getGreen() - start.getGreen()) * segmentProgress);
            int b = Math.round(start.getBlue() + (end.getBlue() - start.getBlue()) * segmentProgress);

            String hex = String.format("&#%02x%02x%02x", r, g, b);
            result.append(hex).append(text.charAt(i));
        }

        return result.toString();
    }
}