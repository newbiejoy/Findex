package com.findex.core.snippet;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnippetGenerator {
    private final int windowSize;

    public SnippetGenerator() {
        this(220);
    }

    public SnippetGenerator(int windowSize) {
        this.windowSize = windowSize;
    }

    public String generateSnippet(String text, Set<String> matchedTerms) {
        if (text == null || text.isBlank()) {
            return "No content available.";
        }

        if (matchedTerms == null || matchedTerms.isEmpty()) {
            int len = Math.min(text.length(), windowSize);
            return escapeHtml(text.substring(0, len).trim()) + (text.length() > windowSize ? "..." : "");
        }

        // Clean extra whitespaces for smooth reading
        String cleanText = text.replaceAll("\\s+", " ").trim();
        String lowerText = cleanText.toLowerCase(Locale.ROOT);

        // Find occurrences of all terms
        List<Integer> matchIndices = new ArrayList<>();
        for (String term : matchedTerms) {
            if (term.isBlank()) continue;
            int idx = lowerText.indexOf(term.toLowerCase(Locale.ROOT));
            while (idx >= 0) {
                matchIndices.add(idx);
                idx = lowerText.indexOf(term.toLowerCase(Locale.ROOT), idx + 1);
            }
        }

        if (matchIndices.isEmpty()) {
            int len = Math.min(cleanText.length(), windowSize);
            return escapeHtml(cleanText.substring(0, len)) + (cleanText.length() > windowSize ? "..." : "");
        }

        Collections.sort(matchIndices);

        // Find the window of size windowSize containing the maximum number of matches
        int bestStart = 0;
        int bestCount = 0;

        for (int i = 0; i < matchIndices.size(); i++) {
            int windowStart = matchIndices.get(i);
            int windowEnd = windowStart + windowSize;
            int count = 0;

            for (int j = i; j < matchIndices.size(); j++) {
                if (matchIndices.get(j) < windowEnd) {
                    count++;
                } else {
                    break;
                }
            }

            if (count > bestCount) {
                bestCount = count;
                bestStart = windowStart;
            }
        }

        // Adjust window start slightly backward so we don't cut words in the middle
        int start = Math.max(0, bestStart - 35);
        if (start > 0) {
            int prevSpace = cleanText.indexOf(' ', start);
            if (prevSpace != -1 && prevSpace < bestStart) {
                start = prevSpace + 1;
            }
        }

        int end = Math.min(cleanText.length(), start + windowSize);
        if (end < cleanText.length()) {
            int nextSpace = cleanText.lastIndexOf(' ', end);
            if (nextSpace > start + 50) {
                end = nextSpace;
            }
        }

        String rawSnippet = cleanText.substring(start, end).trim();
        String escapedSnippet = escapeHtml(rawSnippet);

        // Highlight matched terms with <mark> tags
        for (String term : matchedTerms) {
            if (term.isBlank()) continue;
            String regex = "(?i)\\b(" + Pattern.quote(term) + ")\\b";
            escapedSnippet = escapedSnippet.replaceAll(regex, "<mark class=\"findex-highlight\">$1</mark>");
        }

        StringBuilder sb = new StringBuilder();
        if (start > 0) sb.append("... ");
        sb.append(escapedSnippet);
        if (end < cleanText.length()) sb.append(" ...");

        return sb.toString();
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
