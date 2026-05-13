import java.util.*;

public class PokerHandEvaluator {

    private static final List<String> RANK_ORDER = Arrays.asList(
            "2", "3", "4", "5", "6", "7", "8",
            "9", "10", "J", "Q", "K", "A"
    );

    public static List<Integer> evaluate(List<Card> cards) {

        if (isRoyalFlush(cards)) return Arrays.asList(10);

        List<Integer> straightFlush = getStraightFlush(cards);
        if (straightFlush != null) return straightFlush;

        List<Integer> four = getNOfKind(cards, 4, 8);
        if (four != null) return four;

        List<Integer> fullHouse = getFullHouse(cards);
        if (fullHouse != null) return fullHouse;

        List<Integer> flush = getFlush(cards);
        if (flush != null) return flush;

        List<Integer> straight = getStraight(cards);
        if (straight != null) return straight;

        List<Integer> three = getNOfKind(cards, 3, 4);
        if (three != null) return three;

        List<Integer> twoPair = getTwoPair(cards);
        if (twoPair != null) return twoPair;

        List<Integer> pair = getNOfKind(cards, 2, 2);
        if (pair != null) return pair;

        return getHighCard(cards);
    }

    public static HandResult evaluateHand(List<Card> cards) {
        List<Integer> score = evaluate(cards);
        return new HandResult(
                score.get(0),
                score.subList(1, score.size()),
                getHandName(score)
        );
    }

    public static String getHandName(List<Integer> score) {
        switch (score.get(0)) {
            case 10:
                return "Royal Flush";
            case 9:
                return "Straight Flush";
            case 8:
                return "Four of a Kind";
            case 7:
                return "Full House";
            case 6:
                return "Flush";
            case 5:
                return "Straight";
            case 4:
                return "Three of a Kind";
            case 3:
                return "Two Pair";
            case 2:
                return "One Pair";
            default:
                return "High Card";
        }
    }

    public static int compare(List<Integer> a, List<Integer> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return a.get(i) - b.get(i);
            }
        }
        return 0;
    }

    public static int compare(HandResult a, HandResult b) {
        return compare(a.getScore(), b.getScore());
    }

    private static int rankValue(Card c) {
        return c.getRankValue();
    }

    private static List<Integer> getSortedUniqueRanks(List<Card> cards) {
        Set<Integer> set = new TreeSet<>();

        for (Card c : cards) {
            set.add(rankValue(c));
        }

        // A2345 straight対応
        if (set.contains(14)) {
            set.add(1);
        }

        return new ArrayList<>(set);
    }

    private static List<Integer> getStraight(List<Card> cards) {
        List<Integer> nums = getSortedUniqueRanks(cards);

        int consecutive = 1;
        int bestHigh = -1;

        for (int i = 1; i < nums.size(); i++) {
            if (nums.get(i) == nums.get(i - 1) + 1) {
                consecutive++;

                if (consecutive >= 5) {
                    bestHigh = nums.get(i);
                }
            } else {
                consecutive = 1;
            }
        }

        if (bestHigh != -1) {
            return Arrays.asList(5, bestHigh);
        }

        return null;
    }

    private static boolean isRoyalFlush(List<Card> cards) {
        List<Integer> sf = getStraightFlush(cards);
        return sf != null && sf.get(1) == 14;
    }

    private static List<Integer> getStraightFlush(List<Card> cards) {
        Map<String, List<Card>> suitMap = new HashMap<>();

        for (Card c : cards) {
            suitMap.putIfAbsent(c.getSuit(), new ArrayList<>());
            suitMap.get(c.getSuit()).add(c);
        }

        for (List<Card> suited : suitMap.values()) {
            if (suited.size() >= 5) {
                List<Integer> straight = getStraight(suited);

                if (straight != null) {
                    return Arrays.asList(9, straight.get(1));
                }
            }
        }

        return null;
    }

    private static List<Integer> getFlush(List<Card> cards) {
        Map<String, List<Card>> suitMap = new HashMap<>();

        for (Card c : cards) {
            suitMap.putIfAbsent(c.getSuit(), new ArrayList<>());
            suitMap.get(c.getSuit()).add(c);
        }

        for (List<Card> suited : suitMap.values()) {
            if (suited.size() >= 5) {
                suited.sort((a, b) -> rankValue(b) - rankValue(a));

                List<Integer> result = new ArrayList<>();
                result.add(6);

                for (int i = 0; i < 5; i++) {
                    result.add(rankValue(suited.get(i)));
                }

                return result;
            }
        }

        return null;
    }

    private static Map<Integer, Integer> rankCount(List<Card> cards) {
        Map<Integer, Integer> map = new HashMap<>();

        for (Card c : cards) {
            int r = rankValue(c);
            map.put(r, map.getOrDefault(r, 0) + 1);
        }

        return map;
    }

    private static List<Integer> sortedRanks(List<Card> cards) {
        List<Integer> ranks = new ArrayList<>();

        for (Card c : cards) {
            ranks.add(rankValue(c));
        }

        ranks.sort(Collections.reverseOrder());
        return ranks;
    }

    private static List<Integer> getNOfKind(
            List<Card> cards,
            int n,
            int handRank
    ) {
        Map<Integer, Integer> count = rankCount(cards);

        int found = -1;

        for (int r : count.keySet()) {
            if (count.get(r) == n && r > found) {
                found = r;
            }
        }

        if (found == -1) return null;

        List<Integer> result = new ArrayList<>();
        result.add(handRank);
        result.add(found);

        Set<Integer> used = new HashSet<>();
        used.add(found);

        for (int r : sortedRanks(cards)) {
            if (!used.contains(r)) {
                result.add(r);
                used.add(r);
            }

            if (result.size() >= 6) break;
        }

        return result;
    }

    private static List<Integer> getTwoPair(List<Card> cards) {
        Map<Integer, Integer> count = rankCount(cards);

        List<Integer> pairs = new ArrayList<>();
        List<Integer> others = new ArrayList<>();

        for (int r : count.keySet()) {
            if (count.get(r) >= 2) {
                pairs.add(r);
            } else {
                others.add(r);
            }
        }

        pairs.sort(Collections.reverseOrder());
        others.sort(Collections.reverseOrder());

        if (pairs.size() >= 2) {
            List<Integer> result = new ArrayList<>();
            result.add(3);
            result.add(pairs.get(0));
            result.add(pairs.get(1));

            if (!others.isEmpty()) {
                result.add(others.get(0));
            }

            return result;
        }

        return null;
    }

    private static List<Integer> getFullHouse(List<Card> cards) {
        Map<Integer, Integer> count = rankCount(cards);

        List<Integer> threes = new ArrayList<>();
        List<Integer> pairs = new ArrayList<>();

        for (int r : count.keySet()) {
            if (count.get(r) >= 3) {
                threes.add(r);
            } else if (count.get(r) >= 2) {
                pairs.add(r);
            }
        }

        threes.sort(Collections.reverseOrder());
        pairs.sort(Collections.reverseOrder());

        if (threes.size() >= 1 &&
                (!pairs.isEmpty() || threes.size() >= 2)) {

            int three = threes.get(0);
            int pair = threes.size() >= 2
                    ? threes.get(1)
                    : pairs.get(0);

            return Arrays.asList(7, three, pair);
        }

        return null;
    }

    private static List<Integer> getHighCard(List<Card> cards) {
        List<Integer> result = new ArrayList<>();
        result.add(1);

        List<Integer> ranks = sortedRanks(cards);

        Set<Integer> used = new HashSet<>();

        for (int r : ranks) {
            if (!used.contains(r)) {
                result.add(r);
                used.add(r);
            }

            if (result.size() >= 6) break;
        }

        return result;
    }
}
