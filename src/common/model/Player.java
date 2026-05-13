import java.util.*;

public class Player {
    private String name;
    private List<Card> hand = new ArrayList<>();

    private int chips = 10000;
    private int currentBet = 0;
    private int totalContribution = 0;

    private boolean folded = false;

    public Player(String name) {
        this.name = name;
    }

    public Player(String name, int chips) {
        if (chips < 0) {
            throw new IllegalArgumentException("Chips cannot be negative");
        }
        this.name = name;
        this.chips = chips;
    }

    public void receiveCard(Card card) {
        hand.add(card);
    }

    public void clearHand() {
        hand.clear();
        folded = false;
        currentBet = 0;
        totalContribution = 0;
    }

    public void bet(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Bet amount cannot be negative");
        }

        if (amount > chips) amount = chips;

        chips -= amount;
        currentBet += amount;
        totalContribution += amount;
    }

    public void addChips(int amount) {
        chips += amount;
    }

    public void fold() {
        folded = true;
    }

    public boolean isFolded() {
        return folded;
    }

    public boolean isAllIn() {
        return chips == 0 && !folded;
    }

    public List<Card> getHand() {
        return hand;
    }

    public String getName() {
        return name;
    }

    public int getChips() {
        return chips;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getTotalContribution() {
        return totalContribution;
    }

    public void resetBet() {
        currentBet = 0;
    }

    @Override
    public String toString() {
        return name + " Chips:" + chips + " Hand:" + hand;
    }
}