import org.java_websocket.WebSocket;

/**
 * 1 クライアント接続に対応するセッション（表示名と送信ヘルパー）。
 */
public class ClientHandler {

    private final WebSocket connection;
    private volatile String playerName;

    public ClientHandler(WebSocket connection) {
        this.connection = connection;
    }

    public WebSocket getConnection() {
        return connection;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public boolean isOpen() {
        return connection != null && connection.isOpen();
    }

    public void send(String jsonText) {
        if (isOpen()) {
            connection.send(jsonText);
        }
    }
}
