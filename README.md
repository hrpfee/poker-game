# poker-game

## ① ディレクトリ（階層）の詳細

このリポジトリは「**server / client / common**」の3つに分けて、テキサスホールデム（3人）を動かします。

- **`src/server/`**: サーバー側（WebSocketで接続、ゲーム進行・ターン・配信）
- **`src/client/`**: クライアント側（UI/クライアント実装）
  - 現状は **CLI クライアント `PokerClient`** を同梱（この階層だけで動かす用）
  - もともとの JavaFX 画面（`GameView` 等）も残っていますが、JavaFX の `main` はまだありません
- **`src/common/`**: 共通ロジック（カード/デッキ/ゲーム状態/手役判定など）
- **`lib/`**: 実行に必要な外部 JAR（`Java-WebSocket`, `jackson` など）
- **`out/`**: `javac` のコンパイル出力（`.class`）
- **`pom.xml`**: Maven 用（依存関係定義）。Maven が無い環境でも `lib/*` + `javac` で動かせます

## ② 各ファイルの詳細な解説

### サーバー（`src/server/`）

- **`src/server/PokerServer.java`**
  - WebSocket サーバー本体。ポートで待ち受け、接続ごとに `ClientHandler` を作って `GameManager` に渡します。
  - `main(String[] args)` で起動します（例: `PokerServer 8765`）。

- **`src/server/GameManager.java`**
  - ゲームの大枠を管理します（ロビー、参加、ハンド開始、フェーズ遷移、アクション処理、配信）。
  - 内部で `TexasHoldemGame`（`src/common`）を生成/再利用して、状態の進行は common 側のルールに沿って行います。
  - クライアントとのやり取り（JSON）はここで組み立てます。

- **`src/server/ClientHandler.java`**
  - 1接続（1プレイヤー）ぶんのセッション情報を持つ薄いラッパーです。
  - `WebSocket` コネクションと `playerName`、`send()` を提供します。

### 共通（`src/common/`）

- **`src/common/TexasHoldemGame.java`**
  - ホールデムのゲーム進行・ベット処理・フェーズ管理・勝敗判定の中心。
  - `GameSnapshot` を生成して「クライアントに見せて良い情報だけ」を作れます（他人の手札は隠す）。

- **`src/common/PokerHandEvaluator.java`**
  - 5枚/7枚から手役を評価して `HandResult` を返します（`evaluateHand`）。
  - `compare` で役の強さ比較をします（スコア配列比較）。

- **`src/common/GameSnapshot.java` / `src/common/PlayerSnapshot.java`**
  - クライアント表示用の状態DTO。
  - `TexasHoldemGame#createSnapshotFor(...)` から生成され、フェーズ・ポット・ボード・プレイヤー一覧・行動候補を含みます。

- **`src/common/RoundResult.java` / `src/common/WinnerInfo.java` / `src/common/HandResult.java`**
  - 勝敗結果と配当、役名、比較用スコア等を表現します。

- **`src/common/model/Card.java` / `src/common/model/Deck.java` / `src/common/model/Player.java`**
  - カード/デッキ/プレイヤー（チップ、ベット、フォールド状態など）の基礎モデル。

- **`src/common/PlayerActionType.java` / `src/common/GameEvent.java`**
  - アクション種別（check/call/raise/fold）と、ゲーム中に蓄積されるイベントログ。

- **`src/common/PokerGameTest.java` / `src/common/PokerTest.java`**
  - common ロジックの簡易テスト（`main` で実行）。

### クライアント（`src/client/`）

- **`src/client/PokerClient.java`**
  - このリポジトリ単体で動かすための **CLI クライアント**（ターミナル入力で操作）。
  - `JOIN` と `ACTION` を JSON で送ります。

- **`src/client/PokerWebSocketClient.java`**
  - JavaFX 側で使う想定の WebSocket クライアント（`GameMessage` を JSON 変換して送受信）。
  - ※このリポジトリでは `GameMessage` の定義が無いため、現時点では CLI の `PokerClient` を推奨します。

- **`src/client/GameView.java` / `src/client/LoginView.java` / `src/client/PlayerPane.java` / `src/client/CardView.java`**
  - JavaFX の表示コンポーネント群（テーブル、ログ、入力、カード表示など）。
  - ※起動用 `main` は未実装です（必要なら追加できます）。

## ③ 各 class / 関数の詳細（役割）

### `PokerServer`（サーバー起動・接続受け）

- **`PokerServer(int port)`**: 指定ポートで待ち受けする WebSocket サーバーを生成
- **`onOpen(WebSocket, ClientHandshake)`**: 接続確立時。`ClientHandler` を作成し `GameManager.onOpen()` へ
- **`onMessage(WebSocket, String)`**: 受信メッセージを `GameManager.handleMessage()` に委譲
- **`onClose(...)`**: 切断時。`GameManager.onClose()` を呼び出し
- **`main(String[] args)`**: `PokerServer 8765` のように起動

### `GameManager`（ロビー〜勝敗までの進行管理）

#### クライアント→サーバー入力

- **`handleMessage(ClientHandler from, String rawJson)`**
  - `type` を見て `JOIN` / `ACTION` を分岐

- **`handleJoin(...)`**
  - 名前重複チェック、席数（3人）チェック
  - 3人揃ったら `startNewHandLocked()` でハンド開始

- **`handleAction(...)`**
  - 「誰のターンか」を検証（違えば ERROR）
  - `TexasHoldemGame#playerAction(...)` を呼ぶ
  - ベットラウンド完了なら `advanceStreetAfterRoundCompleteLocked()` へ
  - 1人だけ残った/リバー完了なら `finishHandLocked()` へ

#### フェーズ進行

- **`startNewHandLocked()`**
  - `TexasHoldemGame` を再利用し、2ハンド目以降だけ `rotateBlinds()`
  - `startNewRound()` → `preFlop()` の順で開始
  - 最初のアクターを `getBettingStartIndex()` から決めて `STATE` を配信

- **`advanceStreetAfterRoundCompleteLocked()`**
  - `PRE_FLOP -> flop()`、`FLOP -> turn()`、`TURN -> river()` と進める
  - 各ストリートで `resetBetsForNewStreet()` を行う

- **`runOutBoardIfAllInLocked()`**
  - 全員オールインなど「これ以上ベットが発生しない」状況で残りボードを自動でめくる

- **`finishHandLocked()`**
  - `TexasHoldemGame#finishRound()` で勝敗・分配
  - `ROUND_END` を配信して次ハンドへ

#### サーバー→クライアント配信（JSON）

- **`broadcastLobbyLocked()`**: `LOBBY`（人数・プレイヤー名）を配信
- **`broadcastStateLocked()`**: `STATE`（`GameSnapshot`、ターン情報）を配信
- **`broadcastRoundEndLocked(RoundResult)`**: `ROUND_END`（勝者/役/配当）を配信
- **`appendEventLogLocked(String)`**: `LOG` を配信
- **`sendError(WebSocket, String)`**: `ERROR` を返す

### `ClientHandler`（接続単位の薄い状態）

- **`getConnection()`**: WebSocket コネクション取得
- **`getPlayerName()` / `setPlayerName(...)`**: 参加名
- **`send(String json)`**: 接続が開いている場合だけ送信

## 起動（このリポジトリ単体）

## 起動（このリポジトリ単体）

### コンパイル（JavaFX不要 / サーバー+CLIクライアント）

```bash
cd /Users/hr_1pree/2026/porker-game
/usr/bin/javac --release 17 -cp "lib/*" -d out $(find src/common src/server -name '*.java' ! -name 'PokerTest.java') src/client/PokerClient.java
```

### サーバー

```bash
java -cp "out:lib/*" PokerServer 8765
```

### クライアント（CLI / 3人）

別ターミナルを3つ開いて、それぞれ実行します。

```bash
java -cp "out:lib/*" PokerClient ws://localhost:8765 Alice
java -cp "out:lib/*" PokerClient ws://localhost:8765 Bob
java -cp "out:lib/*" PokerClient ws://localhost:8765 Carol
```

入力コマンド:

- `check`
- `call`
- `fold`
- `raise 400`

終了: `exit`

