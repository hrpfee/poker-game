import java.util.*;

/**
 * テキサスホールデム用に 7 枚から最良のロール 5 枚を選び {@link HandResult} にまとめる。
 */
public class PokerHandEvaluator {

    private static final int HIGH_CARD = 0;
    private static final int ONE_PAIR = 1;
    private static final int TWO_PAIR = 2;
    private static final int THREE_KIND = 3;
    private static final int STRAIGHT = 4;
    private static final int FLUSH = 5;
    private static final int FULL_HOUSE = 6;
    private static final int FOUR_KIND = 7;
    private static final int STRAIGHT_FLUSH = 8;
    private static final int ROYAL_FLUSH = 9;

    private PokerHandEvaluator() {
    }

    public static HandResult evaluateHand(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards");
        }
        if (cards.size() == 5) {
            return evaluateFive(new ArrayList<>(cards));
        }
        HandResult best = null;
        int n = cards.size();
        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int c = b + 1; c < n - 2; c++) {
                    for (int d = c + 1; d < n - 1; d++) {
                        for (int e = d + 1; e < n; e++) {
                            List<Card> combo = Arrays.asList(
                                    cards.get(a), cards.get(b), cards.get(c),
                                    cards.get(d), cards.get(e));
                            HandResult candidate = evaluateFive(combo);
                            if (best == null || compare(candidate, best) > 0) {
                                best = candidate;
                            }
                        }
                    }
                }
            }
        }
        return Objects.requireNonNull(best);
    }

    public static int compare(HandResult a, HandResult b) {
        List<Integer> sa = a.getScore();
        List<Integer> sb = b.getScore();
        int len = Math.max(sa.size(), sb.size());
        for (int i = 0; i < len; i++) {
            int va = i < sa.size() ? sa.get(i) : 0;
            int vb = i < sb.size() ? sb.get(i) : 0;
            if (va != vb) {
                return Integer.compare(va, vb);
            }
        }
        return 0;
    }

    private static HandResult evaluateFive(List<Card> five) {
        int[] rankCount = new int[15];

        boolean flush = true;
        Card.Suit firstSuit = five.get(0).getSuitEnum();
        for (Card card : five) {
            rankCount[card.getRankEnum().getValue()]++;
            if (card.getSuitEnum() != firstSuit) {
                flush = false;
            }
        }

        int straightHigh = straightHighCard(rankCount);

        if (flush && straightHigh > 0) {
            if (isRoyal(rankCount)) {
                return new HandResult(ROYAL_FLUSH, Collections.singletonList(14), "Royal Flush");
            }
            return new HandResult(
                    STRAIGHT_FLUSH,
                    Collections.singletonList(straightHigh),
                    "Straight Flush"
            );
        }

        List<Integer> freqPattern = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCount[r] > 0) {
                freqPattern.add(rankCount[r]);
            }
        }
        freqPattern.sort(Collections.reverseOrder());

        if (freqPattern.get(0) == 4) {
            int quad = 0;
            int kicker = 0;
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 4) {
                    quad = r;
                } else if (rankCount[r] == 1) {
                    kicker = r;
                }
            }
            return new HandResult(FOUR_KIND, Arrays.asList(quad, kicker), "Four of a Kind");
        }

        if (freqPattern.get(0) == 3 && freqPattern.size() > 1 && freqPattern.get(1) == 2) {
            int trip = 0;
            int pair = 0;
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 3) {
                    trip = r;
                } else if (rankCount[r] == 2) {
                    pair = r;
                }
            }
            return new HandResult(FULL_HOUSE, Arrays.asList(trip, pair), "Full House");
        }

        if (flush) {
            List<Integer> kickers = new ArrayList<>();
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] > 0) {
                    kickers.add(r);
                }
            }
            return new HandResult(FLUSH, kickers, "Flush");
        }

        if (straightHigh > 0) {
            return new HandResult(
                    STRAIGHT,
                    Collections.singletonList(straightHigh),
                    "Straight"
            );
        }

        if (freqPattern.get(0) == 3) {
            List<Integer> tb = new ArrayList<>();
            int trip = 0;
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 3) {
                    trip = r;
                    break;
                }
            }
            tb.add(trip);
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 1) {
                    tb.add(r);
                }
            }
            tb.subList(1, tb.size()).sort(Collections.reverseOrder());
            return new HandResult(THREE_KIND, tb, "Three of a Kind");
        }

        if (freqPattern.get(0) == 2 && freqPattern.get(1) == 2) {
            List<Integer> pairs = new ArrayList<>();
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 2) {
                    pairs.add(r);
                }
            }
            pairs.sort(Collections.reverseOrder());
            int kicker = 0;
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 1) {
                    kicker = r;
                    break;
                }
            }
            return new HandResult(TWO_PAIR, Arrays.asList(pairs.get(0), pairs.get(1), kicker), "Two Pair");
        }

        if (freqPattern.get(0) == 2) {
            List<Integer> tb = new ArrayList<>();
            int pair = 0;
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 2) {
                    pair = r;
                    break;
                }
            }
            tb.add(pair);
            for (int r = 14; r >= 2; r--) {
                if (rankCount[r] == 1) {
                    tb.add(r);
                }
            }
            tb.subList(1, tb.size()).sort(Collections.reverseOrder());
            return new HandResult(ONE_PAIR, tb, "One Pair");
        }

        List<Integer> ranks = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCount[r] > 0) {
                ranks.add(r);
            }
        }
        return new HandResult(HIGH_CARD, ranks, "High Card");
    }

    /**
     * @return ストレートの最高ランク値。ホイール A-5 は 5。なしは 0。
     */
    private static int straightHighCard(int[] rankCount) {
        List<Integer> ds = new ArrayList<>();
        for (int r = 14; r >= 2; r--) {
            if (rankCount[r] > 0) {
                ds.add(r);
            }
        }
        for (int i = 0; i <= ds.size() - 5; i++) {
            int hi = ds.get(i);
            if (hi - ds.get(i + 4) == 4) {
                return hi;
            }
        }
        if (rankCount[14] > 0 && rankCount[5] > 0 && rankCount[4] > 0
                && rankCount[3] > 0 && rankCount[2] > 0) {
            return 5;
        }
        return 0;
    }

    private static boolean isRoyal(int[] rankCount) {
        return rankCount[10] > 0 && rankCount[11] > 0 && rankCount[12] > 0
                && rankCount[13] > 0 && rankCount[14] > 0;
    }
}
