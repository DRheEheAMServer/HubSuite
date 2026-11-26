package cn.dreamgary.hubsuite.trinket;

/**
 * Curios 风格饰品槽位类型（5 格基础槽位，后续生存科技树也可复用）。
 */
public enum TrinketSlotType {
    AMULET(0, Category.AMULET, "护符"),
    RING_1(1, Category.RING, "戒指 I"),
    RING_2(2, Category.RING, "戒指 II"),
    CLOAK(3, Category.CLOAK, "披风"),
    WRIST(4, Category.WRIST, "护腕");

    public enum Category {
        AMULET("护符"),
        RING("戒指"),
        CLOAK("披风"),
        WRIST("护腕");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public static final int SLOT_COUNT = values().length;

    private final int index;
    private final Category category;
    private final String label;

    TrinketSlotType(int index, Category category, String label) {
        this.index = index;
        this.category = category;
        this.label = label;
    }

    public int index() {
        return index;
    }

    public Category category() {
        return category;
    }

    public String label() {
        return label;
    }

    public boolean acceptsCategory(Category cat) {
        return this.category == cat;
    }

    public static TrinketSlotType byIndex(int idx) {
        for (TrinketSlotType t : values()) {
            if (t.index == idx) {
                return t;
            }
        }
        return null;
    }
}
