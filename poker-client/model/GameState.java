package poker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * クライアント側が保持するゲーム状態
 */
public class GameState {

    public enum Phase {
        WAITING,        // 待機中（プレイヤー集合中）
        PRE_FLOP,       // プリフロップ
        FLOP,           // フロップ
        TURN,           // ターン
        RIVER,          // リバー
        SHOWDOWN        // 勝敗判定
    }

    // 自分の情報
    private String myId;
    private String myName;
    private int myChips = 10000;
    private List<Card> myHand = new ArrayList<>();
    private String myPosition;   // "SB", "BB", "none"
    private boolean myTurn = false;
    private boolean folded = false;

    // コミュニティカード
    private List<Card> communityCards = new ArrayList<>();

    // ゲーム進行状態
    private Phase phase = Phase.WAITING;
    private int pot = 0;
    private int currentBet = 0;

    // 相手プレイヤー情報（表示用）
    private List<PlayerInfo> opponents = new ArrayList<>();

    // ============ 内部クラス ============

    public static class PlayerInfo {
        private String id;
        private String name;
        private int chips;
        private String position;
        private boolean folded;
        private boolean isCurrentTurn;
        private int currentBet;

        public PlayerInfo(String id, String name, int chips, String position) {
            this.id = id;
            this.name = name;
            this.chips = chips;
            this.position = position;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getChips() { return chips; }
        public void setChips(int chips) { this.chips = chips; }
        public String getPosition() { return position; }
        public boolean isFolded() { return folded; }
        public void setFolded(boolean folded) { this.folded = folded; }
        public boolean isCurrentTurn() { return isCurrentTurn; }
        public void setCurrentTurn(boolean currentTurn) { isCurrentTurn = currentTurn; }
        public int getCurrentBet() { return currentBet; }
        public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }
    }

    // ============ メソッド ============

    public void reset() {
        myHand.clear();
        communityCards.clear();
        opponents.forEach(p -> {
            p.setFolded(false);
            p.setCurrentTurn(false);
            p.setCurrentBet(0);
        });
        pot = 0;
        currentBet = 0;
        myTurn = false;
        folded = false;
        phase = Phase.PRE_FLOP;
    }

    // ============ ゲッター / セッター ============

    public String getMyId() { return myId; }
    public void setMyId(String myId) { this.myId = myId; }

    public String getMyName() { return myName; }
    public void setMyName(String myName) { this.myName = myName; }

    public int getMyChips() { return myChips; }
    public void setMyChips(int myChips) { this.myChips = myChips; }

    public List<Card> getMyHand() { return myHand; }

    public String getMyPosition() { return myPosition; }
    public void setMyPosition(String myPosition) { this.myPosition = myPosition; }

    public boolean isMyTurn() { return myTurn; }
    public void setMyTurn(boolean myTurn) { this.myTurn = myTurn; }

    public boolean isFolded() { return folded; }
    public void setFolded(boolean folded) { this.folded = folded; }

    public List<Card> getCommunityCards() { return communityCards; }

    public Phase getPhase() { return phase; }
    public void setPhase(Phase phase) { this.phase = phase; }

    public int getPot() { return pot; }
    public void setPot(int pot) { this.pot = pot; }

    public int getCurrentBet() { return currentBet; }
    public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }

    public List<PlayerInfo> getOpponents() { return opponents; }
}
