package moscow.mytheria.systems.modules.modules.other;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
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

    /** Highest level found in lore for the given enchant requirement's needles (0 if none). */
    int getSwordEnchantLevel(class_1799 stack, SwordEnchantRequirement req) {
        int best = 0;
        for (String line : getLoreLines(stack)) {
            int lvl = AutoEnchanterText.extractLevelForNeedles(line, req.needles);
            if (lvl > best) {
                best = lvl;
            }
        }
        return best;
    }
}
