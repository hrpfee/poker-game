package poker.view;

import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import poker.model.Card;

import java.io.InputStream;

/**
 * トランプ1枚を表示するJavaFXコンポーネント（画像版）
 *
 * =====================================================
 *  ★ ファイル名の形式をここだけ変更すればOK ★
 *  toImageFileName() メソッドを編集してください
 * =====================================================
 */
public class CardView extends StackPane {

    private static final double CARD_WIDTH  = 80;
    private static final double CARD_HEIGHT = 112;

    private Card card;
    private boolean faceDown;

    // 表面・裏面のImageView
    private final ImageView frontImageView = new ImageView();
    private final ImageView backImageView  = new ImageView();

    // 画像が見つからない場合のフォールバック用Rectangle
    private final Rectangle emptyRect = new Rectangle(CARD_WIDTH, CARD_HEIGHT);

    public CardView() {
        this(null, true);
    }

    public CardView(Card card, boolean faceDown) {
        this.card     = card;
        this.faceDown = faceDown;
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        buildCard();
        updateDisplay();
    }

    // ============================================================
    //  ★ ファイル名の変換ルール（ここだけ編集してください） ★
    // ============================================================

    /**
     * Cardオブジェクト → 画像ファイル名 の変換メソッド
     *
     * 現在の形式: AH.png, KS.png, 10D.png, 2C.png
     *   （カードコードそのまま。サーバーとの通信コードと同じ形式）
     *
     * ---- 他の形式に変更する場合の例 ----
     *
     * 「ace_of_hearts.png」形式の場合:
     *   return rank.toLowerCase() + "_of_" + suit.toLowerCase() + ".png";
     *   ※ rank は "ACE","KING"... suit は "HEARTS","SPADES"...
     *
     * 「1_heart.png」形式の場合:
     *   return card.getRank().getValue() + "_" + suitName + ".png";
     *
     * 「hearts_A.png」形式の場合:
     *   return suitName + "_" + card.getRank().getDisplay() + ".png";
     */
    private String toImageFileName(Card c) {
        // ===== ここのreturn文を自分のファイル名形式に合わせて変更 =====
        String rank = c.getRank().getDisplay(); // "A","K","Q","J","10","9"..."2"
        String suit = switch (c.getSuit()) {
            case HEARTS   -> "H";
            case SPADES   -> "S";
            case DIAMONDS -> "D";
            case CLUBS    -> "C";
        };
        return rank + suit + ".png";  // 例: AH.png, KS.png, 10D.png
    }

    /** 裏面の画像ファイル名 */
    private String backImageFileName() {
        return "back.png";  // 裏面画像のファイル名（必要に応じて変更）
    }

    // ============================================================

    private void buildCard() {
        // ImageViewの共通設定
        frontImageView.setFitWidth(CARD_WIDTH);
        frontImageView.setFitHeight(CARD_HEIGHT);
        frontImageView.setPreserveRatio(false);

        backImageView.setFitWidth(CARD_WIDTH);
        backImageView.setFitHeight(CARD_HEIGHT);
        backImageView.setPreserveRatio(false);

        // 裏面画像を読み込む
        Image backImg = loadImage(backImageFileName());
        if (backImg != null) {
            backImageView.setImage(backImg);
        } else {
            // 裏面画像がない場合は青い矩形で代替
            Rectangle fallback = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
            fallback.setArcWidth(12);
            fallback.setArcHeight(12);
            fallback.setFill(Color.rgb(30, 80, 160));
            getChildren().add(fallback);
        }

        // プレースホルダー（空のカード枠）
        emptyRect.setArcWidth(12);
        emptyRect.setArcHeight(12);
        emptyRect.setFill(Color.rgb(255, 255, 255, 0.08));
        emptyRect.setStroke(Color.rgb(255, 255, 255, 0.3));
        emptyRect.getStrokeDashArray().addAll(4.0, 4.0);
        emptyRect.setVisible(false);

        getChildren().addAll(emptyRect, backImageView, frontImageView);
        setAlignment(Pos.CENTER);
    }

    private void updateDisplay() {
        emptyRect.setVisible(false);

        if (faceDown || card == null) {
            // 裏向き：裏面画像を表示
            backImageView.setVisible(true);
            frontImageView.setVisible(false);
        } else {
            // 表向き：表面画像を読み込んで表示
            Image img = loadImage(toImageFileName(card));
            if (img != null) {
                frontImageView.setImage(img);
                frontImageView.setVisible(true);
                backImageView.setVisible(false);
            } else {
                // 画像ファイルが見つからない場合のフォールバック表示
                System.err.println("[CardView] 画像が見つかりません: " + toImageFileName(card));
                frontImageView.setVisible(false);
                backImageView.setVisible(false);
                showFallbackText(card);
            }
        }
    }

    /**
     * resources/ フォルダ（srcと同階層）から画像を読み込む
     * クラスパスに resources/ が含まれているので
     * getResourceAsStream("/ファイル名") で直接取得できる
     */
    private Image loadImage(String fileName) {
        try {
            // クラスパスのルート（= resources/）から検索
            InputStream is = getClass().getResourceAsStream("/" + fileName);
            if (is == null) {
                System.err.println("[CardView] 画像が見つかりません: resources/" + fileName);
                return null;
            }
            return new Image(is);
        } catch (Exception e) {
            System.err.println("[CardView] 画像読み込みエラー: " + fileName + " / " + e.getMessage());
            return null;
        }
    }

    /** 画像がない場合のテキストフォールバック */
    private void showFallbackText(Card c) {
        emptyRect.setFill(Color.WHITE);
        emptyRect.setStroke(Color.rgb(180, 180, 180));
        emptyRect.getStrokeDashArray().clear();
        emptyRect.setVisible(true);
        // テキストラベルを追加（フォールバック用）
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(c.toString());
        String color = c.isRed() ? "#cc2200" : "#111111";
        lbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        if (!getChildren().contains(lbl)) getChildren().add(lbl);
    }

    // ============ 公開メソッド ============

    public void setCard(Card card) {
        this.card = card;
        updateDisplay();
    }

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

    public void showEmpty() {
        emptyRect.setFill(Color.rgb(255, 255, 255, 0.08));
        emptyRect.setStroke(Color.rgb(255, 255, 255, 0.3));
        emptyRect.getStrokeDashArray().clear();
        emptyRect.getStrokeDashArray().addAll(4.0, 4.0);
        emptyRect.setVisible(true);
        frontImageView.setVisible(false);
        backImageView.setVisible(false);
    }

    public Card getCard()      { return card; }
    public boolean isFaceDown(){ return faceDown; }
}