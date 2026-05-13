import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 3人テキサスホールデムの進行・席順・ベットターン・ブロードキャストを管理する。
 * common の {@link TexasHoldemGame} をドライブし、JSON メッセージはクライアント向けに組み立てる。
 */
public class GameManager {

    static final int REQUIRED_PLAYERS = 3;
    private static final int STARTING_CHIPS = 10_000;

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ClientHandler> sessions = new CopyOnWriteArrayList<>();

    private TexasHoldemGame game;
    /** テーブルに座っているプレイヤー（game.getPlayers() と同一順序に同期） */
    private final List<Player> seatOrder = new ArrayList<>();

    /** 0 なら次の start でブラインドを回さない（最初のハンド直前のみ）。 */
    private int handsDealtInSession = 0;

    /** 状態が「ハンド進行中」のとき、アクションを期待する席インデックス。ロビー中は -1。 */
    private int currentActorIndex = -1;

    private final Object lock = new Object();

    public void onOpen(ClientHandler session) {
        sessions.add(session);
        synchronized (lock) {
            broadcastLobbyLocked();
        }
    }

    public void onClose(ClientHandler session) {
        sessions.remove(session);
        synchronized (lock) {
            seatOrder.removeIf(p -> Objects.equals(p.getName(), session.getPlayerName()));
            if (seatOrder.size() < REQUIRED_PLAYERS) {
                game = null;
                currentActorIndex = -1;
                handsDealtInSession = 0;
            }
            broadcastLobbyLocked();
        }
    }

    public void handleMessage(ClientHandler from, String rawJson) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            String type = root.path("type").asText("").toUpperCase();
            switch (type) {
                case "JOIN" -> handleJoin(from, root);
                case "ACTION" -> handleAction(from, root);
                default -> sendError(from.getConnection(), "Unknown message type: " + type);
            }
        } catch (Exception e) {
            sendError(from.getConnection(), e.getMessage() == null ? "Invalid message" : e.getMessage());
        }
    }

    private void handleJoin(ClientHandler from, JsonNode root) {
        String name = firstNonBlank(
                root.path("name").asText(null),
                root.path("playerName").asText(null)
        );
        if (name == null || name.isBlank()) {
            sendError(from.getConnection(), "Name is required");
            return;
        }
        synchronized (lock) {
            if (from.getPlayerName() != null) {
                sendError(from.getConnection(), "Already joined");
                return;
            }
            if (nameTakenLocked(name)) {
                sendError(from.getConnection(), "Name already taken");
                return;
            }
            if (seatOrder.size() >= REQUIRED_PLAYERS) {
                sendError(from.getConnection(), "Table is full");
                return;
            }
            from.setPlayerName(name.trim());
            seatOrder.add(new Player(name.trim(), STARTING_CHIPS));
            broadcastLobbyLocked();
            if (seatOrder.size() == REQUIRED_PLAYERS) {
                startNewHandLocked();
            }
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private boolean nameTakenLocked(String name) {
        for (Player p : seatOrder) {
            if (p.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void handleAction(ClientHandler from, JsonNode root) {
        synchronized (lock) {
            if (game == null || currentActorIndex < 0) {
                sendError(from.getConnection(), "No active hand");
                return;
            }
            List<Player> players = game.getPlayers();
            Player actor = players.get(currentActorIndex);
            if (!actor.getName().equals(from.getPlayerName())) {
                sendError(from.getConnection(), "Not your turn");
                return;
            }
            String action = root.path("action").asText("").toLowerCase();
            int raiseAmount = root.path("raiseAmount").asInt(0);

            try {
                game.playerAction(actor, action, raiseAmount);
            } catch (IllegalArgumentException ex) {
                sendError(from.getConnection(), ex.getMessage());
                return;
            }

            appendEventLogLocked(actor.getName() + " " + action
                    + (("raise".equals(action)) ? " " + raiseAmount : ""));

            if (game.hasSingleActivePlayer()) {
                finishHandLocked();
                return;
            }

            if (!game.isBettingRoundComplete()) {
                int n = game.getPlayers().size();
                currentActorIndex = nextEligibleIndex((currentActorIndex + 1) % n);
                if (currentActorIndex < 0) {
                    advanceStreetAfterRoundCompleteLocked();
                } else {
                    broadcastStateLocked();
                }
                return;
            }

            advanceStreetAfterRoundCompleteLocked();
        }
    }

    /**
     * 現在フェーズのベットが揃ったあと、ボードを進めるかショーダウンする。
     */
    private void advanceStreetAfterRoundCompleteLocked() {
        if (game.hasSingleActivePlayer()) {
            finishHandLocked();
            return;
        }
        TexasHoldemGame.Phase ph = game.getPhase();
        if (ph == TexasHoldemGame.Phase.RIVER) {
            finishHandLocked();
            return;
        }

        game.resetBetsForNewStreet();
        switch (ph) {
            case PRE_FLOP -> game.flop();
            case FLOP -> game.turn();
            case TURN -> game.river();
            default -> {
            }
        }

        runOutBoardIfAllInLocked();

        if (game.getPhase() == TexasHoldemGame.Phase.RIVER && game.isBettingRoundComplete()) {
            finishHandLocked();
            return;
        }

        currentActorIndex = firstEligibleFromBettingStartLocked();
        if (currentActorIndex < 0) {
            advanceStreetAfterRoundCompleteLocked();
        } else {
            broadcastStateLocked();
        }
    }

    /**
     * 全員オールインなどでこれ以上のベットがない場合、残りのコミュニティカードを一気にめくる。
     */
    private void runOutBoardIfAllInLocked() {
        while (game.getActivePlayers().size() > 1
                && game.isBettingRoundComplete()
                && game.getPhase() != TexasHoldemGame.Phase.RIVER) {
            game.resetBetsForNewStreet();
            TexasHoldemGame.Phase p = game.getPhase();
            if (p == TexasHoldemGame.Phase.FLOP) {
                game.turn();
            } else if (p == TexasHoldemGame.Phase.TURN) {
                game.river();
            } else {
                break;
            }
        }
    }

    private int firstEligibleFromBettingStartLocked() {
        int start = game.getBettingStartIndex();
        return nextEligibleIndex(start);
    }

    private int nextEligibleIndex(int start) {
        List<Player> players = game.getPlayers();
        int n = players.size();
        for (int i = 0; i < n; i++) {
            int idx = (start + i) % n;
            Player p = players.get(idx);
            if (!p.isFolded() && !p.isAllIn()) {
                return idx;
            }
        }
        return -1;
    }

    private void startNewHandLocked() {
        if (seatOrder.size() < REQUIRED_PLAYERS) {
            return;
        }

        if (game == null) {
            game = new TexasHoldemGame(new ArrayList<>(seatOrder));
        } else if (handsDealtInSession > 0) {
            game.rotateBlinds();
        }

        game.startNewRound();
        seatOrder.clear();
        seatOrder.addAll(game.getPlayers());

        notifyBustedPlayersLocked();

        if (seatOrder.size() < REQUIRED_PLAYERS || game.getPlayers().size() < 2) {
            currentActorIndex = -1;
            game = null;
            handsDealtInSession = 0;
            broadcastLobbyLocked();
            return;
        }

        game.preFlop();
        handsDealtInSession++;

        appendEventLogLocked("--- New hand ---");
        for (GameEvent ev : game.getEvents()) {
            appendEventLogLocked(ev.toString());
        }
        currentActorIndex = firstEligibleFromBettingStartLocked();
        if (currentActorIndex < 0) {
            advanceStreetAfterRoundCompleteLocked();
        } else {
            broadcastStateLocked();
        }
    }

    /** startNewRound が破産者を除いた後、席に残れないクライアントへ通知する。 */
    private void notifyBustedPlayersLocked() {
        for (ClientHandler c : sessions) {
            String name = c.getPlayerName();
            if (name == null) {
                continue;
            }
            boolean stillSeated = seatOrder.stream().anyMatch(p -> p.getName().equals(name));
            if (!stillSeated) {
                send(c.getConnection(), gameOverPayload(
                        "BUST", "チップがなくなりました: " + name));
            }
        }
    }

    private void finishHandLocked() {
        RoundResult result = game.finishRound();
        for (GameEvent ev : game.getEvents()) {
            if (ev.getType().equals("ROUND_FINISHED") || ev.getType().equals("POT_AWARDED")) {
                appendEventLogLocked(ev.toString());
            }
        }
        broadcastRoundEndLocked(result);

        if (game.isGameOver()) {
            broadcastGameOverLocked();
            game = null;
            handsDealtInSession = 0;
            currentActorIndex = -1;
            return;
        }

        startNewHandLocked();
    }

    private ClientHandler findSessionByName(String name) {
        for (ClientHandler c : sessions) {
            if (name.equals(c.getPlayerName())) {
                return c;
            }
        }
        return null;
    }

    private void broadcastLobbyLocked() {
        List<String> names = new ArrayList<>();
        for (Player p : seatOrder) {
            names.add(p.getName());
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "LOBBY");
        node.put("need", REQUIRED_PLAYERS);
        node.put("joined", seatOrder.size());
        node.set("players", mapper.valueToTree(names));
        String json = node.toString();
        for (ClientHandler c : sessions) {
            if (c.isOpen()) {
                c.send(json);
            }
        }
    }

    private void broadcastStateLocked() {
        if (game == null) {
            broadcastLobbyLocked();
            return;
        }
        String actingName = currentActorIndex >= 0
                ? game.getPlayers().get(currentActorIndex).getName()
                : null;
        for (ClientHandler c : sessions) {
            if (c.getPlayerName() == null || !c.isOpen()) {
                continue;
            }
            Player viewer = game.findPlayerByName(c.getPlayerName());
            if (viewer == null) {
                continue;
            }
            GameSnapshot snap = game.createSnapshotFor(viewer);
            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("type", "STATE");
            envelope.set("snapshot", mapper.valueToTree(snap));
            envelope.put("actingPlayer", actingName);
            envelope.put("yourTurn", actingName != null && actingName.equals(c.getPlayerName()));
            envelope.put("currentActorIndex", currentActorIndex);
            envelope.set("seatOrder", mapper.valueToTree(playerNames()));
            String json = envelope.toString();
            c.send(json);
        }
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (Player p : seatOrder) {
            names.add(p.getName());
        }
        return names;
    }

    private void broadcastRoundEndLocked(RoundResult result) {
        List<Map<String, Object>> winners = new ArrayList<>();
        for (WinnerInfo w : result.getWinners()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("playerName", w.getPlayerName());
            row.put("handName", w.getHandName());
            row.put("payout", w.getPayout());
            winners.add(row);
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "ROUND_END");
        node.put("totalPot", result.getTotalPot());
        node.put("showdown", result.isShowdown());
        node.set("winners", mapper.valueToTree(winners));
        String json = node.toString();
        for (ClientHandler c : sessions) {
            if (c.getPlayerName() != null && c.isOpen()) {
                c.send(json);
            }
        }
    }

    private void broadcastGameOverLocked() {
        String champ = seatOrder.isEmpty() ? "?" : seatOrder.get(0).getName();
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "GAME_OVER");
        node.put("message", "優勝者: " + champ);
        node.put("champion", champ);
        String json = node.toString();
        for (ClientHandler c : sessions) {
            if (c.isOpen()) {
                c.send(json);
            }
        }
    }

    private ObjectNode gameOverPayload(String code, String message) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "ERROR");
        n.put("code", code);
        n.put("message", message);
        return n;
    }

    private void sendError(WebSocket to, String message) {
        ObjectNode n = mapper.createObjectNode();
        n.put("type", "ERROR");
        n.put("message", message);
        if (to != null && to.isOpen()) {
            to.send(n.toString());
        }
    }

    private void send(WebSocket to, ObjectNode payload) {
        if (to != null && to.isOpen()) {
            to.send(payload.toString());
        }
    }

    private void appendEventLogLocked(String line) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "LOG");
        node.put("message", line);
        String json = node.toString();
        for (ClientHandler c : sessions) {
            if (c.isOpen()) {
                c.send(json);
            }
        }
    }
}
