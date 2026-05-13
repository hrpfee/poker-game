package poker.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.control.*;

import java.util.function.Consumer;

public class LoginView extends StackPane {

    private final TextField nameField = new TextField();
    private Consumer<String> onJoin;

    // 日本語対応フォントを一箇所で管理
    private static final String JP_FONT = detectJpFont();

    private static String detectJpFont() {
        for (String name : new String[]{"Noto Sans CJK JP", "IPAGothic", "TakaoGothic", "Noto Sans JP"}) {
            Font f = Font.font(name, 14);
            if (!f.getFamily().equals("System")) return name;
        }
        return "System";
    }

    public LoginView() {
        buildUI();
    }

    private void buildUI() {
        setPrefSize(900, 650);

        // 背景
        Rectangle bg = new Rectangle(900, 650);
        LinearGradient bgGrad = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(10, 25, 10)),
            new Stop(1, Color.rgb(5, 15, 5))
        );
        bg.setFill(bgGrad);

        // タイトル（英語のみなのでGeorgiaでOK）
        Label title = new Label("POKER");
        title.setStyle(
            "-fx-font-size: 56px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: 'Georgia';" +
            "-fx-text-fill: #f0c040;"
        );

        Label subtitle = new Label("Texas Hold'em  3 Players");
        subtitle.setStyle(
            "-fx-font-size: 18px;" +
            "-fx-text-fill: rgba(240,198,64,0.6);" +
            "-fx-font-family: 'Georgia';"
        );

        // 日本語ラベル：JP_FONTを使用
        Label nameLabel = new Label("プレイヤー名を入力");
        nameLabel.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: rgba(255,255,255,0.8);" +
            "-fx-font-family: '" + JP_FONT + "';"
        );

        nameField.setPromptText("例: Alice");
        nameField.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-font-family: '" + JP_FONT + "';" +
            "-fx-background-color: rgba(255,255,255,0.1);" +
            "-fx-text-fill: white;" +
            "-fx-prompt-text-fill: rgba(255,255,255,0.3);" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(255,255,255,0.2);" +
            "-fx-border-radius: 8;" +
            "-fx-padding: 10 16 10 16;"
        );
        nameField.setPrefWidth(280);

        // 参加ボタン
        Button joinButton = new Button("ゲームに参加する");
        joinButton.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-font-family: '" + JP_FONT + "';" +
            "-fx-background-color: #2a7a3a;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 12 32 12 32;" +
            "-fx-cursor: hand;"
        );
        joinButton.setOnMouseEntered(e ->
            joinButton.setStyle(joinButton.getStyle().replace("#2a7a3a", "#3a9a4a")));
        joinButton.setOnMouseExited(e ->
            joinButton.setStyle(joinButton.getStyle().replace("#3a9a4a", "#2a7a3a")));

        joinButton.setOnAction(e -> handleJoin());
        nameField.setOnAction(e -> handleJoin());

        VBox card = new VBox(20, title, subtitle, nameLabel, nameField, joinButton);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(50, 60, 50, 60));
        card.setStyle(
            "-fx-background-color: rgba(0,0,0,0.5);" +
            "-fx-background-radius: 20;" +
            "-fx-border-color: rgba(240,198,64,0.2);" +
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;"
        );
        card.setMaxSize(420, 420);

        getChildren().addAll(bg, card);
        StackPane.setAlignment(card, Pos.CENTER);
    }

    private void handleJoin() {
        String name = nameField.getText().trim();
        if (!name.isEmpty() && onJoin != null) {
            onJoin.accept(name);
        }
    }

    public void setOnJoin(Consumer<String> onJoin) {
        this.onJoin = onJoin;
    }
}
