package com.ranit.botscraft.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class to filter inappropriate content for Play Store compliance.
 */
public class SafetyHelper {

    // High-risk keywords that lead to app rejection
    private static final List<String> BANNED_KEYWORDS = Arrays.asList(
            "porn", "sex", "naked", "xxx", "nude", "pussy", "dick", "cock", "fuck", "blowjob",
            "rape", "incest", "pedophile", "drugs", "cocaine", "heroin", "meth", "suicide", "kill yourself",
            "hentai", "escort", "hookup", "onlyfans", "sugar baby", "sugar daddy"
    );

    /**
     * Checks if the text contains any banned keywords.
     * Returns true if the text is unsafe.
     */
    public static boolean isUnsafe(String text) {
        if (text == null || text.isEmpty()) return false;
        
        String lowerText = text.toLowerCase();
        for (String word : BANNED_KEYWORDS) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Optional: Replaces banned words with asterisks for softer filtering.
     */
    public static String censor(String text) {
        if (text == null || text.isEmpty()) return text;
        
        String censored = text;
        for (String word : BANNED_KEYWORDS) {
            String replacement = new String(new char[word.length()]).replace('\0', '*');
            censored = censored.replaceAll("(?i)" + word, replacement);
        }
        return censored;
    }
}
