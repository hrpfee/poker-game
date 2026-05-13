package poker.model;

/**
 * トランプ1枚を表すモデルクラス
 */
public class Card {

    public enum Suit {
        SPADES("♠"), HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣");

        private final String symbol;
        Suit(String symbol) { this.symbol = symbol; }
        public String getSymbol() { return symbol; }
    }

    public enum Rank {
        TWO(2,"2"), THREE(3,"3"), FOUR(4,"4"), FIVE(5,"5"),
        SIX(6,"6"), SEVEN(7,"7"), EIGHT(8,"8"), NINE(9,"9"),
        TEN(10,"10"), JACK(11,"J"), QUEEN(12,"Q"), KING(13,"K"), ACE(14,"A");

        private final int value;
        private final String display;
        Rank(int value, String display) { this.value = value; this.display = display; }
        public int getValue() { return value; }
        public String getDisplay() { return display; }
    }

    private final Suit suit;
    private final Rank rank;
    private boolean faceDown;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        this.faceDown = false;
    }

    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }
    public boolean isFaceDown() { return faceDown; }
    public void setFaceDown(boolean faceDown) { this.faceDown = faceDown; }

    /** 赤スートかどうか（ハート・ダイヤ） */
    public boolean isRed() {
        return suit == Suit.HEARTS || suit == Suit.DIAMONDS;
    }

    @Override
    public String toString() {
        return rank.getDisplay() + suit.getSymbol();
    }
}
