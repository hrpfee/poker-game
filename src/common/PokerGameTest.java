import java.util.*;

public class PokerGameTest {
    public static void main(String[] args) {
        testDeckHas52UniqueCards();
        testRoyalFlushEvaluation();
        testWheelStraightEvaluation();
        testKickerBreaksPairTie();
        testTwoPairBeatsOnePair();
        testBettingStartsAfterBigBlindPreFlop();
        testBettingStartsLeftOfDealerAfterFlop();
        testMinimumRaiseIsEnforced();
        testShortAllInRaiseIsAllowed();
        testEnumActionApi();
        testGameEventsAreRecorded();
        testSidePotDistribution();
        testFoldAwardsPotWithoutShowdown();
        testSnapshotHidesOtherPlayersHands();
        testSnapshotShowsAvailableActions();
        testInvalidActionIsRejected();
        System.out.println("All tests passed.");
    }

    private static void testDeckHas52UniqueCards() {
        Deck deck = new Deck();
        Set<String> cards = new HashSet<>();

        for (int i = 0; i < 52; i++) {
            cards.add(deck.drawCard().toString());
        }

        assertEquals(52, cards.size(), "deck has 52 unique cards");
        assertThrows(
                IllegalStateException.class,
                deck::drawCard,
                "drawing from empty deck is rejected"
        );
    }

    private static void testRoyalFlushEvaluation() {
        List<Card> cards = Arrays.asList(
                new Card("Hearts", "10"),
                new Card("Hearts", "J"),
                new Card("Hearts", "Q"),
                new Card("Hearts", "K"),
                new Card("Hearts", "A"),
                new Card("Clubs", "2"),
                new Card("Spades", "3")
        );

        HandResult result = PokerHandEvaluator.evaluateHand(cards);
        assertEquals("Royal Flush", result.getHandName(), "royal flush name");
    }

    private static void testWheelStraightEvaluation() {
        List<Card> cards = Arrays.asList(
                new Card("Hearts", "A"),
                new Card("Clubs", "2"),
                new Card("Diamonds", "3"),
                new Card("Spades", "4"),
                new Card("Hearts", "5"),
                new Card("Clubs", "9"),
                new Card("Spades", "K")
        );

        HandResult result = PokerHandEvaluator.evaluateHand(cards);
        assertEquals("Straight", result.getHandName(), "A2345 is a straight");
        assertEquals(5, result.getTieBreakers().get(0), "wheel straight high card");
    }

    private static void testKickerBreaksPairTie() {
        HandResult aceKicker = PokerHandEvaluator.evaluateHand(Arrays.asList(
                new Card("Hearts", "Q"),
                new Card("Clubs", "Q"),
                new Card("Diamonds", "A"),
                new Card("Spades", "9"),
                new Card("Hearts", "7"),
                new Card("Clubs", "4"),
                new Card("Spades", "2")
        ));
        HandResult kingKicker = PokerHandEvaluator.evaluateHand(Arrays.asList(
                new Card("Diamonds", "Q"),
                new Card("Spades", "Q"),
                new Card("Clubs", "K"),
                new Card("Diamonds", "9"),
                new Card("Spades", "7"),
                new Card("Hearts", "4"),
                new Card("Diamonds", "2")
        ));

        assertTrue(
                PokerHandEvaluator.compare(aceKicker, kingKicker) > 0,
                "pair with ace kicker beats pair with king kicker"
        );
    }

    private static void testTwoPairBeatsOnePair() {
        HandResult twoPair = PokerHandEvaluator.evaluateHand(Arrays.asList(
                new Card("Hearts", "A"),
                new Card("Clubs", "A"),
                new Card("Diamonds", "K"),
                new Card("Spades", "K"),
                new Card("Hearts", "7"),
                new Card("Clubs", "4"),
                new Card("Spades", "2")
        ));
        HandResult onePair = PokerHandEvaluator.evaluateHand(Arrays.asList(
                new Card("Hearts", "Q"),
                new Card("Clubs", "Q"),
                new Card("Diamonds", "A"),
                new Card("Spades", "K"),
                new Card("Hearts", "7"),
                new Card("Clubs", "4"),
                new Card("Spades", "2")
        ));

        assertTrue(
                PokerHandEvaluator.compare(twoPair, onePair) > 0,
                "two pair beats one pair"
        );
    }

    private static void testBettingStartsAfterBigBlindPreFlop() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        Player carol = new Player("Carol");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob, carol))
        );

        game.startNewRound();
        game.preFlop();

        assertEquals(0, game.getBettingStartIndex(), "pre-flop starts after BB");
    }

    private static void testBettingStartsLeftOfDealerAfterFlop() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        Player carol = new Player("Carol");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob, carol))
        );

        game.startNewRound();
        game.preFlop();
        game.resetBetsForNewStreet();
        game.flop();

        assertEquals(1, game.getBettingStartIndex(), "post-flop starts left of dealer");
    }

    private static void testMinimumRaiseIsEnforced() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();

        assertThrows(
                IllegalArgumentException.class,
                () -> game.playerAction(alice, PlayerActionType.RAISE, 100),
                "raise below minimum is rejected"
        );
    }

    private static void testShortAllInRaiseIsAllowed() {
        Player alice = new Player("Alice", 250);
        Player bob = new Player("Bob", 1000);
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();
        game.playerAction(alice, PlayerActionType.RAISE, 50);

        assertEquals(true, alice.isAllIn(), "short all-in raise is allowed");
        assertEquals(false, game.wasLastActionReopensBetting(), "short all-in does not reopen betting");
    }

    private static void testEnumActionApi() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();
        game.playerAction(alice, PlayerActionType.CALL, 0);

        assertEquals(9800, alice.getChips(), "enum call pays the blind difference");
    }

    private static void testGameEventsAreRecorded() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();
        game.playerAction(alice, PlayerActionType.CALL, 0);

        assertTrue(game.getEvents().size() >= 4, "events are recorded");
        assertEquals(
                "PLAYER_ACTION",
                game.getEvents().get(game.getEvents().size() - 1).getType(),
                "last event is player action"
        );
    }

    private static void testSidePotDistribution() {
        Player alice = new Player("Alice", 300);
        Player bob = new Player("Bob", 1000);
        Player carol = new Player("Carol", 1000);
        List<Player> players = new ArrayList<>(Arrays.asList(alice, bob, carol));

        TexasHoldemGame game = new TexasHoldemGame(players);
        game.startNewRound();
        game.preFlop();

        alice.getHand().clear();
        bob.getHand().clear();
        carol.getHand().clear();
        game.getCommunityCards().clear();

        alice.receiveCard(new Card("Hearts", "5"));
        alice.receiveCard(new Card("Hearts", "6"));
        bob.receiveCard(new Card("Clubs", "A"));
        bob.receiveCard(new Card("Diamonds", "A"));
        carol.receiveCard(new Card("Clubs", "K"));
        carol.receiveCard(new Card("Diamonds", "Q"));

        game.getCommunityCards().add(new Card("Hearts", "2"));
        game.getCommunityCards().add(new Card("Hearts", "3"));
        game.getCommunityCards().add(new Card("Hearts", "4"));
        game.getCommunityCards().add(new Card("Clubs", "9"));
        game.getCommunityCards().add(new Card("Diamonds", "K"));

        game.playerAction(alice, "raise", 100);
        game.playerAction(bob, "raise", 400);
        game.playerAction(carol, "call", 0);

        RoundResult result = game.finishRound();

        assertEquals(900, alice.getChips(), "Alice wins the 900 main pot");
        assertEquals(1100, bob.getChips(), "Bob wins the 800 side pot");
        assertEquals(300, carol.getChips(), "Carol loses both pots");
        assertEquals(2, result.getWinners().size(), "main and side pot winners");
    }

    private static void testFoldAwardsPotWithoutShowdown() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();
        game.playerAction(alice, "fold", 0);
        RoundResult result = game.finishRound();

        assertEquals(false, result.isShowdown(), "fold win does not require showdown");
        assertEquals(1, result.getWinners().size(), "single fold winner");
        assertEquals("Bob", result.getWinners().get(0).getPlayerName(), "Bob wins");
        assertEquals(10100, bob.getChips(), "Bob receives blinds");
    }

    private static void testSnapshotHidesOtherPlayersHands() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();

        GameSnapshot snapshot = game.createSnapshotFor(alice);

        for (PlayerSnapshot player : snapshot.getPlayers()) {
            if (player.getName().equals("Alice")) {
                assertEquals(2, player.getHand().size(), "viewer sees own hand");
            } else if (player.getName().equals("Bob")) {
                assertEquals(0, player.getHand().size(), "viewer cannot see opponent hand");
            }
        }
    }

    private static void testSnapshotShowsAvailableActions() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();

        GameSnapshot snapshot = game.createSnapshotFor("Alice");

        assertEquals("Pre-Flop", snapshot.getPhase(), "snapshot phase");
        assertTrue(
                snapshot.getAvailableActions().contains("call"),
                "small blind can call pre-flop"
        );
        assertTrue(
                snapshot.getAvailableActions().contains("raise"),
                "small blind can raise pre-flop"
        );
    }

    private static void testInvalidActionIsRejected() {
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        TexasHoldemGame game = new TexasHoldemGame(
                new ArrayList<>(Arrays.asList(alice, bob))
        );

        game.startNewRound();
        game.preFlop();

        assertThrows(
                IllegalArgumentException.class,
                () -> game.playerAction("Alice", "check", 0),
                "small blind cannot check against big blind"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> game.playerAction("Nobody", "fold", 0),
                "unknown player action is rejected"
        );
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(
                    message + " expected:<" + expected + "> actual:<" + actual + ">"
            );
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expected,
            Runnable runnable,
            String message
    ) {
        try {
            runnable.run();
        } catch (Throwable actual) {
            if (expected.isInstance(actual)) {
                return;
            }
            throw new AssertionError(
                    message + " expected:<" + expected.getSimpleName()
                            + "> actual:<" + actual.getClass().getSimpleName() + ">"
            );
        }

        throw new AssertionError(
                message + " expected:<" + expected.getSimpleName() + "> but nothing was thrown"
        );
    }
}