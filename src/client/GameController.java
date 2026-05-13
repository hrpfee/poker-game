package poker.controller;

import javafx.application.Platform;
import poker.model.Card;
import poker.model.GameMessage;
import poker.model.GameState;
import poker.view.GameView;
import poker.websocket.PokerWebSocketClient;

import java.util.ArrayList;
import java.util.List;

/**
 * ゲームの中心的なコントローラー
 * WebSocketから受け取ったメッセージを解釈してGameViewに反映する
 * GameViewのボタン操作をWebSocketで送信する
 */
public class GameController {

    private final GameView view;
    private final GameState state;
    private PokerWebSocketClient wsClient;

    private static final String SERVER_URI = "ws://localhost:8080/poker";

    public GameController(GameView view) {
        this.view = view;
        this.state = new GameState();
        setupViewCallbacks();
    }

    // ============ 初期化 ============

    /** サーバーへ接続してゲームに参加する */
    public void connect(String playerName) {
        try {
            wsClient = new PokerWebSocketClient(SERVER_URI);
            wsClient.setOnConnected(msg -> {
                view.addLog("✅ サーバーに接続しました");
                sendJoin(playerName);
            });
            wsClient.setOnMessageReceived(this::handleMessage);
            wsClient.setOnError(err -> {
                view.addLog("❌ " + err);
            });
            wsClient.connect();
        } catch (Exception e) {
            view.addLog("❌ 接続エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** ビューのボタン操作をWebSocketに繋ぐ */
    private void setupViewCallbacks() {
        view.setOnCall(v -> sendAction(GameMessage.Type.CALL, 0));
        view.setOnFold(v -> sendAction(GameMessage.Type.FOLD, 0));
        view.setOnCheck(v -> sendAction(GameMessage.Type.CHECK, 0));
        view.setOnRaise(amount -> sendAction(GameMessage.Type.RAISE, amount));
    }

    // ============ メッセージ処理（サーバー→クライアント） ============

    private void handleMessage(GameMessage msg) {
        switch (msg.getType()) {
            case GAME_START    -> handleGameStart(msg);
            case DEAL_CARDS    -> handleDealCards(msg);
            case FLOP          -> handleCommunityCards(msg, "FLOP", 3);
            case TURN          -> handleCommunityCards(msg, "TURN", 4);
            case RIVER         -> handleCommunityCards(msg, "RIVER", 5);
            case YOUR_TURN     -> handleYourTurn(msg);
            case PLAYER_ACTION -> handlePlayerAction(msg);
            case CHIP_UPDATE   -> handleChipUpdate(msg);
            case GAME_RESULT   -> handleGameResult(msg);
            case PLAYER_JOINED -> handlePlayerJoined(msg);
            default            -> view.addLog("📨 " + msg.getMessage());
        }
    }

    /** ゲーム開始 */
    private void handleGameStart(GameMessage msg) {
        state.reset();
        view.resetRound();
        view.updateGameStatus(0, "PRE-FLOP");
        view.addLog("🎮 ゲーム開始！");
        view.addLog("席順: " + (msg.getPosition() != null ? msg.getPosition() : "-"));
        state.setMyPosition(msg.getPosition());
        view.updateMyInfo(state.getMyName(), state.getMyChips(), msg.getPosition());
    }

    /** 手札配布 */
    private void handleDealCards(GameMessage msg) {
        if (msg.getCards() == null || msg.getCards().size() < 2) return;

        List<Card> hand = new ArrayList<>();
        for (String code : msg.getCards()) {
            Card c = parseCard(code);
            if (c != null) hand.add(c);
        }
        state.getMyHand().clear();
        state.getMyHand().addAll(hand);
        view.setMyHand(hand);
        view.addLog("🃏 手札が配られました: " + msg.getCards());
    }

    /** コミュニティカード公開（フロップ/ターン/リバー） */
    private void handleCommunityCards(GameMessage msg, String phaseName, int expectedCount) {
        if (msg.getCards() == null) return;

        List<Card> community = new ArrayList<>(state.getCommunityCards());
        for (String code : msg.getCards()) {
            Card c = parseCard(code);
            if (c != null) community.add(c);
        }
        state.getCommunityCards().clear();
        state.getCommunityCards().addAll(community);
        view.setCommunityCards(community);
        view.updateGameStatus(state.getPot(), phaseName);
        view.addLog("🂠 " + phaseName + " 公開: " + msg.getCards());
    }

    /** 自分のターン通知 */
    private void handleYourTurn(GameMessage msg) {
        state.setMyTurn(true);
        state.setCurrentBet(msg.getCurrentBet());
        view.setActionsEnabled(true);
        view.addLog("👉 あなたのターン（現在のベット: " + msg.getCurrentBet() + "）");
        if (msg.getPot() > 0) {
            state.setPot(msg.getPot());
            view.updateGameStatus(msg.getPot(), state.getPhase().name());
        }
    }

    /** 他プレイヤーのアクション通知 */
    private void handlePlayerAction(GameMessage msg) {
        String actionText = switch (msg.getType()) {
            default -> msg.getMessage() != null ? msg.getMessage() : "action";
        };
        view.addLog("👤 " + msg.getPlayerName() + ": " + actionText);

        // ポット更新
        if (msg.getPot() > 0) {
            state.setPot(msg.getPot());
            view.updateGameStatus(msg.getPot(), state.getPhase().name());
        }

        // フォールド処理（相手の表示更新）
        if (msg.getMessage() != null && msg.getMessage().contains("FOLD")) {
            updateOpponentDisplay(msg.getPlayerId(), "folded");
        }
    }

    /** チップ更新 */
    private void handleChipUpdate(GameMessage msg) {
        if (msg.getPlayerId().equals(state.getMyId())) {
            state.setMyChips(msg.getChips());
            view.updateMyInfo(state.getMyName(), msg.getChips(), state.getMyPosition());
        }
        // 相手のチップは opponent1/2 の順に対応して更新
        for (int i = 0; i < state.getOpponents().size(); i++) {
            GameState.PlayerInfo op = state.getOpponents().get(i);
            if (op.getId().equals(msg.getPlayerId())) {
                op.setChips(msg.getChips());
                view.updateOpponent(i, op.getName(), msg.getChips(), op.getPosition());
            }
        }
    }

    /** 勝敗結果 */
    private void handleGameResult(GameMessage msg) {
        view.setActionsEnabled(false);
        state.setMyTurn(false);
        boolean isWinner = msg.getWinnerId() != null && msg.getWinnerId().equals(state.getMyId());

        // 全員の手札をオープン
        if (msg.getAllCards() != null) {
            msg.getAllCards().forEach((playerId, cards) -> {
                if (playerId.equals(state.getMyId())) return; // 自分はすでに表示済み
                for (int i = 0; i < state.getOpponents().size(); i++) {
                    if (state.getOpponents().get(i).getId().equals(playerId) && cards.size() >= 2) {
                        Card c1 = parseCard(cards.get(0));
                        Card c2 = parseCard(cards.get(1));
                        if (c1 != null && c2 != null) {
                            view.revealOpponentCards(i, c1, c2);
                        }
                    }
                }
            });
        }

        view.showResult(isWinner, msg.getPlayerName(), msg.getWinnerHand());
        view.addLog("🏆 勝者: " + msg.getPlayerName() + " [" + msg.getWinnerHand() + "]");
    }

    /** プレイヤー入室 */
    private void handlePlayerJoined(GameMessage msg) {
        view.addLog("👤 " + msg.getPlayerName() + " が入室しました");

        // 相手プレイヤーとして登録（自分以外）
        if (!msg.getPlayerId().equals(state.getMyId())) {
            int index = state.getOpponents().size();
            if (index < 2) {
                GameState.PlayerInfo info = new GameState.PlayerInfo(
                    msg.getPlayerId(), msg.getPlayerName(), 10000, msg.getPosition()
                );
                state.getOpponents().add(info);
                view.updateOpponent(index, msg.getPlayerName(), 10000, msg.getPosition());
            }
        }
    }

    // ============ メッセージ送信（クライアント→サーバー） ============

    private void sendJoin(String playerName) {
        state.setMyName(playerName);
        GameMessage msg = new GameMessage(GameMessage.Type.JOIN);
        msg.setPlayerName(playerName);
        wsClient.sendMessage(msg);
        view.updateMyInfo(playerName, state.getMyChips(), "");
        view.addLog("🚪 ゲームに参加しました: " + playerName);
    }

    private void sendAction(GameMessage.Type type, int amount) {
        if (wsClient == null || !wsClient.isOpen()) {
            view.addLog("⚠️ サーバーに接続されていません");
            return;
        }
        GameMessage msg = new GameMessage(type);
        msg.setPlayerId(state.getMyId());
        msg.setAmount(amount);
        wsClient.sendMessage(msg);
        view.setActionsEnabled(false);
        state.setMyTurn(false);

        String actionLog = switch (type) {
            case CALL  -> "📞 CALL";
            case RAISE -> "📈 RAISE " + amount;
            case FOLD  -> "🏳️ FOLD";
            case CHECK -> "✋ CHECK";
            default    -> type.name();
        };
        view.addLog(actionLog);

        if (type == GameMessage.Type.FOLD) {
            state.setFolded(true);
        }
    }

    // ============ ユーティリティ ============

    /**
     * カードコード（例: "AH", "KS", "10D", "2C"）をCardオブジェクトに変換
     */
    private Card parseCard(String code) {
        if (code == null || code.length() < 2) return null;
        try {
            char suitChar = code.charAt(code.length() - 1);
            String rankStr = code.substring(0, code.length() - 1);

            Card.Suit suit = switch (suitChar) {
                case 'H' -> Card.Suit.HEARTS;
                case 'D' -> Card.Suit.DIAMONDS;
                case 'C' -> Card.Suit.CLUBS;
                case 'S' -> Card.Suit.SPADES;
                default  -> null;
            };

            Card.Rank rank = switch (rankStr) {
                case "A"  -> Card.Rank.ACE;
                case "K"  -> Card.Rank.KING;
                case "Q"  -> Card.Rank.QUEEN;
                case "J"  -> Card.Rank.JACK;
                case "10" -> Card.Rank.TEN;
                case "9"  -> Card.Rank.NINE;
                case "8"  -> Card.Rank.EIGHT;
                case "7"  -> Card.Rank.SEVEN;
                case "6"  -> Card.Rank.SIX;
                case "5"  -> Card.Rank.FIVE;
                case "4"  -> Card.Rank.FOUR;
                case "3"  -> Card.Rank.THREE;
                case "2"  -> Card.Rank.TWO;
                default   -> null;
            };

            if (suit == null || rank == null) return null;
            return new Card(suit, rank);
        } catch (Exception e) {
            System.err.println("カードコードの解析エラー: " + code);
            return null;
        }
    }

    private void updateOpponentDisplay(String playerId, String action) {
        for (int i = 0; i < state.getOpponents().size(); i++) {
            if (state.getOpponents().get(i).getId().equals(playerId)) {
                if ("folded".equals(action)) {
                    view.showOpponentFolded(i);
                    view.setOpponentTurn(i, false);
                }
            }
        }
    }

    public void disconnect() {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.close();
        }
    }
}
