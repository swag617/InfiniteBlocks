package com.swag.infiniteblocks.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BirdflopParser {
    public static List<String> parseHexList(String birdflopRaw) {
        List<String> hexes = new ArrayList<>();
        // Pattern to find hex sequences in &x&r&r&g&g&b&b format
        Pattern pattern = Pattern.compile("(?i)(?:&|§)x(?:(?:&|§)([0-9A-Fa-f])){6}");
        Matcher matcher = pattern.matcher(birdflopRaw);

        while (matcher.find()) {
            String fullMatch = matcher.group();
            // Strip the &x and & symbols to get just the 6 hex chars
            String hex = fullMatch.replaceAll("(?i)[&§x]", "");
            if (hex.length() == 6) {
                hexes.add("#" + hex);
            }
        }
        return hexes;
    }
}