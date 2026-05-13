# Poker Client — JavaFX

Texas Hold'em ポーカーのクライアントアプリ（JavaFX製）

---

## ファイル構成

```
poker-client/
├── build.gradle
├── settings.gradle
└── src/main/java/poker/
    ├── PokerApp.java                   # エントリーポイント
    ├── model/
    │   ├── Card.java                   # カードモデル（スート・ランク）
    │   ├── GameMessage.java            # サーバーとの通信メッセージ
    │   └── GameState.java              # クライアント側のゲーム状態
    ├── view/
    │   ├── LoginView.java              # ログイン・参加画面
    │   ├── GameView.java               # メインゲーム画面
    │   ├── CardView.java               # カード1枚のUIコンポーネント
    │   └── PlayerPane.java             # 相手プレイヤー表示コンポーネント
    ├── controller/
    │   └── GameController.java         # WebSocket ↔ View のブリッジ
    └── websocket/
        └── PokerWebSocketClient.java   # WebSocket通信クライアント
```

---

## 起動方法

### 前提条件
- Java 17 以上
- Gradle（`./gradlew` でラッパー使用可）
- サーバーが `ws://localhost:8080/poker` で起動済みであること

### 実行
```bash
./gradlew run
```

---

## サーバーとの通信仕様

### メッセージ形式（JSON）

```json
{
  "type": "YOUR_TURN",
  "playerId": "player-1",
  "playerName": "Alice",
  "cards": ["AH", "KS"],
  "amount": 200,
  "pot": 600,
  "chips": 9800,
  "currentBet": 200,
  "position": "BB"
}
```

### カードコード形式
| コード | 意味 |
|--------|------|
| `AH`   | ハートのエース |
| `KS`   | スペードのキング |
| `10D`  | ダイヤの10 |
| `2C`   | クラブの2 |

スート: `H`=ハート, `S`=スペード, `D`=ダイヤ, `C`=クラブ

### メッセージタイプ一覧

| Type          | 方向            | 説明 |
|---------------|-----------------|------|
| `JOIN`        | Client → Server | 入室 |
| `GAME_START`  | Server → Client | ゲーム開始通知 |
| `DEAL_CARDS`  | Server → Client | 手札配布 |
| `FLOP`        | Server → Client | フロップ公開（3枚） |
| `TURN`        | Server → Client | ターン公開（1枚） |
| `RIVER`       | Server → Client | リバー公開（1枚） |
| `YOUR_TURN`   | Server → Client | 自分のターン通知 |
| `PLAYER_ACTION`| Server → Client | 他プレイヤーのアクション通知 |
| `CHIP_UPDATE` | Server → Client | チップ残高更新 |
| `GAME_RESULT` | Server → Client | 勝敗結果 |
| `CALL`        | Client → Server | コール |
| `RAISE`       | Client → Server | レイズ（amountに金額） |
| `FOLD`        | Client → Server | フォールド |
| `CHECK`       | Client → Server | チェック |

---

## 開発メモ

- `GameController` はサーバー担当（A）と密に連携する。  
  メッセージの JSON 構造は `shared/` モジュールで共通定義することを推奨。
- `CardView.flip()` でカードのフリップアニメーションを実行できる。
- サーバーURIは `GameController.SERVER_URI` で変更可能。
