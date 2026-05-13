package poker.model;

import java.util.List;

/**
 * サーバーとクライアント間でやり取りするメッセージクラス
 * JSON形式でシリアライズ/デシリアライズする
 */
public class GameMessage {

    /** メッセージの種類 */
    public enum Type {
        // サーバー → クライアント
        GAME_START,       // ゲーム開始
        DEAL_CARDS,       // 手札配布
        FLOP,             // フロップ公開
        TURN,             // ターン公開
        RIVER,            // リバー公開
        YOUR_TURN,        // 自分のターン通知
        PLAYER_ACTION,    // 他プレイヤーのアクション通知
        GAME_RESULT,      // 勝敗結果
        CHIP_UPDATE,      // チップ更新
        PLAYER_JOINED,    // プレイヤー入室

        // クライアント → サーバー
        JOIN,             // 入室
        CALL,             // コール
        RAISE,            // レイズ
        FOLD,             // フォールド
        CHECK             // チェック
    }

    private Type type;
    private String playerId;
    private String playerName;
    private List<String> cards;        // カード情報（例: "AH", "KS"）
    private int amount;                // ベット額
    private int pot;                   // ポット総額
    private int chips;                 // チップ残高
    private String winnerId;           // 勝者ID
    private String winnerHand;         // 勝者の手役名
    private String message;            // 汎用メッセージ
    private int currentBet;            // 現在のベット額
    private String position;           // 席順（SB / BB / none）
    private java.util.Map<String, java.util.List<String>> allCards; // 全員の手札 playerId -> [card1, card2]

    // ============ コンストラクタ ============

    public GameMessage() {}

    public GameMessage(Type type) {
        this.type = type;
    }

    // ============ ゲッター / セッター ============

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public List<String> getCards() { return cards; }
    public void setCards(List<String> cards) { this.cards = cards; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public int getPot() { return pot; }
    public void setPot(int pot) { this.pot = pot; }

    public int getChips() { return chips; }
    public void setChips(int chips) { this.chips = chips; }

    public String getWinnerId() { return winnerId; }
    public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

    public String getWinnerHand() { return winnerHand; }
    public void setWinnerHand(String winnerHand) { this.winnerHand = winnerHand; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getCurrentBet() { return currentBet; }
    public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public java.util.Map<String, java.util.List<String>> getAllCards() { return allCards; }
    public void setAllCards(java.util.Map<String, java.util.List<String>> allCards) { this.allCards = allCards; }
}
