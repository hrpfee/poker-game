import java.util.*;

public class TexasHoldemGame {
    private static final int SMALL_BLIND = 100;
    private static final int BIG_BLIND = 200;

    public enum Phase {
        WAITING("Waiting"),
        PRE_FLOP("Pre-Flop"),
        FLOP("Flop"),
        TURN("Turn"),
        RIVER("River"),
        SHOWDOWN("Showdown"),
        FINISHED("Finished");

        private final String label;

        Phase(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private Deck deck;
    private List<Player> players;
    private List<Card> communityCards = new ArrayList<>();
    private List<GameEvent> events = new ArrayList<>();

    private int pot = 0;
    private int currentBet = BIG_BLIND;
    private int minimumRaise = BIG_BLIND;
    private boolean lastActionReopensBetting = false;
    private Phase phase = Phase.WAITING;

    private int dealerIndex = 0;
    private int sbIndex = 1;
    private int bbIndex = 2;

    public TexasHoldemGame(List<Player> players) {
        this.players = players;

        if (players.size() == 2) {
            sbIndex = 0;
            bbIndex = 1;
        }
    }

    public void startNewRound() {
        removeBrokePlayers();

        communityCards.clear();
        deck = new Deck();
        deck.shuffle();

        pot = 0;
        currentBet = BIG_BLIND;
        minimumRaise = BIG_BLIND;
        lastActionReopensBetting = false;
        phase = Phase.WAITING;
        events.clear();

        for (Player p : players) {
            p.clearHand();
        }

        if (players.size() < 2) return;

        pot += collectBet(players.get(sbIndex), SMALL_BLIND);
        pot += collectBet(players.get(bbIndex), BIG_BLIND);
        addEvent("ROUND_STARTED", "New round started");
        addEvent("BLIND", players.get(sbIndex).getName() + " posts small blind " + SMALL_BLIND);
        addEvent("BLIND", players.get(bbIndex).getName() + " posts big blind " + BIG_BLIND);

    }

    private int collectBet(Player player, int amount) {
        int before = player.getCurrentBet();
        player.bet(amount);
        return player.getCurrentBet() - before;
    }

    private void addEvent(String type, String message) {
        events.add(new GameEvent(type, message));
    }

    public void rotateBlinds() {
        if (players.size() < 2) return;

        dealerIndex = (dealerIndex + 1) % players.size();

        if (players.size() == 2) {
            sbIndex = dealerIndex;
            bbIndex = (dealerIndex + 1) % players.size();
        } else {
            sbIndex = (dealerIndex + 1) % players.size();
            bbIndex = (dealerIndex + 2) % players.size();
        }
    }

    public void removeBrokePlayers() {
        players.removeIf(p -> p.getChips() <= 0);

        if (players.size() < 2) return;

        dealerIndex %= players.size();

        if (players.size() == 2) {
            sbIndex = dealerIndex;
            bbIndex = (dealerIndex + 1) % players.size();
        } else {
            sbIndex = (dealerIndex + 1) % players.size();
            bbIndex = (dealerIndex + 2) % players.size();
        }
    }

    public boolean isGameOver() {
        return players.size() <= 1;
    }

    public void announceChampion() {
        if (players.isEmpty()) {
            System.out.println("No champion");
            return;
        }

        System.out.println("Champion: " + players.get(0).getName());
    }

    public void preFlop() {
        phase = Phase.PRE_FLOP;
        addEvent("PHASE_CHANGED", phase.getLabel());

        for (int i = 0; i < 2; i++) {
            for (Player p : players) {
                p.receiveCard(deck.drawCard());
            }
        }
    }

    public void flop() {
        phase = Phase.FLOP;
        addEvent("PHASE_CHANGED", phase.getLabel());
        deck.drawCard();
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.drawCard());
        }
    }

    public void turn() {
        phase = Phase.TURN;
        addEvent("PHASE_CHANGED", phase.getLabel());
        deck.drawCard();
        communityCards.add(deck.drawCard());
    }

    public void river() {
        phase = Phase.RIVER;
        addEvent("PHASE_CHANGED", phase.getLabel());
        deck.drawCard();
        communityCards.add(deck.drawCard());
    }

    public void playerAction(Player player, String action, int raiseAmount) {
        playerAction(player, PlayerActionType.fromCommand(action), raiseAmount);
    }

    public void playerAction(Player player, PlayerActionType action, int raiseAmount) {
        if (player.isFolded() || player.isAllIn()) return;

        int diff = currentBet - player.getCurrentBet();
        lastActionReopensBetting = false;

        switch (action) {
            case CHECK:
                if (diff != 0) {
                    throw new IllegalArgumentException("Cannot check");
                }
                addEvent("PLAYER_ACTION", player.getName() + " checks");
                break;

            case CALL:
                int paid = collectBet(player, diff);
                pot += paid;
                addEvent(
                        "PLAYER_ACTION",
                        player.getName() + (player.isAllIn()
                                ? " calls all-in " + paid
                                : " calls " + paid)
                );
                break;

            case RAISE:
                int total = diff + raiseAmount;

                if (raiseAmount <= 0 || total > player.getChips()) {
                    throw new IllegalArgumentException("Invalid raise");
                }

                boolean allInRaise = total == player.getChips();
                boolean fullRaise = raiseAmount >= minimumRaise;
                if (!fullRaise && !allInRaise) {
                    throw new IllegalArgumentException(
                            "Minimum raise is " + minimumRaise
                    );
                }

                pot += collectBet(player, total);
                currentBet = player.getCurrentBet();
                if (fullRaise) {
                    minimumRaise = raiseAmount;
                    lastActionReopensBetting = true;
                }
                addEvent(
                        "PLAYER_ACTION",
                        player.getName() + (player.isAllIn()
                                ? " raises all-in " + raiseAmount
                                : " raises " + raiseAmount)
                );
                break;

            case FOLD:
                player.fold();
                addEvent("PLAYER_ACTION", player.getName() + " folds");
                break;
        }
    }

    public List<String> getAvailableActions(Player player) {
        List<String> actions = new ArrayList<>();

        if (player.isFolded() || player.isAllIn()) return actions;

        if (player.getCurrentBet() == currentBet) {
            actions.add("check");
        } else {
            actions.add("call");
        }

        if (player.getChips() > currentBet - player.getCurrentBet()) {
            actions.add("raise");
        }
        actions.add("fold");

        return actions;
    }

    public List<PlayerActionType> getAvailableActionTypes(Player player) {
        List<PlayerActionType> actions = new ArrayList<>();

        if (player.isFolded() || player.isAllIn()) return actions;

        if (player.getCurrentBet() == currentBet) {
            actions.add(PlayerActionType.CHECK);
        } else {
            actions.add(PlayerActionType.CALL);
        }

        if (player.getChips() > currentBet - player.getCurrentBet()) {
            actions.add(PlayerActionType.RAISE);
        }
        actions.add(PlayerActionType.FOLD);

        return actions;
    }

    public void resetBetsForNewStreet() {
        currentBet = 0;
        minimumRaise = BIG_BLIND;
        lastActionReopensBetting = false;
        for (Player p : players) {
            p.resetBet();
        }
    }

    public void playerAction(String playerName, String action, int raiseAmount) {
        playerAction(playerName, PlayerActionType.fromCommand(action), raiseAmount);
    }

    public void playerAction(String playerName, PlayerActionType action, int raiseAmount) {
        Player player = findPlayerByName(playerName);
        if (player == null) {
            throw new IllegalArgumentException("Unknown player: " + playerName);
        }
        playerAction(player, action, raiseAmount);
    }

    public Player findPlayerByName(String playerName) {
        for (Player p : players) {
            if (p.getName().equals(playerName)) {
                return p;
            }
        }
        return null;
    }

    public int getActivePlayerCount() {
        int count = 0;
        for (Player p : players) {
            if (!p.isFolded()) {
                count++;
            }
        }
        return count;
    }

    public boolean hasSingleActivePlayer() {
        return getActivePlayerCount() == 1;
    }

    public List<Player> getActivePlayers() {
        List<Player> activePlayers = new ArrayList<>();
        for (Player p : players) {
            if (!p.isFolded()) {
                activePlayers.add(p);
            }
        }
        return activePlayers;
    }

    public boolean isBettingRoundComplete() {
        for (Player p : players) {
            if (!p.isFolded() && !p.isAllIn() && p.getCurrentBet() != currentBet) {
                return false;
            }
        }
        return true;
    }

    public HandResult evaluatePlayerHand(Player player) {
        List<Card> cards = new ArrayList<>();
        cards.addAll(player.getHand());
        cards.addAll(communityCards);
        return PokerHandEvaluator.evaluateHand(cards);
    }

    public List<Player> showdownWinners() {
        List<Player> winners = new ArrayList<>();
        HandResult best = null;

        for (Player p : getActivePlayers()) {
            HandResult result = evaluatePlayerHand(p);
            if (best == null || PokerHandEvaluator.compare(result, best) > 0) {
                best = result;
                winners.clear();
                winners.add(p);
            } else if (PokerHandEvaluator.compare(result, best) == 0) {
                winners.add(p);
            }
        }

        return winners;
    }

    public RoundResult finishRound() {
        RoundResult result = hasSingleActivePlayer()
                ? awardUncontestedPot()
                : awardSidePots();
        phase = Phase.FINISHED;
        addEvent("ROUND_FINISHED", result.getWinnerSummary());
        return result;
    }

    private RoundResult awardUncontestedPot() {
        RoundResult result = new RoundResult(false, pot);
        List<Player> activePlayers = getActivePlayers();

        if (!activePlayers.isEmpty() && pot > 0) {
            Player winner = activePlayers.get(0);
            winner.addChips(pot);
            result.addPayout(winner, null, pot);
            addEvent("POT_AWARDED", winner.getName() + " wins " + pot);
        }

        pot = 0;
        return result;
    }

    private RoundResult awardSidePots() {
        phase = Phase.SHOWDOWN;

        int totalPot = pot;
        RoundResult result = new RoundResult(true, totalPot);
        List<Integer> levels = getContributionLevels();
        int previousLevel = 0;

        for (int level : levels) {
            int sidePot = 0;
            List<Player> eligible = new ArrayList<>();

            for (Player p : players) {
                if (p.getTotalContribution() >= level) {
                    sidePot += level - previousLevel;
                    if (!p.isFolded()) {
                        eligible.add(p);
                    }
                }
            }

            if (!eligible.isEmpty() && sidePot > 0) {
                List<Player> winners = bestPlayers(eligible);
                paySplitPot(result, winners, sidePot);
            }

            previousLevel = level;
        }

        pot = 0;
        return result;
    }

    private List<Integer> getContributionLevels() {
        Set<Integer> levels = new TreeSet<>();
        for (Player p : players) {
            if (p.getTotalContribution() > 0) {
                levels.add(p.getTotalContribution());
            }
        }
        return new ArrayList<>(levels);
    }

    private List<Player> bestPlayers(List<Player> candidates) {
        List<Player> winners = new ArrayList<>();
        HandResult best = null;

        for (Player p : candidates) {
            HandResult hand = evaluatePlayerHand(p);
            if (best == null || PokerHandEvaluator.compare(hand, best) > 0) {
                best = hand;
                winners.clear();
                winners.add(p);
            } else if (PokerHandEvaluator.compare(hand, best) == 0) {
                winners.add(p);
            }
        }

        return winners;
    }

    private void paySplitPot(
            RoundResult result,
            List<Player> winners,
            int amount
    ) {
        int share = amount / winners.size();
        int remainder = amount % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            int payout = share + (i < remainder ? 1 : 0);
            Player winner = winners.get(i);
            winner.addChips(payout);
            result.addPayout(winner, evaluatePlayerHand(winner), payout);
            addEvent("POT_AWARDED", winner.getName() + " wins " + payout);
        }
    }

    public String getShowdownString() {
        phase = Phase.SHOWDOWN;

        StringBuilder sb = new StringBuilder();
        sb.append("\n---- Showdown ----\n");
        sb.append("Community: ").append(communityCards).append("\n");

        for (Player p : getActivePlayers()) {
            HandResult result = evaluatePlayerHand(p);
            sb.append(p.getName())
                    .append(" Hand:").append(p.getHand())
                    .append(" -> ").append(result.getHandName())
                    .append(" ").append(result.getTieBreakers())
                    .append("\n");
        }

        return sb.toString();
    }

    public String getPlayerView(Player player) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n========================\n");
        sb.append("PLAYER VIEW : ").append(player.getName()).append("\n");
        sb.append("PHASE       : ").append(phase.getLabel()).append("\n");
        sb.append("========================\n");

        sb.append("Your Hand: ").append(player.getHand()).append("\n");
        sb.append("Community: ").append(communityCards).append("\n");
        sb.append("Pot: ").append(pot).append("\n");
        sb.append("Current Bet: ").append(currentBet).append("\n\n");

        sb.append("Players:\n");
        for (Player p : players) {
            sb.append(p.getName())
                    .append(" ").append(getPlayerRole(p))
                    .append(" Chips:").append(p.getChips())
                    .append(" Bet:").append(p.getCurrentBet());

            if (p.isFolded()) sb.append(" FOLD");
            if (p.isAllIn()) sb.append(" ALL-IN");
            if (p == player) sb.append(" YOU");

            sb.append("\n");
        }

        sb.append("\nAvailable Actions: ");
        sb.append(getAvailableActions(player));

        return sb.toString();
    }

    public String getStatusString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n---- Status ----\n");
        sb.append("Phase: ").append(phase.getLabel()).append("\n");
        sb.append("Pot: ").append(pot).append("\n");

        for (Player p : players) {
            sb.append(p.getName())
                    .append(" ").append(getPlayerRole(p))
                    .append(" Chips:").append(p.getChips())
                    .append(" Bet:").append(p.getCurrentBet())
                    .append(" Fold:").append(p.isFolded())
                    .append(" AllIn:").append(p.isAllIn())
                    .append("\n");
        }

        return sb.toString();
    }

    public String getPlayerRole(Player player) {
        int index = players.indexOf(player);
        if (index < 0) return "";

        List<String> roles = new ArrayList<>();
        if (index == dealerIndex) roles.add("DEALER");
        if (index == sbIndex) roles.add("SB");
        if (index == bbIndex) roles.add("BB");

        if (roles.isEmpty()) return "";
        return "[" + String.join("/", roles) + "]";
    }

    public GameSnapshot createSnapshotFor(Player viewingPlayer) {
        List<PlayerSnapshot> playerSnapshots = new ArrayList<>();

        for (Player p : players) {
            List<String> visibleHand = p == viewingPlayer
                    ? cardStrings(p.getHand())
                    : new ArrayList<>();

            playerSnapshots.add(new PlayerSnapshot(
                    p.getName(),
                    p.getChips(),
                    p.getCurrentBet(),
                    p.getTotalContribution(),
                    p.isFolded(),
                    p.isAllIn(),
                    getPlayerRole(p),
                    visibleHand
            ));
        }

        List<String> actions = viewingPlayer == null
                ? new ArrayList<>()
                : getAvailableActions(viewingPlayer);

        return new GameSnapshot(
                phase.getLabel(),
                pot,
                currentBet,
                minimumRaise,
                cardStrings(communityCards),
                playerSnapshots,
                actions
        );
    }

    public GameSnapshot createSnapshotFor(String playerName) {
        return createSnapshotFor(findPlayerByName(playerName));
    }

    private List<String> cardStrings(List<Card> cards) {
        List<String> values = new ArrayList<>();
        for (Card card : cards) {
            values.add(card.toString());
        }
        return values;
    }

    public int getBettingStartIndex() {
        if (players.isEmpty()) return 0;

        if (phase == Phase.PRE_FLOP) {
            return (bbIndex + 1) % players.size();
        }

        return (dealerIndex + 1) % players.size();
    }

    public int getMinimumRaise() {
        return minimumRaise;
    }

    public int getCallAmount(Player player) {
        return Math.max(0, currentBet - player.getCurrentBet());
    }

    public int getMaxRaiseAmount(Player player) {
        return Math.max(0, player.getChips() - getCallAmount(player));
    }

    public boolean wasLastActionReopensBetting() {
        return lastActionReopensBetting;
    }

    public List<GameEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public String getRoundHeaderString() {
        return """
                ========================
                === NEW ROUND START ===
                ========================
                """;
    }

    public String getPhaseHeaderString() {
        return "\n========================\n"
                + "=== " + phase.getLabel().toUpperCase() + " ===\n"
                + "========================";
    }

    public List<Player> getPlayers() {
        return players;
    }

    public int getPot() {
        return pot;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public Phase getPhase() {
        return phase;
    }

    public List<Card> getCommunityCards() {
        return communityCards;
    }
}
