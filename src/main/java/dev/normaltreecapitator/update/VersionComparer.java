package dev.normaltreecapitator.update;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionComparer {

    private static final Pattern STRIP_SUFFIX = Pattern.compile("[-+].*$");

    private VersionComparer() {
    }

    static boolean isRemoteNewer(String remote, String local) {
        if (remote == null || remote.isBlank() || local == null || local.isBlank()) {
            return false;
        }
        List<Integer> remoteParts = parse(normalize(remote));
        List<Integer> localParts = parse(normalize(local));
        if (remoteParts.isEmpty() || localParts.isEmpty()) {
            return !normalize(remote).equalsIgnoreCase(normalize(local));
        }
        int length = Math.max(remoteParts.size(), localParts.size());
        for (int i = 0; i < length; i++) {
            int r = i < remoteParts.size() ? remoteParts.get(i) : 0;
            int l = i < localParts.size() ? localParts.get(i) : 0;
            if (r > l) {
                return true;
            }
            if (r < l) {
                return false;
            }
        }
        return false;
    }

    private static String normalize(String version) {
        String trimmed = version.trim();
        int buildIndex = trimmed.toLowerCase().indexOf(" build ");
        if (buildIndex >= 0) {
            trimmed = trimmed.substring(0, buildIndex).trim();
        }
        Matcher matcher = STRIP_SUFFIX.matcher(trimmed);
        if (matcher.find()) {
            return matcher.replaceFirst("");
        }
        return trimmed;
    }

    private static List<Integer> parse(String version) {
        List<Integer> parts = new ArrayList<>();
        for (String segment : version.split("\\.")) {
            if (segment.isEmpty()) {
                continue;
            }
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (Character.isDigit(c)) {
                    digits.append(c);
                } else {
                    break;
                }
            }
            if (!digits.isEmpty()) {
                try {
                    parts.add(Integer.parseInt(digits.toString()));
                } catch (NumberFormatException ignored) {
                    return List.of();
                }
            }
        }
        return parts;
    }
}
