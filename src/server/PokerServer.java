import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * テキサスホールデム用 WebSocket サーバー。クライアントは {@link GameManager} が解釈する JSON を送受信する。
 * <p>
 * メッセージ例: {@code {"type":"JOIN","name":"Alice"}} 、
 * {@code {"type":"ACTION","action":"call","raiseAmount":0}}
 */
public class PokerServer extends WebSocketServer {

    private final GameManager gameManager = new GameManager();
    private final Map<WebSocket, ClientHandler> clients = new ConcurrentHashMap<>();

    public PokerServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        ClientHandler session = new ClientHandler(conn);
        clients.put(conn, session);
        gameManager.onOpen(session);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ClientHandler session = clients.remove(conn);
        if (session != null) {
            gameManager.onClose(session);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientHandler session = clients.get(conn);
        if (session != null) {
            gameManager.handleMessage(session, message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("PokerServer listening on port " + getPort());
    }

    public static void main(String[] args) throws Exception {
        int port = 8765;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        PokerServer server = new PokerServer(port);
        server.setReuseAddr(true);
        server.start();
    }
}
