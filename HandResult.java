import java.util.*;

public class HandResult {
    private final int handRank;
    private final List<Integer> tieBreakers;
    private final String handName;
    private final List<Integer> score;

    public HandResult(int handRank, List<Integer> tieBreakers, String handName) {
        this.handRank = handRank;
        this.tieBreakers = new ArrayList<>(tieBreakers);
        this.handName = handName;

        this.score = new ArrayList<>();
        this.score.add(handRank);
        this.score.addAll(tieBreakers);
    }

    public int getHandRank() {
        return handRank;
    }

    public List<Integer> getTieBreakers() {
        return Collections.unmodifiableList(tieBreakers);
    }

    public String getHandName() {
        return handName;
    }

    public List<Integer> getScore() {
        return Collections.unmodifiableList(score);
    }

    @Override
    public String toString() {
        return handName + " " + tieBreakers;
    }
}
