public class Card {
    public enum Suit {
        SPADES("Spades"),
        HEARTS("Hearts"),
        DIAMONDS("Diamonds"),
        CLUBS("Clubs");

        private final String label;

        Suit(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static Suit fromLabel(String label) {
            for (Suit suit : values()) {
                if (suit.label.equalsIgnoreCase(label)) {
                    return suit;
                }
            }
            throw new IllegalArgumentException("Invalid suit: " + label);
        }
    }

    public enum Rank {
        TWO("2", 2),
        THREE("3", 3),
        FOUR("4", 4),
        FIVE("5", 5),
        SIX("6", 6),
        SEVEN("7", 7),
        EIGHT("8", 8),
        NINE("9", 9),
        TEN("10", 10),
        JACK("J", 11),
        QUEEN("Q", 12),
        KING("K", 13),
        ACE("A", 14);

        private final String label;
        private final int value;

        Rank(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public int getValue() {
            return value;
        }

        public static Rank fromLabel(String label) {
            for (Rank rank : values()) {
                if (rank.label.equalsIgnoreCase(label)) {
                    return rank;
                }
            }
            throw new IllegalArgumentException("Invalid rank: " + label);
        }
    }

    private final Suit suit;
    private final Rank rank;

    public Card(String suit, String rank) {
        this(Suit.fromLabel(suit), Rank.fromLabel(rank));
    }

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String getSuit() {
        return suit.getLabel();
    }

    public String getRank() {
        return rank.getLabel();
    }

    public Suit getSuitEnum() {
        return suit;
    }

    public Rank getRankEnum() {
        return rank;
    }

    public int getRankValue() {
        return rank.getValue();
    }

    @Override
    public String toString() {
        return rank.getLabel() + " of " + suit.getLabel();
    }
}