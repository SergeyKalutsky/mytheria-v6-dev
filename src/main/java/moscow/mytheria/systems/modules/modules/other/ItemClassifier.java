package moscow.mytheria.systems.modules.modules.other;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1799;
import net.minecraft.class_1887;
import net.minecraft.class_2561;
import net.minecraft.class_6880;
import net.minecraft.class_9290;
import net.minecraft.class_9334;

/**
 * Item inspection / classification for AutoEnchanter.
 *
 * Holds a reference back to the module so predicates that depend on the
 * current settings (target item type, target enchant, …) can resolve them.
 * Extracted incrementally — see the refactor plan (slice 3).
 */
final class ItemClassifier {

    private final AutoEnchanter module;

    ItemClassifier(AutoEnchanter module) {
        this.module = module;
    }

    /** All lore lines of the stack as plain strings (may be empty). */
    List<String> getLoreLines(class_1799 stack) {
        List<String> out = new ArrayList<>();
        class_9290 lore = stack.method_57824(class_9334.field_49632);
        if (lore != null) {
            for (class_2561 line : lore.comp_2400()) {
                out.add(line.getString());
            }
        }
        return out;
    }

    /** Custom display name if present, else the item's default name (formatting stripped). */
    String getItemName(class_1799 stack) {
        String custom = stack.method_7964().getString();
        return custom != null && !custom.isBlank()
            ? AutoEnchanterText.stripFormatting(custom)
            : AutoEnchanterText.stripFormatting(stack.method_7909().method_63680().getString());
    }

    /**
     * Highest level of the given enchant on the stack (0 if absent).
     *
     * Reads BOTH sources, because a server may store an enchant either way:
     *   1) custom lore text (e.g. a line "Острота 7" written into minecraft:lore)
     *   2) the real vanilla enchantment component (minecraft:enchantments) — these
     *      render as a tooltip line ("Sharpness VII") but are NOT in the lore
     *      component, so they must be read from the component directly.
     *
     * The old version only read (1), which is why Sharpness on vanilla-enchanted
     * auction swords always came back as 0.
     */
    int getSwordEnchantLevel(class_1799 stack, SwordEnchantRequirement req) {
        int best = 0;

        // 1) custom lore text
        for (String line : getLoreLines(stack)) {
            int lvl = AutoEnchanterText.extractLevelForNeedles(line, req.needles);
            if (lvl > best) {
                best = lvl;
            }
        }

        // 2) vanilla enchantment component (same access path hasBadEnchant uses)
        for (Entry entry : stack.method_58657().method_57539()) {
            if (entry.getKey() == null || entry.getValue() <= 0) {
                continue;
            }
            String name = ((class_1887) ((class_6880) entry.getKey()).comp_349()).comp_2686().getString();
            String norm = AutoEnchanterText.normalizeLettersOnly(AutoEnchanterText.stripFormatting(name));
            if (AutoEnchanterText.containsAnyNeedle(norm, req.needles) && entry.getValue() > best) {
                best = entry.getValue();
            }
        }

        return best;
    }
}
