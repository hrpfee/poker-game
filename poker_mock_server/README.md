# Poker Mock Server

クライアント（JavaFX）の動作確認用モックサーバーです。  
Spring Bootは不要で、軽量なWebSocketサーバーのみで動きます。

---

## 起動手順

### 1. モックサーバーを起動（ターミナルA）

```bash
cd poker-mock-server
./gradlew run
```

起動すると以下のように表示されます：
```
========================================
  Mock Poker Server 起動
  ws://localhost:8080/poker
  接続待ち人数: 1 人
========================================
```

### 2. クライアントを起動（ターミナルB）

```bash
cd poker-client
./gradlew run
```

---

## ゲームの流れ（自動シミュレーション）

```
クライアントが接続・JOIN送信
        ↓
[自動] GAME_START
        ↓
[自動] DEAL_CARDS（手札2枚）
        ↓
[自動] YOUR_TURN → アクションボタンが有効になる
        ↓
CALL / RAISE / FOLD / CHECK を押す
        ↓
[自動] FLOP（コミュニティカード3枚）
        ↓
[自動] YOUR_TURN
        ↓
CALL / RAISE / FOLD / CHECK を押す
        ↓
[自動] TURN（4枚目）
        ↓
[自動] YOUR_TURN
        ↓
CALL / RAISE / FOLD / CHECK を押す
        ↓
[自動] RIVER（5枚目）
        ↓
[自動] YOUR_TURN
        ↓
CALL / RAISE / FOLD / CHECK を押す
        ↓
[自動] GAME_RESULT（勝敗表示）
```

---

## 設定変更

`MockPokerServer.java` の先頭にある定数を変更することで挙動を調整できます。

```java
// 接続人数（1にすると1人で即テスト可能）
private static final int REQUIRED_PLAYERS = 1;
```

---

## トラブルシューティング

| 症状 | 原因と対処 |
|------|-----------|
| `Address already in use` | 8080番ポートが使用中。他のプロセスを終了するか、`PORT`定数を変更する |
| クライアントが繋がらない | サーバーが起動しているか確認。`GameController.java`の`SERVER_URI`も確認 |
| `./gradlew: Permission denied` | `chmod +x gradlew` を実行してから再試行 |
