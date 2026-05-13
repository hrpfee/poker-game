import java.util.*;

public class PokerTest {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        List<Player> players = new ArrayList<>();

        System.out.print("Number of players (2 or more): ");
        int numPlayers = sc.nextInt();

        while (numPlayers < 2) {
            System.out.print("Need at least 2 players: ");
            numPlayers = sc.nextInt();
        }

        for (int i = 1; i <= numPlayers; i++) {
            System.out.print("Player " + i + " name: ");
            players.add(new Player(sc.next()));
        }

        TexasHoldemGame game = new TexasHoldemGame(players);

        while (!game.isGameOver()) {
            game.startNewRound();
            if (game.isGameOver()) break;

            System.out.println(game.getRoundHeaderString());
            System.out.println(game.getStatusString());

            game.preFlop();
            System.out.println(game.getPhaseHeaderString());
            runBettingRound(sc, game);

            if (!game.hasSingleActivePlayer()) {
                game.resetBetsForNewStreet();
                game.flop();
                System.out.println(game.getPhaseHeaderString());
                runBettingRound(sc, game);
            }

            if (!game.hasSingleActivePlayer()) {
                game.resetBetsForNewStreet();
                game.turn();
                System.out.println(game.getPhaseHeaderString());
                runBettingRound(sc, game);
            }

            if (!game.hasSingleActivePlayer()) {
                game.resetBetsForNewStreet();
                game.river();
                System.out.println(game.getPhaseHeaderString());
                runBettingRound(sc, game);
            }

            if (!game.hasSingleActivePlayer()) {
                System.out.println(game.getShowdownString());
            }

            RoundResult result = game.finishRound();
            System.out.println("Winner(s): " + result.getWinnerSummary());
            System.out.println(game.getStatusString());

            game.removeBrokePlayers();
            game.rotateBlinds();
        }

        game.announceChampion();
    }

    private static void runBettingRound(Scanner sc, TexasHoldemGame game) {
        Set<Player> acted = new HashSet<>();
        int index = game.getBettingStartIndex();

        while (!game.hasSingleActivePlayer()) {
            Player player = game.getPlayers().get(index % game.getPlayers().size());

            if (!player.isFolded() && !player.isAllIn()) {
                boolean needsAction = !acted.contains(player)
                        || player.getCurrentBet() != game.getCurrentBet();

                if (needsAction) {
                    System.out.println("\n\n\n\n\n");
                    System.out.println(game.getPlayerView(player));

                    String action = readAction(sc, game, player);
                    int raise = action.equalsIgnoreCase("raise")
                            ? readRaiseAmount(sc, game, player)
                            : 0;

                    try {
                        game.playerAction(player, action, raise);

                        if (game.wasLastActionReopensBetting()) {
                            acted.clear();
                        }
                        acted.add(player);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                        continue;
                    }
                }
            }

            if (game.isBettingRoundComplete() && allPendingPlayersActed(game, acted)) {
                break;
            }

            index++;
        }
    }

    private static String readAction(
            Scanner sc,
            TexasHoldemGame game,
            Player player
    ) {
        List<String> actions = game.getAvailableActions(player);

        while (true) {
            System.out.print("\nAction " + actions + ": ");
            String action = sc.next().toLowerCase();

            if (actions.contains(action)) {
                return action;
            }

            System.out.println("Invalid action. Choose one of " + actions);
        }
    }

    private static int readRaiseAmount(
            Scanner sc,
            TexasHoldemGame game,
            Player player
    ) {
        int minRaise = game.getMinimumRaise();
        int maxRaise = game.getMaxRaiseAmount(player);

        while (true) {
            System.out.print(
                    "Raise amount (min " + minRaise
                            + ", max " + maxRaise
                            + ", or " + maxRaise + " for all-in): "
            );

            if (!sc.hasNextInt()) {
                System.out.println("Enter a positive number.");
                sc.next();
                continue;
            }

            int amount = sc.nextInt();
            boolean allInRaise = amount == maxRaise;
            if (amount > 0 && amount <= maxRaise
                    && (amount >= minRaise || allInRaise)) {
                return amount;
            }

            System.out.println(
                    "Raise amount must be at least " + minRaise
                            + ", or exactly " + maxRaise + " to go all-in."
            );
        }
    }

    private static boolean allPendingPlayersActed(
            TexasHoldemGame game,
            Set<Player> acted
    ) {
        for (Player p : game.getPlayers()) {
            if (!p.isFolded() && !p.isAllIn() && !acted.contains(p)) {
                return false;
            }
        }
        return true;
    }
}