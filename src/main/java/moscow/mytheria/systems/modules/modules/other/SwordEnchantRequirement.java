package moscow.mytheria.systems.modules.modules.other;

/**
 * A required sword enchant: an id, the lore needles that identify it
 * (RU + EN), and the minimum level. Pure data — lifted out of AutoEnchanter
 * so the item-classification logic can live in its own class.
 */
final class SwordEnchantRequirement {
    final String id;
    final String[] needles;
    final int level;

    SwordEnchantRequirement(String id, String[] needles, int level) {
        this.id = id;
        this.needles = needles;
        this.level = level;
    }
}
