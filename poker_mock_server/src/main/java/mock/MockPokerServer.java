package mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public class MockPokerServer extends WebSocketServer {

    private static final int PORT = 8080;
    private static final int REQUIRED_PLAYERS = 1;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<WebSocket> clients = new CopyOnWriteArrayList<>();
    private final Map<WebSocket, String> playerNames = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String[] SUITS = {"H", "D", "S", "C"};
    private static final String[] RANKS = {"2","3","4","5","6","7","8","9","10","J","Q","K","A"};

    public MockPokerServer() {
        super(new InetSocketAddress(PORT));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[Mock] クライアント接続: " + conn.getRemoteSocketAddress());
        clients.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[Mock] クライアント切断: " + conn.getRemoteSocketAddress());
        clients.remove(conn);
        playerNames.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("[Mock] 受信: " + message);
        try {
            // Map<String, Object> で受け取ることで getOrDefault の型問題を回避
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = mapper.readValue(message, Map.class);
            String type = (String) msg.get("type");

            if (type == null) return;
            switch (type) {
                case "JOIN":
                    handleJoin(conn, msg);
                    break;
                case "CALL":
                    handleAction(conn, "CALL", 0);
                    break;
                case "RAISE":
                    Object amountObj = msg.get("amount");
                    int amount = (amountObj instanceof Number) ? ((Number) amountObj).intValue() : 400;
                    handleAction(conn, "RAISE", amount);
                    break;
                case "FOLD":
                    handleAction(conn, "FOLD", 0);
                    break;
                case "CHECK":
                    handleAction(conn, "CHECK", 0);
                    break;
                default:
                    System.out.println("[Mock] 未知のメッセージタイプ: " + type);
            }
        } catch (Exception e) {
            System.err.println("[Mock] メッセージ解析エラー: " + e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[Mock] エラー: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("========================================");
        System.out.println("  Mock Poker Server 起動");
        System.out.println("  ws://localhost:" + PORT + "/poker");
        System.out.println("  接続待ち人数: " + REQUIRED_PLAYERS + " 人");
        System.out.println("========================================");
    }

    // ===== メッセージハンドラ =====

    private void handleJoin(WebSocket conn, Map<String, Object> msg) {
        // getOrDefault を String で直接使えるように Map<String,Object> に変更
        String name = (String) msg.getOrDefault("playerName", "Player");
        playerNames.put(conn, name);

        sendAll(buildMessage(map(
            "type", "PLAYER_JOINED",
            "playerId", connId(conn),
            "playerName", name,
            "position", assignPosition(clients.indexOf(conn)),
            "message", name + " が入室しました"
        )));

        System.out.println("[Mock] プレイヤー参加: " + name + "（現在 " + playerNames.size() + " 人）");

        if (playerNames.size() >= REQUIRED_PLAYERS) {
            scheduler.schedule(this::startGame, 800, TimeUnit.MILLISECONDS);
        }
    }

    private void handleAction(WebSocket conn, String action, int amount) {
        String name = playerNames.getOrDefault(conn, "Player");
        System.out.println("[Mock] アクション受信: " + name + " → " + action);

        sendAll(buildMessage(map(
            "type", "PLAYER_ACTION",
            "playerId", connId(conn),
            "playerName", name,
            "amount", amount,
            "pot", 800,
            "message", name + " が " + action + " しました"
        )));

        scheduler.schedule(() -> advancePhase(conn, action), 1200, TimeUnit.MILLISECONDS);
    }

    // ===== ゲーム進行 =====

    private int phase = 0;

    private void startGame() {
        phase = 0;
        System.out.println("[Mock] ゲーム開始");

        List<WebSocket> conns = new ArrayList<>(clients);
        for (int i = 0; i < conns.size(); i++) {
            WebSocket conn = conns.get(i);
            sendTo(conn, buildMessage(map(
                "type", "GAME_START",
                "playerId", connId(conn),
                "playerName", playerNames.getOrDefault(conn, "Player"),
                "position", assignPosition(i),
                "chips", 10000,
                "message", "ゲームを開始します"
            )));
        }

        scheduler.schedule(() -> {
            List<String> deck = shuffledDeck();
            int idx = 0;
            for (WebSocket conn : new ArrayList<>(clients)) {
                List<String> hand = Arrays.asList(deck.get(idx), deck.get(idx + 1));
                dealtCards.put(connId(conn), hand); // 手札を記憶
                sendTo(conn, buildMessage(map(
                    "type", "DEAL_CARDS",
                    "playerId", connId(conn),
                    "cards", hand,
                    "message", "手札を配布しました"
                )));
                idx += 2;
                if (idx + 1 >= deck.size()) idx = 0;
            }
            scheduler.schedule(this::sendYourTurn, 600, TimeUnit.MILLISECONDS);
        }, 600, TimeUnit.MILLISECONDS);
    }

    private void advancePhase(WebSocket actionConn, String action) {
        if ("FOLD".equals(action)) { sendResult(); return; }
        phase++;
        switch (phase) {
            case 1: sendFlop();   break;
            case 2: sendTurn();   break;
            case 3: sendRiver();  break;
            default: sendResult(); break;
        }
    }

    private void sendFlop() {
        List<String> deck = shuffledDeck();
        sendAll(buildMessage(map(
            "type", "FLOP",
            "cards", Arrays.asList(deck.get(0), deck.get(1), deck.get(2)),
            "pot", 600,
            "message", "フロップを公開しました"
        )));
        scheduler.schedule(this::sendYourTurn, 800, TimeUnit.MILLISECONDS);
    }

    private void sendTurn() {
        List<String> deck = shuffledDeck();
        sendAll(buildMessage(map(
            "type", "TURN",
            "cards", Arrays.asList(deck.get(5)),
            "pot", 800,
            "message", "ターンを公開しました"
        )));
        scheduler.schedule(this::sendYourTurn, 800, TimeUnit.MILLISECONDS);
    }

    private void sendRiver() {
        List<String> deck = shuffledDeck();
        sendAll(buildMessage(map(
            "type", "RIVER",
            "cards", Arrays.asList(deck.get(6)),
            "pot", 1000,
            "message", "リバーを公開しました"
        )));
        scheduler.schedule(this::sendYourTurn, 800, TimeUnit.MILLISECONDS);
    }

    private void sendYourTurn() {
        if (clients.isEmpty()) return;
        WebSocket first = clients.get(0);
        sendTo(first, buildMessage(map(
            "type", "YOUR_TURN",
            "playerId", connId(first),
            "currentBet", 200,
            "pot", 600 + phase * 200,
            "message", "あなたのターンです"
        )));
        System.out.println("[Mock] YOUR_TURN 送信 → " + playerNames.getOrDefault(first, "Player"));
    }

    // 各プレイヤーの手札を記憶する（DEAL_CARDSで保存）
    private final Map<String, List<String>> dealtCards = new java.util.concurrent.ConcurrentHashMap<>();

    private void sendResult() {
        if (clients.isEmpty()) return;
        WebSocket winner = clients.get(0);
        String winnerName = playerNames.getOrDefault(winner, "Player");

        // 全員の手札をまとめてGAME_RESULTに含める
        Map<String, Object> resultMsg = map(
            "type", "GAME_RESULT",
            "winnerId", connId(winner),
            "playerName", winnerName,
            "winnerHand", "フラッシュ",
            "pot", 1200,
            "message", winnerName + " の勝利！",
            "allCards", dealtCards
        );
        sendAll(buildMessage(resultMsg));

        scheduler.schedule(() -> {
            sendTo(winner, buildMessage(map(
                "type", "CHIP_UPDATE",
                "playerId", connId(winner),
                "chips", 11200
            )));
        }, 500, TimeUnit.MILLISECONDS);

        System.out.println("[Mock] ゲーム終了 → 勝者: " + winnerName);
        phase = 0;
    }

    // ===== ユーティリティ =====

    /** broadcast という名前が親クラスと衝突するため sendAll に変更 */
    private void sendAll(String message) {
        System.out.println("[Mock] ブロードキャスト: " + message);
        for (WebSocket conn : new ArrayList<>(clients)) {
            if (conn.isOpen()) conn.send(message);
        }
    }

    private void sendTo(WebSocket conn, String message) {
        System.out.println("[Mock] 送信 → " + playerNames.getOrDefault(conn, "?") + ": " + message);
        if (conn.isOpen()) conn.send(message);
    }

    private String buildMessage(Map<String, Object> data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Map.of の代わりに可変長引数でMapを作るヘルパー */
    private Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }

    private String connId(WebSocket conn) {
        return "player-" + Math.abs(conn.hashCode() % 1000);
    }

    private String assignPosition(int index) {
        if (index == 1) return "SB";
        if (index == 2) return "BB";
        return "none";
    }

    private List<String> shuffledDeck() {
        List<String> deck = new ArrayList<>();
        for (String suit : SUITS) {
            for (String rank : RANKS) {
                deck.add(rank + suit);
            }
        }
        Collections.shuffle(deck);
        return deck;
    }

    // ===== エントリーポイント =====

    public static void main(String[] args) throws Exception {
        MockPokerServer server = new MockPokerServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                System.out.println("[Mock] サーバーを停止しました");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }
}
