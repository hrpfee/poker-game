package poker.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import poker.model.GameMessage;

import java.net.URI;
import java.util.function.Consumer;

/**
 * サーバーとのWebSocket通信を担うクラス
 * 受信したメッセージはコールバック（onMessageReceived）でGameControllerに渡す
 */
public class PokerWebSocketClient extends WebSocketClient {

    private final ObjectMapper mapper = new ObjectMapper();
    private Consumer<GameMessage> onMessageReceived;
    private Consumer<String> onConnected;
    private Consumer<String> onError;

    public PokerWebSocketClient(String serverUri) throws Exception {
        super(new URI(serverUri));
    }

    // ============ コールバック設定 ============

    public void setOnMessageReceived(Consumer<GameMessage> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void setOnConnected(Consumer<String> onConnected) {
        this.onConnected = onConnected;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    // ============ WebSocketClientの実装 ============

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("[WebSocket] サーバーに接続しました");
        if (onConnected != null) {
            onConnected.accept("接続しました");
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            GameMessage gameMessage = mapper.readValue(message, GameMessage.class);
            if (onMessageReceived != null) {
                onMessageReceived.accept(gameMessage);
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] メッセージの解析に失敗: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WebSocket] 接続が切断されました: " + reason);
        if (onError != null) {
            onError.accept("接続が切断されました: " + reason);
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("[WebSocket] エラー: " + ex.getMessage());
        if (onError != null) {
            onError.accept("エラー: " + ex.getMessage());
        }
    }

    // ============ メッセージ送信 ============

    /**
     * サーバーへGameMessageをJSON形式で送信する
     */
    public void sendMessage(GameMessage message) {
        try {
            String json = mapper.writeValueAsString(message);
            send(json);
            System.out.println("[WebSocket] 送信: " + json);
        } catch (Exception e) {
            System.err.println("[WebSocket] 送信エラー: " + e.getMessage());
        }
    }
}
