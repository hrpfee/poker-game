public class WinnerInfo {
    private final String playerName;
    private final String handName;
    private int payout;

    public WinnerInfo(String playerName, String handName, int payout) {
        this.playerName = playerName;
        this.handName = handName;
        this.payout = payout;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getHandName() {
        return handName;
    }

    public int getPayout() {
        return payout;
    }

    public void addPayout(int amount) {
        payout += amount;
    }

    @Override
    public String toString() {
        return playerName + " (" + handName + ", +" + payout + ")";
    }
}