import java.util.*;

public class PlayerSnapshot {
    private final String name;
    private final int chips;
    private final int currentBet;
    private final int totalContribution;
    private final boolean folded;
    private final boolean allIn;
    private final String role;
    private final List<String> hand;

    public PlayerSnapshot(
            String name,
            int chips,
            int currentBet,
            int totalContribution,
            boolean folded,
            boolean allIn,
            String role,
            List<String> hand
    ) {
        this.name = name;
        this.chips = chips;
        this.currentBet = currentBet;
        this.totalContribution = totalContribution;
        this.folded = folded;
        this.allIn = allIn;
        this.role = role;
        this.hand = new ArrayList<>(hand);
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

    public boolean isFolded() {
        return folded;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public String getRole() {
        return role;
    }

    public List<String> getHand() {
        return Collections.unmodifiableList(hand);
    }
}
