package moscow.mytheria.systems.modules.modules.other;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure text / enchant-level parsing helpers extracted from AutoEnchanter.
 *
 * All methods are static and side-effect free (string in, value out), so they
 * are trivially testable and shared by the item-classification logic.
 *
 * Refactor slice 1 of N — see the refactor plan.
 */
public final class AutoEnchanterText {

    public static final Pattern ENCHANT_LEVEL_PATTERN = Pattern.compile(
        "(?i)(\\b\\d+\\b|\\bI{1,3}\\b|\\bl{1,3}\\b|\\bIV\\b|\\bV\\b|\\bVI\\b|\\bVII\\b|\\bVIII\\b|\\bIX\\b|\\bX\\b|[\\u2160-\\u2169\\u2170-\\u2179])"
    );

    private AutoEnchanterText() {}

    /** Strip Minecraft §-formatting codes. */
    public static String stripFormatting(String s) {
        return s == null ? "" : s.replaceAll("(?i)\\u00A7[0-9A-FK-OR]", "");
    }

    /** Lowercase and keep only letters (drops digits, spaces, punctuation). */
    public static String normalizeLettersOnly(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]+", "");
    }

    /** True if {@code haystack} (already letters-only) contains any of the needles. */
    public static boolean containsAnyNeedle(String haystack, String[] needles) {
        for (String needle : needles) {
            String n = normalizeLettersOnly(needle);
            if (!n.isBlank() && haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    /** Parse an enchant level (digit or Roman numeral) from a lore line. */
    public static int parseEnchantLevel(String line) {
        Matcher m = ENCHANT_LEVEL_PATTERN.matcher(stripFormatting(line));
        if (!m.find()) {
            return 0;
        }
        String token = m.group(1).trim();
        return token.chars().allMatch(Character::isDigit) ? tryParseInt(token) : romanToInt(token);
    }

    /** Level of an enchant on this line if it matches one of the needles, else 0. */
    public static int extractLevelForNeedles(String line, String[] needles) {
        String norm = normalizeLettersOnly(stripFormatting(line));
        return !containsAnyNeedle(norm, needles) ? 0 : parseEnchantLevel(line);
    }

    public static int tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int romanToInt(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        String r = s.toUpperCase(Locale.ROOT)
            .replace('Ⅰ', 'I').replace('Ⅱ', 'I').replace('Ⅲ', 'I')
            .replace('Ⅳ', 'I').replace('Ⅴ', 'V').replace('Ⅵ', 'V')
            .replace('Ⅶ', 'V').replace('Ⅷ', 'V').replace('Ⅸ', 'I')
            .replace('Ⅹ', 'X')
            .replace('ⅰ', 'I').replace('ⅱ', 'I').replace('ⅲ', 'I')
            .replace('ⅳ', 'I').replace('ⅴ', 'V').replace('ⅵ', 'V')
            .replace('ⅶ', 'V').replace('ⅷ', 'V').replace('ⅸ', 'I')
            .replace('ⅹ', 'X')
            .replace("LL", "II")
            .replace('L', 'I');
        int total = 0;
        int prev = 0;
        for (int i = r.length() - 1; i >= 0; i--) {
            char c = r.charAt(i);
            int val = c == 'I' ? 1 : (c == 'V' ? 5 : (c == 'X' ? 10 : 0));
            if (val < prev) {
                total -= val;
            } else {
                total += val;
                prev = val;
            }
        }
        return total;
    }

    // ---- pure text classifiers (slice 2) ----

    public static boolean isBadEnchantText(String s) {
        if (s == null || s.isBlank()) return false;
        String n = normalizeLettersOnly(stripFormatting(s));
        return n.startsWith(normalizeLettersOnly("Нест"))
            || n.startsWith(normalizeLettersOnly("Тяж"))
            || n.contains(normalizeLettersOnly("Нестабильн"))
            || n.contains("unstable")
            || n.contains("heavy")
            || n.contains(normalizeLettersOnly("отдач"))
            || n.contains("knockback");
    }

    public static boolean isKnockbackText(String s) {
        if (s == null || s.isBlank()) return false;
        String n = normalizeLettersOnly(stripFormatting(s));
        return n.contains(normalizeLettersOnly("отдач")) || n.contains("knockback");
    }

    public static boolean lineHasEnchantLevel(String s) {
        return ENCHANT_LEVEL_PATTERN.matcher(stripFormatting(s)).find();
    }

    public static boolean isXpNeedle(String s) {
        return s.contains("опыт") || s.contains("уров") || s.contains("level") || s.contains("exp");
    }

    public static boolean isWoodNeedle(String s) {
        return s.contains("дерев") || s.contains("wood") || s.contains("log");
    }

    public static boolean isSwordAuctionNeedle(String s) {
        return s != null && !s.isBlank()
            && (s.contains(normalizeLettersOnly("незерит")) || s.contains("netherite"))
            && (s.contains(normalizeLettersOnly("меч")) || s.contains("sword"));
    }
}
