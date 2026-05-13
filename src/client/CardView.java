package poker.view;

import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import poker.model.Card;

/**
 * トランプ1枚を表示するJavaFXコンポーネント
 * 表向き / 裏向きの切り替えとフリップアニメーションをサポート
 */
public class CardView extends StackPane {

    private static final double CARD_WIDTH = 70;
    private static final double CARD_HEIGHT = 100;
    private static final double CORNER_RADIUS = 8;

    private Card card;
    private boolean faceDown;

    // カード表面のUI要素
    private final Rectangle background = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
    private final Label rankTopLabel = new Label();
    private final Label suitTopLabel = new Label();
    private final Label rankBottomLabel = new Label();
    private final Label suitBottomLabel = new Label();
    private final Label centerSuitLabel = new Label();
    private final VBox frontContent = new VBox(2);

    // 裏面のUI要素
    private final Rectangle backBackground = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
    private final Label backPattern = new Label("🂠");

    public CardView() {
        this(null, true);
    }

    public CardView(Card card, boolean faceDown) {
        this.card = card;
        this.faceDown = faceDown;
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        buildCard();
        updateDisplay();
    }

    // ============ UI構築 ============

    private void buildCard() {
        // 表面の背景
        background.setArcWidth(CORNER_RADIUS * 2);
        background.setArcHeight(CORNER_RADIUS * 2);
        background.setFill(Color.WHITE);
        background.setStroke(Color.rgb(180, 180, 180));
        background.setStrokeWidth(1.5);
        background.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.3)));

        // ランク（左上）
        rankTopLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Georgia';");
        suitTopLabel.setStyle("-fx-font-size: 13px;");

        // 中央スート
        centerSuitLabel.setStyle("-fx-font-size: 28px;");
        centerSuitLabel.setAlignment(Pos.CENTER);

        // ランク（右下・逆さ）
        rankBottomLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-font-family: 'Georgia'; -fx-rotate: 180;");
        suitBottomLabel.setStyle("-fx-font-size: 13px; -fx-rotate: 180;");

        // 左上のコーナー情報
        VBox topLeft = new VBox(0, rankTopLabel, suitTopLabel);
        topLeft.setAlignment(Pos.TOP_LEFT);
        topLeft.setTranslateX(-CARD_WIDTH / 2 + 40);
        topLeft.setTranslateY(-CARD_HEIGHT / 2 + 50);

        // 中央
        centerSuitLabel.setTranslateX(0);
        centerSuitLabel.setTranslateY(0);

        // 右下のコーナー情報
        VBox bottomRight_label = new VBox(0, rankBottomLabel);
        bottomRight_label.setAlignment(Pos.BOTTOM_RIGHT);
        bottomRight_label.setTranslateX(CARD_WIDTH / 2 - 40);
        bottomRight_label.setTranslateY(CARD_HEIGHT / 2 - 50);
        VBox bottomRight_suit = new VBox(0, suitBottomLabel);
        bottomRight_suit.setAlignment(Pos.BOTTOM_RIGHT);
        bottomRight_suit.setTranslateX(CARD_WIDTH / 2 - 40);
        bottomRight_suit.setTranslateY(CARD_HEIGHT / 2 - 70);


        // 裏面
        backBackground.setArcWidth(CORNER_RADIUS * 2);
        backBackground.setArcHeight(CORNER_RADIUS * 2);
        backBackground.setFill(Color.rgb(30, 80, 160));
        backBackground.setStroke(Color.rgb(20, 60, 130));
        backBackground.setStrokeWidth(1.5);
        backBackground.setEffect(new javafx.scene.effect.DropShadow(6, Color.rgb(0, 0, 0, 0.3)));

        backPattern.setStyle("-fx-font-size: 50px; -fx-text-fill: rgba(255,255,255,0.15);");

        getChildren().addAll(
            background,
            topLeft,
            centerSuitLabel,
            bottomRight_label,
            bottomRight_suit,
            backBackground,
            backPattern
        );
    }

    private void updateDisplay() {
        if (faceDown || card == null) {
            // 裏向き表示
            background.setVisible(false);
            rankTopLabel.setVisible(false);
            suitTopLabel.setVisible(false);
            centerSuitLabel.setVisible(false);
            rankBottomLabel.setVisible(false);
            backBackground.setVisible(true);
            backPattern.setVisible(true);
        } else {
            // 表向き表示
            background.setFill(Color.WHITE);
            background.setStroke(Color.rgb(180, 180, 180));
            background.getStrokeDashArray().clear();
            String colorStyle = card.isRed()
                ? "-fx-text-fill: #cc2200;"
                : "-fx-text-fill: #111111;";

            rankTopLabel.setText(card.getRank().getDisplay());
            rankTopLabel.setStyle(rankTopLabel.getStyle() + colorStyle);

            suitTopLabel.setText(card.getSuit().getSymbol());
            suitTopLabel.setStyle(suitTopLabel.getStyle() + colorStyle);

            centerSuitLabel.setText(card.getSuit().getSymbol());
            centerSuitLabel.setStyle(centerSuitLabel.getStyle() + colorStyle);

            rankBottomLabel.setText(card.getRank().getDisplay());
            rankBottomLabel.setStyle(rankBottomLabel.getStyle() + colorStyle);

            suitBottomLabel.setText(card.getSuit().getSymbol());
            suitBottomLabel.setStyle(suitBottomLabel.getStyle() + colorStyle);

            background.setVisible(true);
            rankTopLabel.setVisible(true);
            suitTopLabel.setVisible(true);
            centerSuitLabel.setVisible(true);
            rankBottomLabel.setVisible(true);
            backBackground.setVisible(false);
            backPattern.setVisible(false);
        }
    }

    // ============ 公開メソッド ============

    /** カードを設定して表示を更新 */
    public void setCard(Card card) {
        this.card = card;
        updateDisplay();
    }

    /** 表向き / 裏向きを切り替え（アニメーションあり） */
    public void flip(boolean showFace) {
        RotateTransition rt1 = new RotateTransition(Duration.millis(200), this);
        rt1.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
        rt1.setFromAngle(0);
        rt1.setToAngle(90);
        rt1.setOnFinished(e -> {
            this.faceDown = !showFace;
            updateDisplay();
            RotateTransition rt2 = new RotateTransition(Duration.millis(200), this);
            rt2.setAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
            rt2.setFromAngle(-90);
            rt2.setToAngle(0);
            rt2.play();
        });
        rt1.play();
    }

    /** 空のカード（プレースホルダー）として表示 */
    public void showEmpty() {
        background.setFill(Color.rgb(255, 255, 255, 0.1));
        background.setStroke(Color.rgb(255, 255, 255, 0.3));
        background.getStrokeDashArray().addAll(4.0, 4.0);
        background.setVisible(true);
        rankTopLabel.setVisible(false);
        suitTopLabel.setVisible(false);
        centerSuitLabel.setVisible(false);
        rankBottomLabel.setVisible(false);
        backBackground.setVisible(false);
        backPattern.setVisible(false);
    }

    public Card getCard() { return card; }
    public boolean isFaceDown() { return faceDown; }
}