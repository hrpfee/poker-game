package poker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import poker.controller.GameController;
import poker.view.GameView;
import poker.view.LoginView;

/**
 * ポーカークライアントのエントリーポイント
 *
 * 起動方法:
 *   ./gradlew run
 *
 * サーバーが ws://localhost:8080/poker で起動している必要があります。
 */
public class PokerApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("♠ Poker - Texas Hold'em ♣");
        stage.setResizable(false);

        // ログイン画面からスタート
        LoginView loginView = new LoginView();
        Scene scene = new Scene(loginView, 900, 650);

        loginView.setOnJoin(playerName -> {
            // ゲーム画面へ切り替え
            GameView gameView = new GameView();
            GameController controller = new GameController(gameView);

            Platform.runLater(() -> {
                scene.setRoot(gameView);
                stage.setTitle("♠ Poker — " + playerName);
            });

            // サーバーへ接続
            controller.connect(playerName);

            // ウィンドウを閉じたら切断
            stage.setOnCloseRequest(e -> controller.disconnect());
        });

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
