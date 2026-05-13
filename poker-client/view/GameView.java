package poker.view;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.util.Duration;
import poker.model.Card;
import poker.model.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameView extends StackPane {

    // ============ UI コンポーネント ============
    private final Ellipse tableOuter = new Ellipse(380, 190);
    private final Ellipse tableInner = new Ellipse(360, 175);

    private final HBox communityCardsBox = new HBox(10);
    private final List<CardView> communityCardViews = new ArrayList<>();

    private final Label potLabel    = new Label("POT: 0");
    private final Label phaseLabel  = new Label("WAITING");

    private final HBox myHandBox = new HBox(12);
    private final List<CardView> myCardViews = new ArrayList<>();

    private final Label myNameLabel     = new Label("YOU");
    private final Label myChipsLabel    = new Label("10,000");
    private final Label myPositionLabel = new Label();
    private final Label myBetLabel      = new Label();

    private final PlayerPane opponent1Pane = new PlayerPane();
    private final PlayerPane opponent2Pane = new PlayerPane();

    private final Button callButton  = new Button("CALL");
    private final Button raiseButton = new Button("RAISE");
    private final Button foldButton  = new Button("FOLD");
    private final Button checkButton = new Button("CHECK");
    private final Spinner<Integer> raiseSpinner = new Spinner<>(200, 100000, 400, 200);

    // ログはListViewで日本語フォントを明示的に指定
    private final ListView<String> logList = new ListView<>();

    private final VBox resultOverlay = new VBox();

    private Consumer<Void>    onCall;
    private Consumer<Integer> onRaise;
    private Consumer<Void>    onFold;
    private Consumer<Void>    onCheck;

    public GameView() {
        buildUI();
    }

    // ============ UI構築 ============

    private void buildUI() {
        setPrefSize(1000, 680);
        // 背景：濃い緑ではなく落ち着いたダークグレー
        setStyle("-fx-background-color: #1c1c2e;");

        buildTable();
        buildOpponentPanes();
        buildMyArea();
        buildResultOverlay();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: transparent;");

                // テーブルエリア
         AnchorPane tableArea = new AnchorPane();
        tableArea.setPrefSize(1000, 500);

        // テーブル本体
        StackPane centerTable = new StackPane(
        tableOuter,
        tableInner,
        buildCenterContent()
        );

        centerTable.setPrefSize(800, 400);
        centerTable.setLayoutX(500);
        centerTable.setLayoutY(250);

        // 相手プレイヤー配置
        opponent1Pane.setLayoutX(400);
        opponent1Pane.setLayoutY(350);

        opponent2Pane.setLayoutX(1200);
        opponent2Pane.setLayoutY(350);

        tableArea.getChildren().addAll(
            centerTable,
            opponent1Pane,
            opponent2Pane
        );

        root.setCenter(tableArea);
        root.setBottom(buildBottomArea());
        root.setRight(buildLogPanel());

        getChildren().addAll(root, resultOverlay);
        StackPane.setAlignment(resultOverlay, Pos.CENTER);
    }

    private void buildTable() {
        // 外枠（ゴールド）
        tableOuter.setFill(Color.rgb(60, 40, 10));
        tableOuter.setStroke(Color.rgb(180, 140, 50));
        tableOuter.setStrokeWidth(4);

        // フェルト（深い緑）
        LinearGradient felt = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(20, 80, 40)),
            new Stop(1, Color.rgb(10, 55, 25))
        );
        tableInner.setFill(felt);
    }

    private VBox buildCenterContent() {
        phaseLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #ffffff;" +
            "-fx-font-weight: bold;"
        );

        potLabel.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #ffd700;"
        );

        for (int i = 0; i < 5; i++) {
            CardView cv = new CardView();
            cv.showEmpty();
            communityCardViews.add(cv);
            communityCardsBox.getChildren().add(cv);
        }
        communityCardsBox.setAlignment(Pos.CENTER);
        communityCardsBox.setSpacing(10);

        VBox center = new VBox(10, phaseLabel, communityCardsBox, potLabel);
        center.setAlignment(Pos.CENTER);
        return center;
    }

    private void buildOpponentPanes() {
        opponent1Pane.setPlayerName("Player 2");
        opponent1Pane.setChips(10000);
        opponent2Pane.setPlayerName("Player 3");
        opponent2Pane.setChips(10000);
    }

    private void buildMyArea() {
        for (int i = 0; i < 2; i++) {
            CardView cv = new CardView();
            cv.showEmpty();
            myCardViews.add(cv);
            myHandBox.getChildren().add(cv);
        }
        myHandBox.setAlignment(Pos.CENTER);
        myHandBox.setSpacing(12);

        // 明るい色で見やすく
        myNameLabel.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;" +
            "-fx-text-fill: #ffffff;"
        );
        myChipsLabel.setStyle(
            "-fx-font-size: 15px; -fx-text-fill: #00e676; -fx-font-weight: bold;"
        );
        myPositionLabel.setStyle(
            "-fx-font-size: 13px; -fx-text-fill: #ffd700; -fx-font-weight: bold;"
        );
        myBetLabel.setStyle(
            "-fx-font-size: 13px; -fx-text-fill: #ffffff;"
        );
    }

    private HBox buildBottomArea() {
        // 自分の情報パネル（明るい背景）
        Label chipsIcon = new Label("CHIPS");
        chipsIcon.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaaaaa;");

        VBox myInfoBox = new VBox(4, myNameLabel, myPositionLabel, chipsIcon, myChipsLabel, myBetLabel);
        myInfoBox.setAlignment(Pos.CENTER_LEFT);
        myInfoBox.setPadding(new Insets(12));
        myInfoBox.setStyle(
            "-fx-background-color: #2d2d44;" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: #5555aa;" +
            "-fx-border-radius: 12;" +
            "-fx-border-width: 2;"
        );
        myInfoBox.setPrefWidth(160);

        HBox actionButtons = buildActionButtons();

        Label handTitle = new Label("YOUR HAND");
        handTitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #aaaaaa; -fx-font-weight: bold;");
        VBox myHandSection = new VBox(6, handTitle, myHandBox);
        myHandSection.setAlignment(Pos.CENTER);

        HBox bottom = new HBox(20, myInfoBox, myHandSection, actionButtons);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(14));
        bottom.setStyle("-fx-background-color: #12122a;");
        return bottom;
    }

    private HBox buildActionButtons() {
        // 色を明るくしてはっきり見えるように
        styleButton(checkButton, "#1565c0", "#1976d2");  // 青
        styleButton(callButton,  "#2e7d32", "#388e3c");  // 緑
        styleButton(raiseButton, "#f57f17", "#f9a825");  // オレンジ
        styleButton(foldButton,  "#c62828", "#d32f2f");  // 赤

        raiseSpinner.setPrefWidth(110);
        raiseSpinner.setStyle("-fx-font-size: 13px;");

        callButton.setOnAction(e ->  { if (onCall  != null) onCall.accept(null); });
        foldButton.setOnAction(e ->  { if (onFold  != null) onFold.accept(null); });
        checkButton.setOnAction(e -> { if (onCheck != null) onCheck.accept(null); });
        raiseButton.setOnAction(e -> { if (onRaise != null) onRaise.accept(raiseSpinner.getValue()); });

        setActionsEnabled(false);

        Label raiseLabel = new Label("RAISE AMOUNT");
        raiseLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #cccccc;");

        VBox raiseBox = new VBox(3, raiseLabel, raiseSpinner);
        raiseBox.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(12, checkButton, callButton, raiseBox, raiseButton, foldButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setPadding(new Insets(0, 10, 0, 10));
        return buttons;
    }

    private void styleButton(Button btn, String normalColor, String hoverColor) {
        String base =
            "-fx-font-size: 15px; -fx-font-weight: bold;" +
            "-fx-text-fill: white; -fx-cursor: hand;" +
            "-fx-background-radius: 8; -fx-padding: 12 24 12 24;" +
            "-fx-background-color: " + normalColor + ";";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(base.replace(normalColor, hoverColor)));
        btn.setOnMouseExited(e -> btn.setStyle(base));
    }

    private VBox buildLogPanel() {
        logList.setPrefWidth(220);
        logList.setPrefHeight(500);
        logList.setStyle(
            "-fx-control-inner-background: #1a1a2e;" +
            "-fx-background-color: #1a1a2e;"
        );

        // 日本語対応フォントをJavaコードで直接指定（CSSより確実）
        // Noto Sans CJK JP が入っていればそれを使い、なければシステムデフォルト
        Font jpFont = Font.font("Noto Sans CJK JP", 12);
        if (jpFont.getFamily().equals("System")) {
            jpFont = Font.font("IPAGothic", 12);
        }
        if (jpFont.getFamily().equals("System")) {
            jpFont = Font.font("TakaoGothic", 12);
        }
        final Font finalFont = jpFont;

        logList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setFont(finalFont);
                    setStyle(
                        "-fx-text-fill: #e0e0e0;" +
                        "-fx-background-color: transparent;"
                    );
                }
            }
        });

        Label logTitle = new Label("GAME LOG");
        logTitle.setStyle(
            "-fx-font-size: 12px; -fx-text-fill: #ffffff;" +
            "-fx-font-weight: bold;"
        );

        VBox panel = new VBox(8, logTitle, logList);
        panel.setPadding(new Insets(12));
        panel.setStyle("-fx-background-color: #12122a;");
        return panel;
    }

    private void buildResultOverlay() {
        resultOverlay.setAlignment(Pos.CENTER);
        resultOverlay.setSpacing(16);
        resultOverlay.setVisible(false);
        resultOverlay.setStyle(
            "-fx-background-color: rgba(0,0,0,0.9);" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 40;" +
            "-fx-border-color: #ffd700;" +
            "-fx-border-width: 2;" +
            "-fx-border-radius: 20;"
        );
        resultOverlay.setMaxSize(420, 280);
    }

    // ============ ゲーム状態の更新メソッド ============

    public void updateMyInfo(String name, int chips, String position) {
        Platform.runLater(() -> {
            myNameLabel.setText(name);
            myChipsLabel.setText(String.format("%,d", chips));
            if (position != null && !position.equals("none") && !position.isEmpty()) {
                myPositionLabel.setText("[ " + position + " ]");
                myPositionLabel.setVisible(true);
            } else {
                myPositionLabel.setVisible(false);
            }
        });
    }

    public void setMyHand(List<Card> cards) {
        Platform.runLater(() -> {
            for (int i = 0; i < Math.min(cards.size(), myCardViews.size()); i++) {
                myCardViews.get(i).setCard(cards.get(i));
                myCardViews.get(i).flip(true);
            }
        });
    }

    public void setCommunityCards(List<Card> cards) {
        Platform.runLater(() -> {
            for (int i = 0; i < communityCardViews.size(); i++) {
                if (i < cards.size()) {
                    communityCardViews.get(i).setCard(cards.get(i));
                    communityCardViews.get(i).flip(true);
                }
            }
        });
    }

    public void updateGameStatus(int pot, String phase) {
        Platform.runLater(() -> {
            potLabel.setText("POT  " + String.format("%,d", pot));
            phaseLabel.setText("[ " + phase + " ]");
        });
    }

    public void updateOpponent(int index, String name, int chips, String position) {
        Platform.runLater(() -> {
            PlayerPane pane = index == 0 ? opponent1Pane : opponent2Pane;
            pane.setPlayerName(name);
            pane.setChips(chips);
            pane.setPosition(position);
        });
    }

    public void setOpponentTurn(int index, boolean isTurn) {
        Platform.runLater(() -> {
            (index == 0 ? opponent1Pane : opponent2Pane).setCurrentTurn(isTurn);
        });
    }

    public void showOpponentFolded(int index) {
        Platform.runLater(() -> {
            (index == 0 ? opponent1Pane : opponent2Pane).showFolded();
        });
    }

    public void setActionsEnabled(boolean enabled) {
        Platform.runLater(() -> {
            callButton.setDisable(!enabled);
            raiseButton.setDisable(!enabled);
            foldButton.setDisable(!enabled);
            checkButton.setDisable(!enabled);
            raiseSpinner.setDisable(!enabled);
        });
    }

    /** ログ追加（ListViewで日本語文字化けを回避） */
    public void addLog(String message) {
        Platform.runLater(() -> {
            logList.getItems().add(message);
            // 最新行に自動スクロール
            logList.scrollTo(logList.getItems().size() - 1);
        });
    }

    private static final String JP_FONT = detectJpFont();

    private static String detectJpFont() {
        for (String name : new String[]{"Noto Sans CJK JP", "IPAGothic", "TakaoGothic", "Noto Sans JP"}) {
            Font f = Font.font(name, 14);
            if (!f.getFamily().equals("System")) return name;
        }
        return "System";
    }

    public void showResult(boolean isWinner, String winnerName, String handName) {
        Platform.runLater(() -> {
            resultOverlay.getChildren().clear();

            String title      = isWinner ? "YOU WIN!" : "YOU LOSE";
            String titleColor = isWinner ? "#ffd700"  : "#ff5252";

            Label titleLabel = new Label(title);
            titleLabel.setStyle(
                "-fx-font-size: 40px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + titleColor + ";"
            );

            Label winnerLabel = new Label("Winner: " + winnerName);
            winnerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #ffffff;");

            Label handLabel = new Label(handName);
            handLabel.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-text-fill: #69f0ae;" + 
                "-fx-font-weight: bold;" +
                "-fx-font-family: '" + JP_FONT + "';"
            );

            Button nextButton = new Button("NEXT ROUND");
            styleButton(nextButton, "#2e7d32", "#388e3c");
            nextButton.setOnAction(e -> hideResult());

            resultOverlay.getChildren().addAll(titleLabel, winnerLabel, handLabel, nextButton);
            resultOverlay.setVisible(true);

            FadeTransition ft = new FadeTransition(Duration.millis(400), resultOverlay);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        });
    }

    public void hideResult() {
        Platform.runLater(() -> {
            FadeTransition ft = new FadeTransition(Duration.millis(300), resultOverlay);
            ft.setFromValue(1);
            ft.setToValue(0);
            ft.setOnFinished(e -> resultOverlay.setVisible(false));
            ft.play();
        });
    }

    public void resetRound() {
        Platform.runLater(() -> {
            communityCardViews.forEach(cv -> cv.showEmpty());
            myCardViews.forEach(cv -> cv.showEmpty());
            opponent1Pane.reset();
            opponent2Pane.reset();
            updateGameStatus(0, "PRE-FLOP");
        });
    }

    /** ゲーム終了時に相手の手札を表向きで表示 */
    public void revealOpponentCards(int index, Card c1, Card c2) {
        Platform.runLater(() -> {
            PlayerPane pane = index == 0 ? opponent1Pane : opponent2Pane;
            pane.revealCards(c1, c2);
        });
    }

    public void setOnCall(Consumer<Void> onCall)       { this.onCall  = onCall;  }
    public void setOnRaise(Consumer<Integer> onRaise)  { this.onRaise = onRaise; }
    public void setOnFold(Consumer<Void> onFold)       { this.onFold  = onFold;  }
    public void setOnCheck(Consumer<Void> onCheck)     { this.onCheck = onCheck; }
}
