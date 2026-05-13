import java.util.*;

public class RoundResult {
    private final boolean showdown;
    private final int totalPot;
    private final List<WinnerInfo> winners = new ArrayList<>();

    public RoundResult(boolean showdown, int totalPot) {
        this.showdown = showdown;
        this.totalPot = totalPot;
    }

    public boolean isShowdown() {
        return showdown;
    }

    public int getTotalPot() {
        return totalPot;
    }

    public List<WinnerInfo> getWinners() {
        return Collections.unmodifiableList(winners);
    }

    public void addPayout(Player player, HandResult handResult, int payout) {
        String handName = handResult == null ? "Uncontested" : handResult.getHandName();

        for (WinnerInfo winner : winners) {
            if (winner.getPlayerName().equals(player.getName())
                    && winner.getHandName().equals(handName)) {
                winner.addPayout(payout);
                return;
            }
        }

        winners.add(new WinnerInfo(player.getName(), handName, payout));
    }

    public String getWinnerSummary() {
        List<String> parts = new ArrayList<>();
        for (WinnerInfo winner : winners) {
            parts.add(winner.toString());
        }
        return String.join(", ", parts);
    }
}
