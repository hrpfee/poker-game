import java.util.*;

public class GameSnapshot {
    private final String phase;
    private final int pot;
    private final int currentBet;
    private final int minimumRaise;
    private final List<String> communityCards;
    private final List<PlayerSnapshot> players;
    private final List<String> availableActions;

    public GameSnapshot(
            String phase,
            int pot,
            int currentBet,
            int minimumRaise,
            List<String> communityCards,
            List<PlayerSnapshot> players,
            List<String> availableActions
    ) {
        this.phase = phase;
        this.pot = pot;
        this.currentBet = currentBet;
        this.minimumRaise = minimumRaise;
        this.communityCards = new ArrayList<>(communityCards);
        this.players = new ArrayList<>(players);
        this.availableActions = new ArrayList<>(availableActions);
    }

    public String getPhase() {
        return phase;
    }

    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public int getMinimumRaise() {
        return minimumRaise;
    }

    public List<String> getCommunityCards() {
        return Collections.unmodifiableList(communityCards);
    }

    public List<PlayerSnapshot> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public List<String> getAvailableActions() {
        return Collections.unmodifiableList(availableActions);
    }
}
