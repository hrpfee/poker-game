package poker.view;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import poker.model.Card;

/**
 * 相手プレイヤー1人分の情報を表示するコンポーネント
 * 名前・チップ・手札（裏向き）・ターン表示を含む
 */
public class PlayerPane extends VBox {

    private final Label nameLabel = new Label();
    private final Label chipsLabel = new Label();
    private final Label positionLabel = new Label();
    private final Label statusLabel = new Label();
    private final HBox cardsBox = new HBox(6);
    private final Circle turnIndicator = new Circle(6);
    private final CardView card1 = new CardView(null, true);
    private final CardView card2 = new CardView(null, true);

    private boolean active = true;

    public PlayerPane() {
        buildUI();
    }

    private void buildUI() {
        setAlignment(Pos.CENTER);
        setSpacing(6);
        setPadding(new Insets(12, 16, 12, 16));
        setStyle(
            "-fx-background-color: rgba(0,0,0,0.45);" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 1;"
        );
        setPrefWidth(160);

        // ターンインジケーター
        turnIndicator.setFill(Color.TRANSPARENT);
        turnIndicator.setStroke(Color.TRANSPARENT);

        // 名前
        nameLabel.setStyle(
            "-fx-font-size: 15px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #f0e6c8;" +
            "-fx-font-family: 'Georgia';"
        );

        // ポジション（SB/BB）
        positionLabel.setStyle(
            "-fx-font-size: 11px;" +
            "-fx-text-fill: #f0c040;" +
            "-fx-font-weight: bold;"
        );

        // チップ
        chipsLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #aad4a0;"
        );

        // ステータス（Fold等）
        statusLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #ff7070;" +
            "-fx-font-weight: bold;"
        );
        statusLabel.setVisible(false);

        // 手札（裏向き）
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.getChildren().addAll(card1, card2);

        HBox nameRow = new HBox(6, turnIndicator, nameLabel);
        nameRow.setAlignment(Pos.CENTER);

        getChildren().addAll(nameRow, positionLabel, cardsBox, chipsLabel, statusLabel);
    }

    // ============ 公開メソッド ============

    public void setPlayerName(String name) {
        nameLabel.setText(name);
    }

    public void setChips(int chips) {
        chipsLabel.setText("💰 " + String.format("%,d", chips));
    }

    public void setPosition(String position) {
        if (position == null || position.equals("none") || position.isEmpty()) {
            positionLabel.setText("");
            positionLabel.setVisible(false);
        } else {
            positionLabel.setText("[ " + position + " ]");
            positionLabel.setVisible(true);
        }
    }

    /** このプレイヤーがフォールドした時 */
    public void showFolded() {
        statusLabel.setText("FOLD");
        statusLabel.setVisible(true);

        // カードを半透明に
        card1.setOpacity(0.35);
        card2.setOpacity(0.35);
        setStyle(
            "-fx-background-color: rgba(0,0,0,0.25);" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.05);" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 1;"
        );
    }

    /** このプレイヤーのターンになった時にハイライト */
    public void setCurrentTurn(boolean isTurn) {
        if (isTurn) {
            turnIndicator.setFill(Color.LIMEGREEN);
            turnIndicator.setStroke(Color.WHITE);

            // 点滅アニメーション
            FadeTransition ft = new FadeTransition(Duration.millis(700), turnIndicator);
            ft.setFromValue(1.0);
            ft.setToValue(0.3);
            ft.setCycleCount(FadeTransition.INDEFINITE);
            ft.setAutoReverse(true);
            ft.play();

            setStyle(
                "-fx-background-color: rgba(0,60,20,0.55);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: #4CAF50;" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 2;"
            );
        } else {
            turnIndicator.setFill(Color.TRANSPARENT);
            turnIndicator.setStroke(Color.TRANSPARENT);
            setStyle(
                "-fx-background-color: rgba(0,0,0,0.45);" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(255,255,255,0.12);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;"
            );
        }
    }

    /** ラウンド開始時にリセット */
    public void reset() {
        statusLabel.setVisible(false);
        card1.setOpacity(1.0);
        card2.setOpacity(1.0);
        setCurrentTurn(false);
        setStyle(
            "-fx-background-color: rgba(0,0,0,0.45);" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: rgba(255,255,255,0.12);" +
            "-fx-border-radius: 14;" +
            "-fx-border-width: 1;"
        );
    }

    /** ゲーム終了時に相手の手札を表向きで表示 */
    public void revealCards(Card c1, Card c2) {
        card1.setOpacity(1.0);
        card2.setOpacity(1.0);
        card1.setCard(c1);
        card2.setCard(c2);
        card1.flip(true);
        card2.flip(true);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}